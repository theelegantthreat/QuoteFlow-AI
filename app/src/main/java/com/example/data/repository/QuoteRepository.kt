package com.example.data.repository

import android.util.Log
import com.example.BuildConfig
import com.example.data.api.Content
import com.example.data.api.GeminiApiService
import com.example.data.api.GenerateContentRequest
import com.example.data.api.GenerationConfig
import com.example.data.api.Part
import com.example.data.database.QuoteDao
import com.example.data.model.AppSetting
import com.example.data.model.CardThemePreset
import com.example.data.model.Quote
import com.example.data.model.ReadingLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.UUID

class QuoteRepository(
    private val quoteDao: QuoteDao,
    private val apiService: GeminiApiService
) {
    val allQuotes: Flow<List<Quote>> = quoteDao.getAllQuotes()
    val favoriteQuotes: Flow<List<Quote>> = quoteDao.getFavoriteQuotes()
    val themePresets: Flow<List<CardThemePreset>> = quoteDao.getAllThemePresets()
    val readingLogs: Flow<List<ReadingLog>> = quoteDao.getReadingLogs()
    val readingCount: Flow<Int> = quoteDao.getReadingLogsCount()

    suspend fun getQuotesByCategory(category: String): Flow<List<Quote>> {
        return quoteDao.getQuotesByCategory(category)
    }

    suspend fun searchQuotes(query: String): Flow<List<Quote>> {
        return quoteDao.searchQuotes(query)
    }

    suspend fun toggleFavorite(id: String, isFavorite: Boolean) {
        quoteDao.updateFavoriteStatus(id, isFavorite)
    }

    suspend fun updateQuoteNote(id: String, note: String?) {
        quoteDao.updateQuoteNote(id, note)
    }

    suspend fun insertQuote(quote: Quote) {
        quoteDao.insertQuote(quote)
    }

    suspend fun deleteQuote(quote: Quote) {
        quoteDao.deleteQuote(quote)
    }

    suspend fun logReading(quote: Quote) {
        quoteDao.insertReadingLog(
            ReadingLog(
                quoteId = quote.id,
                text = quote.text,
                author = quote.author,
                category = quote.category
            )
        )
    }

    // --- Saved Themes ---
    suspend fun insertThemePreset(preset: CardThemePreset) {
        quoteDao.insertThemePreset(preset)
    }

    suspend fun deleteThemePreset(id: String) {
        quoteDao.deleteThemePreset(id)
    }

    // --- Settings Profiles & Streaks ---
    suspend fun getSetting(key: String, defaultValue: String): String {
        return quoteDao.getSetting(key)?.value ?: defaultValue
    }

    suspend fun saveSetting(key: String, value: String) {
        quoteDao.saveSetting(AppSetting(key, value))
    }

    // --- Seeding Default Quotes ---
    suspend fun seedDefaultQuotesIfEmpty() {
        val existing = quoteDao.getAllQuotes().first()
        if (existing.isNotEmpty()) return

        Log.d("QuoteRepository", "Seeding default quotes...")
        val defaults = listOf(
            Quote("q1", "The obstacle is the way.", "Marcus Aurelius", "Stoicism"),
            Quote("q2", "We suffer more often in imagination than in reality.", "Seneca", "Stoicism"),
            Quote("q3", "Waste no more time arguing about what a good man should be. Be one.", "Marcus Aurelius", "Stoicism"),
            Quote("q4", "The only way to do great work is to love what you do.", "Steve Jobs", "Success"),
            Quote("q5", "Success is not final, failure is not fatal: it is the courage to continue that counts.", "Winston Churchill", "Success"),
            Quote("q6", "Discipline is the bridge between goals and accomplishment.", "Jim Rohn", "Discipline"),
            Quote("q7", "We are what we repeatedly do. Excellence, then, is not an act, but a habit.", "Aristotle", "Discipline"),
            Quote("q8", "Sanity and happiness are an impossible combination.", "Mark Twain", "Happiness"),
            Quote("q9", "For every minute you are angry you lose sixty seconds of happiness.", "Ralph Waldo Emerson", "Happiness"),
            Quote("q10", "Knowing yourself is the beginning of all wisdom.", "Aristotle", "Philosophy"),
            Quote("q11", "I can control my thoughts, not my circumstances.", "Epictetus", "Stoicism"),
            Quote("q12", "Lead from the heart, not from the ego.", "Lao Tzu", "Leadership"),
            Quote("q13", "The great alchemists realized that the gold was within.", "Manly P. Hall", "Alchemy"),
            Quote("q14", "Productivity is being able to do things that you were never able to do before.", "Franz Kafka", "Productivity"),
            Quote("q15", "Gratitude opens the door to the power, the wisdom, the creativity of the universe.", "Deepak Chopra", "Gratitude"),
            Quote("q16", "You must be the change you wish to see in the world.", "Mahatma Gandhi", "Spirituality"),
            Quote("q17", "An unexamined life is not worth living.", "Socrates", "Philosophy"),
            Quote("q18", "The gold is not found in the manifest; it is extracted from the base matters of struggle.", "Nicolas Flamel", "Alchemy"),
            Quote("q19", "Do not go where the path may lead, go instead where there is no path and leave a trail.", "Ralph Waldo Emerson", "Leadership")
        )
        quoteDao.insertQuotes(defaults)

        // Seed some starter themes
        val starterThemes = listOf(
            CardThemePreset("t1", "Sunset Zen", "Serif", 22, "#FFFFFF", "GRADIENT", "#FF512F|#DD2476", "CENTER", true, "#7A000000"),
            CardThemePreset("t2", "Deep Slate", "Sans Serif", 20, "#E0E0E0", "SOLID", "#121212", "CENTER", false, "#000000"),
            CardThemePreset("t3", "Emerald Forest", "Handwritten", 24, "#F0FFF0", "GRADIENT", "#11998e|#38ef7d", "CENTER", true, "#2A000000")
        )
        for (theme in starterThemes) {
            quoteDao.insertThemePreset(theme)
        }
    }

    // --- Gemini AI Generation Engine ---
    suspend fun generateQuoteWithAI(
        category: String,
        tone: String,
        style: String,
        mood: String = "",
        customTopic: String = "",
        customPromptOverride: String? = null
    ): Quote {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            throw Exception("Gemini API Key is not configured. Please add your key in the Secrets panel in AI Studio.")
        }

        val targetTopic = if (customTopic.isNotBlank()) customTopic else category
        val moodClause = if (mood.isNotBlank()) "for someone currently feeling '$mood'" else "to inspire positive perspective"

        val parsedPrompt = customPromptOverride ?: when (style.lowercase()) {
            "affirmation" -> "Generate a powerful, positive $tone daily affirmation $moodClause, focusing on the theme: $targetTopic."
            "stoic" -> "Create a deep, reflective, Stoic quote $moodClause, dealing with $targetTopic in a $tone style."
            else -> "Generate a brand new, highly motivational quote $moodClause about '$targetTopic' in a $tone style. Tone should be inspiring and memorable."
        }

        val request = GenerateContentRequest(
            contents = listOf(
                Content(parts = listOf(Part(text = parsedPrompt)))
            ),
            generationConfig = GenerationConfig(
                temperature = 0.85f,
                maxOutputTokens = 150
            ),
            systemInstruction = Content(
                parts = listOf(
                    Part(
                        text = "You are QuoteFlow AI, a philosophical mentor. Return exactly one newly minted quote, followed by the author name. FORMAT EXACTLY LIKE THIS (no quotes or extra text around it, just the text followed by a dash and then author): \n[Quote text] - [Author Name]\nFor example, if you make up an author, call them 'AI Sage' or 'Alchemist' or a custom stoic name. Do not include introductory conversational statements or greetings."
                    )
                )
            )
        )

        val response = apiService.generateContent(apiKey, request)
        val fullText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            ?: throw Exception("Empty response received from Gemini.")

        // Parse: "Quote text" - Author Name
        var text = fullText.trim()
        var author = "AI Sage"

        if (text.contains(" - ")) {
            val parts = text.split(" - ")
            text = parts[0].trim().replace("\"", "")
            author = parts[1].trim()
        } else if (text.contains("-")) {
            val idx = text.lastIndexOf("-")
            if (idx in 1 until text.length - 1) {
                author = text.substring(idx + 1).trim()
                text = text.substring(0, idx).trim().replace("\"", "")
            }
        }

        if (text.length > 300) {
            text = text.substring(0, 300) + "..."
        }

        val generatedQuote = Quote(
            id = "ai_" + UUID.randomUUID().toString().substring(0, 8),
            text = text,
            author = author,
            category = category,
            isGenerated = true,
            timestamp = System.currentTimeMillis()
        )

        quoteDao.insertQuote(generatedQuote)
        return generatedQuote
    }
}
