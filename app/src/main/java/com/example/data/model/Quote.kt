package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quotes")
data class Quote(
    @PrimaryKey
    val id: String,
    val text: String,
    val author: String,
    val category: String,
    val isFavorite: Boolean = false,
    val isGenerated: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val localNote: String? = null // For user notes on favorited quotes
)
