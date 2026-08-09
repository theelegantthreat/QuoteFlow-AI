package com.example.data.database

import androidx.room.*
import com.example.data.model.AppSetting
import com.example.data.model.CardThemePreset
import com.example.data.model.Quote
import com.example.data.model.ReadingLog
import kotlinx.coroutines.flow.Flow

@Dao
interface QuoteDao {

    // --- Quotes ---
    @Query("SELECT * FROM quotes ORDER BY timestamp DESC")
    fun getAllQuotes(): Flow<List<Quote>>

    @Query("SELECT * FROM quotes WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoriteQuotes(): Flow<List<Quote>>

    @Query("SELECT * FROM quotes WHERE category = :category ORDER BY timestamp DESC")
    fun getQuotesByCategory(category: String): Flow<List<Quote>>

    @Query("SELECT * FROM quotes WHERE text LIKE '%' || :query || '%' OR author LIKE '%' || :query || '%' ORDER BY timestamp DESC")
    fun searchQuotes(query: String): Flow<List<Quote>>

    @Query("SELECT * FROM quotes WHERE id = :id LIMIT 1")
    suspend fun getQuoteById(id: String): Quote?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuote(quote: Quote)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuotes(quotes: List<Quote>)

    @Query("UPDATE quotes SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteStatus(id: String, isFavorite: Boolean)

    @Query("UPDATE quotes SET localNote = :note WHERE id = :id")
    suspend fun updateQuoteNote(id: String, note: String?)

    @Delete
    suspend fun deleteQuote(quote: Quote)

    // --- Theme Presets ---
    @Query("SELECT * FROM theme_presets")
    fun getAllThemePresets(): Flow<List<CardThemePreset>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertThemePreset(preset: CardThemePreset)

    @Query("DELETE FROM theme_presets WHERE id = :id")
    suspend fun deleteThemePreset(id: String)

    // --- Reading Logs (Analytics & Timeline) ---
    @Query("SELECT * FROM reading_logs ORDER BY timestamp DESC")
    fun getReadingLogs(): Flow<List<ReadingLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReadingLog(log: ReadingLog)

    @Query("SELECT COUNT(*) FROM reading_logs")
    fun getReadingLogsCount(): Flow<Int>

    // Group-by queries are handled perfectly inside Kotlin by mapping lists returned,
    // which avoids tricky SQLite aggregation schema failures and is completely dynamic.

    // --- App Settings ---
    @Query("SELECT * FROM app_settings WHERE `key` = :key LIMIT 1")
    suspend fun getSetting(key: String): AppSetting?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSetting(setting: AppSetting)
}
