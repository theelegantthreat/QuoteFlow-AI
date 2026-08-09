package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.graphics.Rect
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.CardThemePreset
import com.example.data.model.Quote
import com.example.data.model.ReadingLog
import com.example.data.repository.QuoteRepository
import com.example.data.api.RetrofitClient
import com.example.util.NotificationHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

class QuoteViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener {

    private val db = AppDatabase.getDatabase(application)
    private val repository = QuoteRepository(db.quoteDao(), RetrofitClient.service)

    // --- Core Flows ---
    val allQuotes: StateFlow<List<Quote>> = repository.allQuotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteQuotes: StateFlow<List<Quote>> = repository.favoriteQuotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val themePresets: StateFlow<List<CardThemePreset>> = repository.themePresets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val readingLogs: StateFlow<List<ReadingLog>> = repository.readingLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Custom UI States ---
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory = _selectedCategory.asStateFlow()

    // --- Filtered Quotes ---
    val filteredQuotes: StateFlow<List<Quote>> = combine(
        allQuotes,
        searchQuery,
        selectedCategory
    ) { quotes, query, category ->
        quotes.filter { quote ->
            val matchesSearch = quote.text.contains(query, ignoreCase = true) ||
                    quote.author.contains(query, ignoreCase = true)
            val matchesCategory = category == "All" || quote.category.equals(category, true)
            matchesSearch && matchesCategory
        }.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.text })
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Dynamic Stats / Analytics ---
    val categoryStats: StateFlow<Map<String, Int>> = readingLogs
        .combine(favoriteQuotes) { logs, favorites ->
            val counts = mutableMapOf<String, Int>()
            logs.forEach {
                counts[it.category] = (counts[it.category] ?: 0) + 1
            }
            if (counts.isEmpty()) {
                counts["Stoicism"] = 4
                counts["Success"] = 3
                counts["Discipline"] = 2
            }
            counts
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val currentStreak: StateFlow<Int> = readingLogs
        .combine(allQuotes) { logs, _ ->
            calculateStreak(logs)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1)

    // --- Text-to-Speech State ---
    private var tts: TextToSpeech? = null
    var isSpeaking by mutableStateOf(false)
        private set
    var ttsSpeed by mutableStateOf(1.0f)

    // --- ACTIVE TODAY QUOTE ---
    private val _todayQuote = MutableStateFlow<Quote?>(null)
    val todayQuote = _todayQuote.asStateFlow()

    // --- Custom Card Designer State ---
    var cardPresetName by mutableStateOf("My Custom Style")
    var cardFontFamily by mutableStateOf("Serif")
    var cardFontSize by mutableStateOf(22)
    var cardTextColor by mutableStateOf("#FFFFFF")
    var cardBgType by mutableStateOf("SOLID") // "SOLID", "GRADIENT", "IMAGE"
    var cardBgValue by mutableStateOf("#6C6A48") // default relaxing olive
    var cardAlignment by mutableStateOf("CENTER") // "LEFT", "CENTER", "RIGHT"
    var cardShowShadow by mutableStateOf(true)
    var cardBorderWidth by mutableStateOf(0)
    var cardBorderColor by mutableStateOf("#FFFFFF")
    var cardBorderRadius by mutableStateOf(16)
    var cardBgOpacity by mutableStateOf(1.0f)
    var cardBgBlur by mutableStateOf(0)

    // --- Custom Category List ---
    var customCategories = mutableListOf(
        "All", "Affirmation", "Alchemy", "Discipline", "Gratitude", "Happiness",
        "Leadership", "Motivation", "Philosophy", "Productivity", "Spirituality",
        "Stoicism", "Success", "Tao"
    )

    init {
        // Initialize TTS
        tts = TextToSpeech(application, this)

        // Seed data & select Quote of the Day
        viewModelScope.launch {
            repository.seedDefaultQuotesIfEmpty()
            selectTodayQuote()
            loadNotificationSettings()
        }
    }

    private suspend fun selectTodayQuote() {
        val quotes = repository.allQuotes.first()
        if (quotes.isNotEmpty()) {
            val calendar = Calendar.getInstance()
            val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
            // Use day of year as semi-random seed to keep today's quote matching and stable per day!
            val index = dayOfYear % quotes.size
            val selected = quotes[index]
            _todayQuote.value = selected
            repository.logReading(selected)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
            tts?.setSpeechRate(ttsSpeed)
        } else {
            Log.e("QuoteViewModel", "TTS Initialization failed!")
        }
    }

    fun speak(text: String, author: String) {
        if (tts == null) return
        stopSpeaking()
        isSpeaking = true
        tts?.setSpeechRate(ttsSpeed)
        val fullText = "$text ... by ... $author"
        tts?.speak(fullText, TextToSpeech.QUEUE_FLUSH, null, "quoteflow_utterance")
        
        // Polling loop to check speaking progress isn't needed, but we can manage isSpeaking state.
        viewModelScope.launch {
            // Simulated check or simple timeout to auto-reset speech indicator
            kotlinx.coroutines.delay(6500)
            isSpeaking = false
        }
    }

    fun stopSpeaking() {
        tts?.stop()
        isSpeaking = false
    }

    fun setSpeechRate(rate: Float) {
        ttsSpeed = rate
        tts?.setSpeechRate(rate)
    }

    private fun calculateStreak(logs: List<ReadingLog>): Int {
        if (logs.isEmpty()) return 1
        
        val sdf = SimpleDateFormat("yyyyMMdd", Locale.US)
        val formattedDates = logs.map { sdf.format(it.timestamp) }.distinct().sortedDescending()
        
        if (formattedDates.isEmpty()) return 1

        val today = sdf.format(System.currentTimeMillis())
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val yesterday = sdf.format(calendar.time)

        // Check if logs exist for today or yesterday to continue streak
        if (formattedDates.first() != today && formattedDates.first() != yesterday) {
            return 1
        }

        var streak = 1
        var currentDateString = formattedDates.first()

        for (i in 1 until formattedDates.size) {
            val prevDate = Calendar.getInstance()
            try {
                val parsed = sdf.parse(currentDateString)
                if (parsed != null) {
                    prevDate.time = parsed
                    prevDate.add(Calendar.DAY_OF_YEAR, -1)
                    val expectedDateString = sdf.format(prevDate.time)
                    if (formattedDates[i] == expectedDateString) {
                        streak++
                        currentDateString = expectedDateString
                    } else {
                        break
                    }
                }
            } catch (e: Exception) {
                break
            }
        }
        return streak
    }

    fun setQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategory(category: String) {
        _selectedCategory.value = category
    }

    fun addCustomCategory(cat: String) {
        if (cat.isNotBlank() && !customCategories.contains(cat)) {
            customCategories.add(cat)
            val sortedOthers = customCategories.drop(1).sortedWith(String.CASE_INSENSITIVE_ORDER)
            customCategories.clear()
            customCategories.add("All")
            customCategories.addAll(sortedOthers)
        }
    }

    fun addNewQuote(text: String, author: String, category: String) {
        viewModelScope.launch {
            val newQuote = Quote(
                id = "custom_" + java.util.UUID.randomUUID().toString().substring(0, 8),
                text = text.trim(),
                author = author.trim().ifBlank { "Unknown" },
                category = category.trim().ifBlank { "General" },
                isFavorite = false,
                isGenerated = false,
                timestamp = System.currentTimeMillis()
            )
            repository.insertQuote(newQuote)
        }
    }

    fun updateQuote(quote: Quote) {
        viewModelScope.launch {
            repository.insertQuote(quote)
        }
    }

    fun restoreQuotesFromJson(context: Context, uri: android.net.Uri, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val contentResolver = context.contentResolver
                val jsonString = contentResolver.openInputStream(uri)?.use { stream ->
                    stream.readBytes().toString(Charsets.UTF_8)
                } ?: throw Exception("Failed to open file stream")

                val jsonArray = org.json.JSONArray(jsonString)
                val quotesList = mutableListOf<Quote>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    val id = obj.optString("id", "custom_" + java.util.UUID.randomUUID().toString().substring(0, 8))
                    val text = obj.optString("text", "")
                    val author = obj.optString("author", "Unknown")
                    val category = obj.optString("category", "General")
                    val isFavorite = obj.optBoolean("isFavorite", false)
                    val isGenerated = obj.optBoolean("isGenerated", false)
                    val timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    val localNote = if (obj.has("localNote") && !obj.isNull("localNote")) obj.getString("localNote") else null

                    if (text.isNotBlank()) {
                        quotesList.add(
                            Quote(
                                id = id,
                                text = text,
                                author = author,
                                category = category,
                                isFavorite = isFavorite,
                                isGenerated = isGenerated,
                                timestamp = timestamp,
                                localNote = localNote
                            )
                        )
                    }
                }

                if (quotesList.isNotEmpty()) {
                    quotesList.forEach { quote ->
                        repository.insertQuote(quote)
                    }
                    onSuccess()
                } else {
                    onError("No valid quotes found in JSON backup")
                }
            } catch (e: Exception) {
                onError(e.message ?: "Unknown parsing error")
            }
        }
    }

    fun deleteQuote(quote: Quote) {
        viewModelScope.launch {
            repository.deleteQuote(quote)
        }
    }

    // --- Favorite & Notes ---
    fun toggleFavorite(quote: Quote) {
        viewModelScope.launch {
            repository.toggleFavorite(quote.id, !quote.isFavorite)
        }
    }

    fun updateQuoteNote(quoteId: String, note: String?) {
        viewModelScope.launch {
            repository.updateQuoteNote(quoteId, note)
        }
    }

    fun selectRandomQuote() {
        viewModelScope.launch {
            val quotes = repository.allQuotes.first()
            if (quotes.isNotEmpty()) {
                val random = quotes.random()
                _todayQuote.value = random
                repository.logReading(random)
            }
        }
    }

    // --- Save & Edit Designs ---
    fun saveDesignerTheme() {
        val preset = CardThemePreset(
            id = "preset_" + UUID.randomUUID().toString().substring(0, 8),
            name = cardPresetName,
            fontFamily = cardFontFamily,
            fontSize = cardFontSize,
            textColor = cardTextColor,
            backgroundType = cardBgType,
            backgroundValue = cardBgValue,
            alignment = cardAlignment,
            showShadow = cardShowShadow,
            borderWidth = cardBorderWidth,
            borderColor = cardBorderColor,
            borderRadius = cardBorderRadius,
            backgroundOpacity = cardBgOpacity,
            backgroundBlur = cardBgBlur
        )
        viewModelScope.launch {
            repository.insertThemePreset(preset)
        }
    }

    fun applySavedTheme(preset: CardThemePreset) {
        cardPresetName = preset.name
        cardFontFamily = preset.fontFamily
        cardFontSize = preset.fontSize
        cardTextColor = preset.textColor
        cardBgType = preset.backgroundType
        cardBgValue = preset.backgroundValue
        cardAlignment = preset.alignment
        cardShowShadow = preset.showShadow
        cardBorderWidth = preset.borderWidth
        cardBorderColor = preset.borderColor
        cardBorderRadius = preset.borderRadius
        cardBgOpacity = preset.backgroundOpacity
        cardBgBlur = preset.backgroundBlur
    }

    fun deleteThemePreset(preset: CardThemePreset) {
        viewModelScope.launch {
            repository.deleteThemePreset(preset.id)
        }
    }

    // --- Settings and Notifications Manager ---
    var userProfileName by mutableStateOf("Guest Inspirer")
    var notificationScheduledTime by mutableStateOf("08:00 AM")
    var isNotificationEnabled by mutableStateOf(true)
    var appThemeMode by mutableStateOf("DARK")

    private suspend fun loadNotificationSettings() {
        userProfileName = repository.getSetting("user_profile_name", "Guest Inspirer")
        notificationScheduledTime = repository.getSetting("notification_scheduled_time", "08:00 AM")
        isNotificationEnabled = repository.getSetting("is_notification_enabled", "true").toBoolean()
        appThemeMode = repository.getSetting("app_theme_mode", "DARK")
    }

    fun updateThemeMode(mode: String) {
        appThemeMode = mode
        viewModelScope.launch {
            repository.saveSetting("app_theme_mode", mode)
        }
    }

    fun updateProfileName(name: String) {
        userProfileName = name
        viewModelScope.launch {
            repository.saveSetting("user_profile_name", name)
        }
    }

    fun updateNotificationTime(time: String) {
        notificationScheduledTime = time
        viewModelScope.launch {
            repository.saveSetting("notification_scheduled_time", time)
        }
    }

    fun toggleNotification(enabled: Boolean) {
        isNotificationEnabled = enabled
        viewModelScope.launch {
            repository.saveSetting("is_notification_enabled", enabled.toString())
        }
    }

    fun triggerLocalMotivationNotification() {
        // Immediatelly trigger a beautiful notification to prove/test scheduled daily motivation!
        val q = _todayQuote.value
        if (q != null) {
            NotificationHelper.showQuoteNotification(
                getApplication(),
                q.text,
                q.author
            )
        } else {
            NotificationHelper.showQuoteNotification(
                getApplication(),
                "The only way to do great work is to love what you do.",
                "Steve Jobs"
            )
        }
    }

    // --- Social Canvas Export Engine (PNG creation) ---
    fun generateQuoteCardBitmap(context: Context, quoteText: String, quoteAuthor: String): Bitmap {
        // Draw the styled card onto a clean standard post layout Bitmap!
        val width = 1080
        val height = 1080
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Draw Background
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        if (cardBgType == "IMAGE") {
            try {
                val resId = context.resources.getIdentifier(cardBgValue, "drawable", context.packageName)
                if (resId != 0) {
                    val drawable = context.resources.getDrawable(resId, null)
                    drawable.setBounds(0, 0, width, height)
                    drawable.draw(canvas)
                    
                    // Highlight contrast with dim overlay
                    val dimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = AndroidColor.argb((255 * 0.45f).toInt(), 0, 0, 0)
                        style = Paint.Style.FILL
                    }
                    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), dimPaint)
                } else {
                    canvas.drawColor(AndroidColor.parseColor("#141218"))
                }
            } catch (e: Exception) {
                canvas.drawColor(AndroidColor.parseColor("#141218"))
            }
        } else if (cardBgType == "GRADIENT") {
            try {
                val hexes = cardBgValue.split("|")
                val colorStart = AndroidColor.parseColor(hexes.getOrElse(0) { "#FF512F" })
                val colorEnd = AndroidColor.parseColor(hexes.getOrElse(1) { "#DD2476" })
                
                val gradientShader = android.graphics.LinearGradient(
                    0f, 0f, width.toFloat(), height.toFloat(),
                    colorStart, colorEnd,
                    android.graphics.Shader.TileMode.CLAMP
                )
                paint.shader = gradientShader
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            } catch (e: Exception) {
                canvas.drawColor(AndroidColor.parseColor("#121212"))
            }
        } else {
            try {
                val solidColor = AndroidColor.parseColor(cardBgValue)
                canvas.drawColor(solidColor)
            } catch (e: Exception) {
                canvas.drawColor(AndroidColor.parseColor("#121212"))
            }
        }

        // Draw card border
        if (cardBorderWidth > 0) {
            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = cardBorderWidth * 3f
                color = try { AndroidColor.parseColor(cardBorderColor) } catch(e:Exception){ AndroidColor.WHITE }
            }
            canvas.drawRect(20f, 20f, width - 20f, height - 20f, borderPaint)
        }

        // 2. Draw Quote Text
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = try { AndroidColor.parseColor(cardTextColor) } catch (e: Exception) { AndroidColor.WHITE }
            textSize = cardFontSize * 2.5f
            textAlign = when (cardAlignment) {
                "LEFT" -> Paint.Align.LEFT
                "RIGHT" -> Paint.Align.RIGHT
                else -> Paint.Align.CENTER
            }
            // Add typewriter, calligraphy typeface approximations
            typeface = when (cardFontFamily.uppercase()) {
                "SERIF" -> android.graphics.Typeface.SERIF
                "MONOSPACE", "TYPEWRITER" -> android.graphics.Typeface.MONOSPACE
                else -> android.graphics.Typeface.SANS_SERIF
            }
        }

        if (cardShowShadow) {
            textPaint.setShadowLayer(8f, 3f, 3f, AndroidColor.DKGRAY)
        }

        // Auto wrap text to fit card
        val words = quoteText.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""
        val maxWidth = width - 160 // margins

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            val bounds = Rect()
            textPaint.getTextBounds(testLine, 0, testLine.length, bounds)
            if (bounds.width() < maxWidth) {
                currentLine = testLine
            } else {
                lines.add(currentLine)
                currentLine = word
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine)
        }

        // Draw multiple lines centered vertically
        val textHeight = textPaint.fontSpacing
        val totalTextHeight = lines.size * textHeight
        var startY = (height - totalTextHeight) / 2f
        val startX = when (cardAlignment) {
            "LEFT" -> 80f
            "RIGHT" -> width - 80f
            else -> width / 2f
        }

        for (line in lines) {
            canvas.drawText(line, startX, startY, textPaint)
            startY += textHeight
        }

        // 3. Draw Author with different styling
        val authorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = textPaint.color
            alpha = 180
            textSize = textPaint.textSize * 0.7f
            textAlign = textPaint.textAlign
            typeface = android.graphics.Typeface.create(textPaint.typeface, android.graphics.Typeface.ITALIC)
        }
        val authorText = "— $quoteAuthor"
        canvas.drawText(authorText, startX, startY + 40f, authorPaint)

        return bitmap
    }

    fun shareQuotePreset(context: Context, quoteText: String, quoteAuthor: String, onShareIntentReady: (File) -> Unit) {
        viewModelScope.launch {
            try {
                val bitmap = generateQuoteCardBitmap(context, quoteText, quoteAuthor)
                val imagesFolder = File(context.cacheDir, "shared_quotes")
                imagesFolder.mkdirs()
                val file = File(imagesFolder, "quoteflow_export_${System.currentTimeMillis()}.png")
                val stream = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                stream.flush()
                stream.close()
                onShareIntentReady(file)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onCleared() {
        tts?.stop()
        tts?.shutdown()
        super.onCleared()
    }
}
