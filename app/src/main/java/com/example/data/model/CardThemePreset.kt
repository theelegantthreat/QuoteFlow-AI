package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "theme_presets")
data class CardThemePreset(
    @PrimaryKey
    val id: String,
    val name: String,
    val fontFamily: String = "Serif",
    val fontSize: Int = 22,
    val textColor: String = "#FFFFFF",
    val backgroundType: String = "SOLID", // "SOLID", "GRADIENT", "IMAGE"
    val backgroundValue: String = "#1E1E1E", // Solid color HEX, gradient key representation, or drawing path
    val alignment: String = "CENTER", // "LEFT", "CENTER", "RIGHT"
    val showShadow: Boolean = false,
    val shadowColor: String = "#000000",
    val borderWidth: Int = 0,
    val borderColor: String = "#FFFFFF",
    val borderRadius: Int = 16,
    val backgroundOpacity: Float = 1.0f,
    val backgroundBlur: Int = 0
)
