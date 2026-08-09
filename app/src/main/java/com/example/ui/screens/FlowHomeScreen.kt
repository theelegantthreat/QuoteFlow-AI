package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.Image
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.data.model.Quote
import com.example.ui.viewmodel.QuoteViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlowHomeScreen(viewModel: QuoteViewModel) {
    val context = LocalContext.current
    val todayQuote by viewModel.todayQuote.collectAsState()
    val filteredQuotes by viewModel.filteredQuotes.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isSpeaking = viewModel.isSpeaking
    val allQuotes by viewModel.allQuotes.collectAsState()

    // Export/Backup JSON Launcher
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        uri?.let {
            try {
                val jsonArray = org.json.JSONArray()
                allQuotes.forEach { q ->
                    val obj = org.json.JSONObject().apply {
                        put("id", q.id)
                        put("text", q.text)
                        put("author", q.author)
                        put("category", q.category)
                        put("isFavorite", q.isFavorite)
                        put("isGenerated", q.isGenerated)
                        put("timestamp", q.timestamp)
                        put("localNote", q.localNote)
                    }
                    jsonArray.put(obj)
                }
                val jsonString = jsonArray.toString(4)
                context.contentResolver.openOutputStream(it)?.use { stream ->
                    stream.write(jsonString.toByteArray(Charsets.UTF_8))
                }
                android.widget.Toast.makeText(context, "Backup exported successfully!", android.widget.Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "Export failed: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    // Import/Restore JSON Launcher
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            viewModel.restoreQuotesFromJson(
                context = context,
                uri = it,
                onSuccess = {
                    android.widget.Toast.makeText(context, "Backup restored successfully!", android.widget.Toast.LENGTH_SHORT).show()
                },
                onError = { error ->
                    android.widget.Toast.makeText(context, "Restore failed: $error", android.widget.Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    var currentNoteDialogQuoteId by remember { mutableStateOf<String?>(null) }
    var currentNoteText by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("flow_home_list"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Header Bar (Local Motivation Notification trigger)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    var menuExpanded by remember { mutableStateOf(false) }

                    Box {
                        IconButton(
                            onClick = { menuExpanded = true },
                            modifier = Modifier.testTag("hamburger_menu_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu"
                            )
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            modifier = Modifier.testTag("hamburger_dropdown_menu")
                        ) {
                            DropdownMenuItem(
                                text = { Text("Export JSON") },
                                onClick = {
                                    menuExpanded = false
                                    val formattedDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
                                    exportLauncher.launch("backup-QuoteFlow-$formattedDate.JSON")
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Backup,
                                        contentDescription = "Export JSON File"
                                    )
                                },
                                modifier = Modifier.testTag("menu_backup_btn")
                            )
                            DropdownMenuItem(
                                text = { Text("Import JSON") },
                                onClick = {
                                    menuExpanded = false
                                    importLauncher.launch(arrayOf("application/json"))
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Restore,
                                        contentDescription = "Import JSON File"
                                    )
                                },
                                modifier = Modifier.testTag("menu_restore_btn")
                            )
                        }
                    }

                    Column {
                        Text(
                            text = "DAILY FLOW",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 2.2.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (viewModel.userProfileName == "Guest Inspirer") "QuoteFlow AI" else "QuoteFlow AI • ${viewModel.userProfileName}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
                
                IconButton(
                    onClick = { viewModel.triggerLocalMotivationNotification() },
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.primaryContainer,
                            RoundedCornerShape(12.dp)
                        )
                        .testTag("notification_ping_btn"),
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = "Trigger immediate morning notification reminder"
                    )
                }
            }
        }

        // Integrated Personalized Daily Quote Generator Container Component
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("today_quote_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // 1. Decorative Header Area
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Personalized Daily Quote",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // 2. Display Area for Quotes
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f))
                            .testTag("main_display_area_quotes")
                    ) {
                        todayQuote?.let { quote ->
                            Column {
                                StyledQuoteCard(
                                    text = quote.text,
                                    author = quote.author,
                                    fontFamily = viewModel.cardFontFamily,
                                    fontSize = viewModel.cardFontSize,
                                    textColor = viewModel.cardTextColor,
                                    bgType = viewModel.cardBgType,
                                    bgValue = viewModel.cardBgValue,
                                    alignment = viewModel.cardAlignment,
                                    showShadow = viewModel.cardShowShadow,
                                    borderWidth = viewModel.cardBorderWidth,
                                    borderColor = viewModel.cardBorderColor,
                                    borderRadius = viewModel.cardBorderRadius,
                                    opacity = viewModel.cardBgOpacity,
                                    blur = viewModel.cardBgBlur
                                )

                                // Nested tools bar under display area
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                        .padding(vertical = 4.dp, horizontal = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = {
                                            if (isSpeaking) {
                                                viewModel.stopSpeaking()
                                            } else {
                                                viewModel.speak(quote.text, quote.author)
                                            }
                                        },
                                        modifier = Modifier.testTag("tts_playback_btn")
                                    ) {
                                        Icon(
                                            imageVector = if (isSpeaking) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                            contentDescription = "Listen to quote in multiple speeds",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { viewModel.toggleFavorite(quote) },
                                        modifier = Modifier.testTag("favorite_toggle_btn")
                                    ) {
                                        Icon(
                                            imageVector = if (quote.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                            contentDescription = "Save quote to library collections",
                                            tint = if (quote.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    IconButton(onClick = { viewModel.selectRandomQuote() }) {
                                        Icon(
                                            imageVector = Icons.Default.Shuffle,
                                            contentDescription = "Surprise me with a scheduled new quote",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            val clipboardManager = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                            val clip = android.content.ClipData.newPlainText("Copied Quote", "\"${quote.text}\" — ${quote.author}")
                                            clipboardManager.setPrimaryClip(clip)
                                            android.widget.Toast.makeText(context, "Quote copied to clipboard!", android.widget.Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.testTag("copy_quote_btn_daily")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy quote to clipboard",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            viewModel.shareQuotePreset(context, quote.text, quote.author) { file ->
                                                val uri = FileProvider.getUriForFile(
                                                    context,
                                                    "${context.packageName}.fileprovider",
                                                    file
                                                )
                                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                    type = "image/png"
                                                    putExtra(Intent.EXTRA_STREAM, uri)
                                                    putExtra(Intent.EXTRA_TEXT, "\"${quote.text}\" - ${quote.author}\n\nShared from QuoteFlow AI")
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }
                                                context.startActivity(Intent.createChooser(shareIntent, "Share Styled Quote Card"))
                                            }
                                        },
                                        modifier = Modifier.testTag("share_quote_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "Export to Instagram or Facebook cards",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        } ?: Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No Quote Loaded. Fetch one below!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Horizontal Category Scrolling Filter chips
        item {
            Column {
                Text(
                    text = "Explore Categories",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(viewModel.customCategories) { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { viewModel.setCategory(category) },
                            label = { Text(category) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }
            }
        }

        // Inline Search quotes bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("quote_search_field"),
                placeholder = { Text("Search quotes by text or author...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setQuery("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )
        }

        // Add Custom Quote component under "search quotes"
        item {
            var showAddForm by remember { mutableStateOf(false) }
            var textInput by remember { mutableStateOf("") }
            var authorInput by remember { mutableStateOf("") }
            var categoryInput by remember { mutableStateOf("") }
            var errorMsg by remember { mutableStateOf("") }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_custom_quote_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.12f)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAddForm = !showAddForm },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Create Custom Quote",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Icon(
                            imageVector = if (showAddForm) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Toggle Add Quote Fields",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }

                    if (showAddForm) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            thickness = 1.dp
                        )

                        // 1. Text/Content Input
                        OutlinedTextField(
                            value = textInput,
                            onValueChange = { 
                                textInput = it
                                if (it.isNotBlank()) errorMsg = ""
                            },
                            label = { Text("Quote Text / Saying") },
                            placeholder = { Text("Enter a beautiful inspiring message...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("add_quote_text_input"),
                            shape = RoundedCornerShape(12.dp),
                            minLines = 2,
                            maxLines = 4,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.FormatQuote,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )

                        // 2. Author Input & Category Input in high-quality row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = authorInput,
                                onValueChange = { authorInput = it },
                                label = { Text("Author / Source") },
                                placeholder = { Text("e.g. Socrates") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("add_quote_author_input"),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            )

                            OutlinedTextField(
                                value = categoryInput,
                                onValueChange = { categoryInput = it },
                                label = { Text("Category") },
                                placeholder = { Text("e.g. Wisdom") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("add_quote_category_input"),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Label,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            )
                        }

                        if (errorMsg.isNotEmpty()) {
                            Text(
                                text = errorMsg,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }

                        // Submit action button
                        Button(
                            onClick = {
                                if (textInput.isBlank()) {
                                    errorMsg = "Quote text cannot be empty"
                                } else {
                                    val cat = categoryInput.trim().ifBlank { "General" }
                                    viewModel.addNewQuote(
                                        text = textInput,
                                        author = authorInput,
                                        category = cat
                                    )
                                    // Add to categories suggestions dynamically if not exist
                                    viewModel.addCustomCategory(cat)
                                    // Reset fields and collapse
                                    textInput = ""
                                    authorInput = ""
                                    categoryInput = ""
                                    errorMsg = ""
                                    showAddForm = false
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("save_custom_quote_btn"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save Quote to Library", style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }

        // Matching local cache list quotes
        if (filteredQuotes.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No offline matches found for '$selectedCategory'",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                    )
                }
            }
        } else {
            items(filteredQuotes) { quote ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(16.dp)
                        ),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            SuggestionChip(
                                onClick = {},
                                label = { Text(quote.category) }
                            )

                            Row {
                                IconButton(
                                    onClick = {
                                        val clipboardManager = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                        val clip = android.content.ClipData.newPlainText("Copied Quote", "\"${quote.text}\" — ${quote.author}")
                                        clipboardManager.setPrimaryClip(clip)
                                        android.widget.Toast.makeText(context, "Quote copied to clipboard!", android.widget.Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.testTag("copy_quote_btn_${quote.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy quote to clipboard",
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }

                                IconButton(onClick = {
                                    currentNoteText = quote.localNote ?: ""
                                    currentNoteDialogQuoteId = quote.id
                                }) {
                                    Icon(
                                        imageVector = if (quote.localNote.isNullOrBlank()) Icons.Outlined.EditNote else Icons.Filled.NoteAlt,
                                        contentDescription = "Add personal notes to this quote",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }

                                IconButton(onClick = { viewModel.toggleFavorite(quote) }) {
                                    Icon(
                                        imageVector = if (quote.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                        contentDescription = "Bookmark",
                                        tint = if (quote.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        Text(
                            text = "\"${quote.text}\"",
                            style = MaterialTheme.typography.bodyLarge,
                            fontStyle = FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "— ${quote.author}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )

                        // Show personal note if available
                        if (!quote.localNote.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "My Note: ${quote.localNote}",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(8.dp),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal dialog to add personal notes on quotes
    if (currentNoteDialogQuoteId != null) {
        AlertDialog(
            onDismissRequest = { currentNoteDialogQuoteId = null },
            title = { Text("Personal Quote Reflection") },
            text = {
                Column {
                    Text(
                        text = "Jot down your notes, insights or what this quote reflects in you:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = currentNoteText,
                        onValueChange = { currentNoteText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Reflect here...") },
                        maxLines = 4
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val quoteId = currentNoteDialogQuoteId
                        if (quoteId != null) {
                            viewModel.updateQuoteNote(quoteId, currentNoteText.takeIf { it.isNotBlank() })
                        }
                        currentNoteDialogQuoteId = null
                    }
                ) {
                    Text("Save Note")
                }
            },
            dismissButton = {
                TextButton(onClick = { currentNoteDialogQuoteId = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// Reusable styled quote card representing layout variations
@Composable
fun StyledQuoteCard(
    text: String,
    author: String,
    fontFamily: String,
    fontSize: Int,
    textColor: String,
    bgType: String,
    bgValue: String,
    alignment: String,
    showShadow: Boolean,
    borderWidth: Int,
    borderColor: String,
    borderRadius: Int,
    opacity: Float,
    blur: Int,
    modifier: Modifier = Modifier
) {
    val textAlignment = when (alignment.uppercase()) {
        "LEFT" -> TextAlign.Left
        "RIGHT" -> TextAlign.Right
        else -> TextAlign.Center
    }

    val parsedTextColor = remember(textColor) {
        try { Color(android.graphics.Color.parseColor(textColor)) } catch (e: Exception) { Color.White }
    }

    val parsedBorderColor = remember(borderColor) {
        try { Color(android.graphics.Color.parseColor(borderColor)) } catch (e: Exception) { Color.White }
    }

    val customFont = when (fontFamily.uppercase()) {
        "SERIF" -> FontFamily.Serif
        "MONOSPACE", "TYPEWRITER" -> FontFamily.Monospace
        else -> FontFamily.SansSerif
    }

    val context = LocalContext.current
    val imageResId = remember(bgValue) {
        if (bgType.uppercase() == "IMAGE") {
            val id = context.resources.getIdentifier(bgValue, "drawable", context.packageName)
            if (id != 0) id else null
        } else {
            null
        }
    }

    // Capture Background configuration correctly
    val bgModifier = if (bgType.uppercase() == "GRADIENT") {
        val colors = remember(bgValue) {
            try {
                val parts = bgValue.split("|")
                val start = Color(android.graphics.Color.parseColor(parts.getOrElse(0) { "#FF512F" }))
                val end = Color(android.graphics.Color.parseColor(parts.getOrElse(1) { "#DD2476" }))
                listOf(start, end)
            } catch (e: Exception) {
                listOf(Color(0xFF6C6A48), Color(0xFF1E1E1E))
            }
        }
        Modifier.background(Brush.linearGradient(colors))
    } else if (bgType.uppercase() == "IMAGE") {
        Modifier.background(Color(0xFF141218))
    } else {
        val solidColor = remember(bgValue) {
            try { Color(android.graphics.Color.parseColor(bgValue)) } catch (e: Exception) { Color(0xFF6C6A48) }
        }
        Modifier.background(solidColor)
    }

    val blurModifier = if (blur > 0) Modifier.blur(blur.dp) else Modifier

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 240.dp)
            .clip(RoundedCornerShape(borderRadius.dp))
            .border(borderWidth.dp, parsedBorderColor, RoundedCornerShape(borderRadius.dp))
            .then(bgModifier)
            .then(blurModifier)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        if (bgType.uppercase() == "IMAGE" && imageResId != null) {
            Image(
                painter = painterResource(id = imageResId),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.45f))
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = when (alignment.uppercase()) {
                "LEFT" -> Alignment.Start
                "RIGHT" -> Alignment.End
                else -> Alignment.CenterHorizontally
            }
        ) {
            Text(
                text = "\"$text\"",
                style = TextStyle(
                    fontFamily = customFont,
                    fontSize = fontSize.sp,
                    lineHeight = (fontSize * 1.4f).sp,
                    textAlign = textAlignment,
                    color = parsedTextColor.copy(alpha = opacity)
                ),
                modifier = if (showShadow) {
                    Modifier.shadow(2.dp, shape = RoundedCornerShape(2.dp))
                } else {
                    Modifier
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "— $author",
                style = TextStyle(
                    fontFamily = customFont,
                    fontSize = (fontSize * 0.75f).sp,
                    fontStyle = FontStyle.Italic,
                    color = parsedTextColor.copy(alpha = opacity * 0.8f)
                )
            )
        }
    }
}
