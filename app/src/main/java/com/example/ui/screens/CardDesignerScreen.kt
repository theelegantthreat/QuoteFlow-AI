package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.QuoteViewModel

data class ArtAsset(
    val resourceName: String,
    val title: String,
    val description: String,
    val tags: String
)

val curatedArtAssets = listOf(
    ArtAsset("img_art_mystic_abyss", "Mystic Abyss", "Fluid dark velvet waves and golden stardust", "PREMIUM • TWILIGHT VIBE"),
    ArtAsset("img_art_cosmic_pulse", "Cosmic Pulse", "Celestial glowing lavender stars & nebula", "COSMIC • MEDITATION"),
    ArtAsset("img_art_golden_mind", "Golden Mind", "Thick textured oil brushstrokes with gold foil", "ABSTRACT • LUXURY CALM"),
    ArtAsset("img_art_zen_serenity", "Zen Serenity", "Layered lilac stacking stones over sunset", "SERENE • BALANCED MIND")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardDesignerScreen(viewModel: QuoteViewModel) {
    val themePresets by viewModel.themePresets.collectAsState()
    val scrollState = rememberScrollState()

    val popularColors = listOf(
        "#6C6A48", // Relaxed Olive
        "#10B981", // Emerald Jade
        "#FF512F", // Radiant Sunrise
        "#DD2476", // Mystic Rose
        "#1F2937", // Charcoal Slate
        "#000000", // AMOLED Black
        "#D4AF37", // Aurean Gold
        "#4A3B32"  // Ancient Soil
    )

    val gradients = listOf(
        "#FF512F|#DD2476", // Sunset Rose
        "#11998E|#38EF7D", // Emerald Ocean
        "#0F2027|#2C5364", // Deep Void
        "#8E2DE2|#4A00E0", // Velvet Violet
        "#F12711|#F5AF19", // Solar Flare
        "#606C88|#3F5C80"  // Slate Blue
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
            .testTag("card_designer_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Quote Card Designer",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        // LIVE CARD PREVIEW
        Text(text = "Live Theme Preview", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        StyledQuoteCard(
            text = "This is a masterpiece of custom visual expression. See how your choices align.",
            author = "QuoteFlow Creator",
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

        // Alignment row & typography config card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Aesthetic Typography",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                // Font Family Options
                Text(text = "Font Pairings", style = MaterialTheme.typography.bodySmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val fontOptions = listOf("Serif", "Sans Serif", "Typewriter")
                    fontOptions.forEach { font ->
                        val selected = viewModel.cardFontFamily == font
                        InputChip(
                            selected = selected,
                            onClick = { viewModel.cardFontFamily = font },
                            label = { Text(font) }
                        )
                    }
                }

                // Font Size Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Font Size: ${viewModel.cardFontSize}sp", style = MaterialTheme.typography.bodySmall)
                }
                Slider(
                    value = viewModel.cardFontSize.toFloat(),
                    onValueChange = { viewModel.cardFontSize = it.toInt() },
                    valueRange = 14f..32f,
                    modifier = Modifier.testTag("font_size_slider")
                )

                // Alignment Selector
                Text(text = "Text Alignment", style = MaterialTheme.typography.bodySmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val aligns = listOf("LEFT", "CENTER", "RIGHT")
                    aligns.forEach { align ->
                        val selected = viewModel.cardAlignment == align
                        IconButton(
                            onClick = { viewModel.cardAlignment = align },
                            modifier = Modifier
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(8.dp)
                                )
                        ) {
                            Icon(
                                imageVector = when (align) {
                                    "LEFT" -> Icons.Default.FormatAlignLeft
                                    "RIGHT" -> Icons.Default.FormatAlignRight
                                    else -> Icons.Default.FormatAlignCenter
                                },
                                contentDescription = align,
                                tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Text Shadow Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Apply Core Shadow Backdrop", style = MaterialTheme.typography.bodySmall)
                    Switch(
                        checked = viewModel.cardShowShadow,
                        onCheckedChange = { viewModel.cardShowShadow = it }
                    )
                }
            }
        }

        // Color & Background config card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Atmosphere & Palette",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                // Bg Type Selection (Solid vs Gradient vs Image)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ElevatedButton(
                        onClick = {
                            viewModel.cardBgType = "SOLID"
                            viewModel.cardBgValue = "#6C6A48"
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (viewModel.cardBgType == "SOLID") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (viewModel.cardBgType == "SOLID") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text("Solid Color")
                    }

                    ElevatedButton(
                        onClick = {
                            viewModel.cardBgType = "GRADIENT"
                            viewModel.cardBgValue = "#FF512F|#DD2476"
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (viewModel.cardBgType == "GRADIENT") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (viewModel.cardBgType == "GRADIENT") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text("Linear Gradient")
                    }

                    ElevatedButton(
                        onClick = {
                            viewModel.cardBgType = "IMAGE"
                            viewModel.cardBgValue = "img_art_mystic_abyss"
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (viewModel.cardBgType == "IMAGE") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (viewModel.cardBgType == "IMAGE") MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text("Artistic Gallery ✨")
                    }
                }

                // Text Color Selector input
                OutlinedTextField(
                    value = viewModel.cardTextColor,
                    onValueChange = { viewModel.cardTextColor = it },
                    label = { Text("Text Color (HEX code, e.g. #FFFFFF)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                // Background Solidarity Choice Lists
                if (viewModel.cardBgType == "SOLID") {
                    Text(text = "Tap Solid Palette", style = MaterialTheme.typography.bodySmall)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(popularColors) { hex ->
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(hex)))
                                    .border(
                                        width = if (viewModel.cardBgValue == hex) 3.dp else 0.dp,
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = CircleShape
                                    )
                                    .clickable {
                                        viewModel.cardBgValue = hex
                                    }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = viewModel.cardBgValue,
                        onValueChange = { viewModel.cardBgValue = it },
                        label = { Text("Solid Background (HEX code)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                } else if (viewModel.cardBgType == "GRADIENT") {
                    // Gradient presets list
                    Text(text = "Tap Gradient Preset", style = MaterialTheme.typography.bodySmall)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(gradients) { grad ->
                            val parts = grad.split("|")
                            val colorStart = Color(android.graphics.Color.parseColor(parts[0]))
                            val colorEnd = Color(android.graphics.Color.parseColor(parts[1]))

                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Brush.linearGradient(listOf(colorStart, colorEnd)))
                                    .border(
                                        width = if (viewModel.cardBgValue == grad) 3.dp else 0.dp,
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        viewModel.cardBgValue = grad
                                    }
                            )
                        }
                    }
                } else {
                    // IMAGE background type - CURATED 'ARTISTIC FLAIR' GALLERY SECTION
                    Text(
                        text = "Curated 'Artistic Flair' Gallery",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(curatedArtAssets) { asset ->
                            val isSelected = viewModel.cardBgValue == asset.resourceName
                            val context = LocalContext.current
                            val resId = remember(asset.resourceName) {
                                context.resources.getIdentifier(asset.resourceName, "drawable", context.packageName)
                            }
                            
                            Card(
                                modifier = Modifier
                                    .width(160.dp)
                                    .clickable {
                                        viewModel.cardBgValue = asset.resourceName
                                    }
                                    .testTag("gallery_item_${asset.resourceName}"),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                ),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(110.dp)
                                    ) {
                                        if (resId != 0) {
                                            Image(
                                                painter = painterResource(id = resId),
                                                contentDescription = asset.title,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    Brush.verticalGradient(
                                                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f))
                                                    )
                                                )
                                        )
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = "Selected",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(6.dp)
                                                    .size(24.dp)
                                            )
                                        }
                                    }
                                    Column(
                                        modifier = Modifier.padding(8.dp)
                                    ) {
                                        Text(
                                            text = asset.title,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = asset.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            maxLines = 2,
                                            lineHeight = 11.sp
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = asset.tags,
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, letterSpacing = 1.sp),
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Border Width & Radius sliders
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Border Width: ${viewModel.cardBorderWidth}dp", style = MaterialTheme.typography.bodySmall)
                }
                Slider(
                    value = viewModel.cardBorderWidth.toFloat(),
                    onValueChange = { viewModel.cardBorderWidth = it.toInt() },
                    valueRange = 0f..8f
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Card Corner Radius: ${viewModel.cardBorderRadius}dp", style = MaterialTheme.typography.bodySmall)
                }
                Slider(
                    value = viewModel.cardBorderRadius.toFloat(),
                    onValueChange = { viewModel.cardBorderRadius = it.toInt() },
                    valueRange = 0f..32f
                )
            }
        }

        // Save preset action card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Save Theme Template",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = viewModel.cardPresetName,
                    onValueChange = { viewModel.cardPresetName = it },
                    label = { Text("Preset Template Name") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("designer_preset_name_field"),
                    shape = RoundedCornerShape(12.dp)
                )

                Button(
                    onClick = { viewModel.saveDesignerTheme() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("save_designer_preset_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Custom Preset")
                }
            }
        }

        // Saved Custom Presets gallery List
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "My Design Templates",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            if (themePresets.isEmpty()) {
                Text(
                    text = "No custom themes templates saved yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            } else {
                themePresets.forEach { preset ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.applySavedTheme(preset) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = preset.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Font: ${preset.fontFamily}, Size: ${preset.fontSize}, Bg: ${preset.backgroundType}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }

                            Row {
                                IconButton(onClick = { viewModel.applySavedTheme(preset) }) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Apply configuration parameters",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }

                                IconButton(onClick = { viewModel.deleteThemePreset(preset) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete setup",
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
