package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reading_logs")
data class ReadingLog(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val quoteId: String,
    val text: String,
    val author: String,
    val category: String,
    val timestamp: Long = System.currentTimeMillis()
)
