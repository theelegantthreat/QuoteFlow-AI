package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Quote
import com.example.ui.viewmodel.QuoteViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditQuotesScreen(viewModel: QuoteViewModel) {
    val context = LocalContext.current
    val allQuotes by viewModel.allQuotes.collectAsState()
    
    // Search filter within edit list for convenience
    var searchQuery by remember { mutableStateOf("") }
    
    // Filter & Sort A-Z by quote text (case-insensitive)
    val sortedQuotesByAZ = remember(allQuotes, searchQuery) {
        allQuotes.filter {
            it.text.contains(searchQuery, ignoreCase = true) ||
                    it.author.contains(searchQuery, ignoreCase = true) ||
                    it.category.contains(searchQuery, ignoreCase = true)
        }.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.text })
    }

    // State for Dialog editing
    var quoteToEdit by remember { mutableStateOf<Quote?>(null) }
    var quoteToDelete by remember { mutableStateOf<Quote?>(null) }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Edit Quote Library",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Manage all quote text contents, authors, and classification categories inside the library database.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                // Search bar and quotes counter
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Filter quotes to manage...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_screen_search_field"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (sortedQuotesByAZ.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isNotEmpty()) "No quotes match your search." else "No quotes available in your library database.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("edit_quotes_lazy_list"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(sortedQuotesByAZ, key = { it.id }) { quote ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("edit_quote_card_${quote.id}"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Quote Content & Metas Column
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "\"${quote.text}\"",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontStyle = FontStyle.Italic,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 3,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = quote.author,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        SuggestionChip(
                                            onClick = { /* No-op just display tag */ },
                                            label = { 
                                                Text(
                                                    text = quote.category,
                                                    style = MaterialTheme.typography.labelSmall
                                                ) 
                                            },
                                            modifier = Modifier.height(24.dp)
                                        )
                                    }
                                }

                                // Interactive action buttons
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Edit trigger
                                    IconButton(
                                        onClick = { quoteToEdit = quote },
                                        modifier = Modifier.testTag("action_edit_quote_${quote.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit Quote Text and metadata",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    // Delete trigger
                                    IconButton(
                                        onClick = { quoteToDelete = quote },
                                        modifier = Modifier.testTag("action_delete_quote_${quote.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Quote",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Elegant Editing Dialog ---
    if (quoteToEdit != null) {
        val editingQuote = quoteToEdit!!
        var textValue by remember(editingQuote) { mutableStateOf(editingQuote.text) }
        var authorValue by remember(editingQuote) { mutableStateOf(editingQuote.author) }
        var categoryValue by remember(editingQuote) { mutableStateOf(editingQuote.category) }
        var errorText by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { quoteToEdit = null },
            title = {
                Text(
                    text = "Edit Quote Details",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Quote text block
                    OutlinedTextField(
                        value = textValue,
                        onValueChange = {
                            textValue = it
                            if (it.isNotBlank()) errorText = ""
                        },
                        label = { Text("Quote Body Text") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_edit_text_field"),
                        minLines = 3,
                        maxLines = 5,
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.FormatQuote,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            )
                        }
                    )

                    // Quote author
                    OutlinedTextField(
                        value = authorValue,
                        onValueChange = { authorValue = it },
                        label = { Text("Author") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_edit_author_field"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            )
                        }
                    )

                    // Quote category
                    OutlinedTextField(
                        value = categoryValue,
                        onValueChange = { categoryValue = it },
                        label = { Text("Category") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_edit_category_field"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Label,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            )
                        }
                    )

                    if (errorText.isNotEmpty()) {
                        Text(
                            text = errorText,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmedText = textValue.trim()
                        if (trimmedText.isBlank()) {
                            errorText = "Quote content cannot be empty"
                        } else {
                            val trimmedCategory = categoryValue.trim().ifBlank { "General" }
                            val updatedQuote = editingQuote.copy(
                                text = trimmedText,
                                author = authorValue.trim().ifBlank { "Unknown" },
                                category = trimmedCategory
                            )
                            viewModel.updateQuote(updatedQuote)
                            viewModel.addCustomCategory(trimmedCategory)
                            quoteToEdit = null
                            android.widget.Toast.makeText(context, "Quote updated successfully!", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.testTag("dialog_save_edit_btn"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { quoteToEdit = null },
                    modifier = Modifier.testTag("dialog_cancel_edit_btn")
                ) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }

    // --- Elegant Delete Confirmation Dialog ---
    if (quoteToDelete != null) {
        val deletingQuote = quoteToDelete!!

        AlertDialog(
            onDismissRequest = { quoteToDelete = null },
            title = {
                Text(
                    text = "Confirm Deletion",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Text(
                    text = "Are you absolutely sure you want to delete this quote from your database? This action is permanent.\n\n\"${deletingQuote.text}\""
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteQuote(deletingQuote)
                        quoteToDelete = null
                        android.widget.Toast.makeText(context, "Quote deleted", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.testTag("dialog_confirm_delete_btn"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { quoteToDelete = null },
                    modifier = Modifier.testTag("dialog_cancel_delete_btn")
                ) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
}
