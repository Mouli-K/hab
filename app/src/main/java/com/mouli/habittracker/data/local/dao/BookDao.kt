package com.mouli.habittracker.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.mouli.habittracker.data.local.entity.BookEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books ORDER BY CASE WHEN status = 'READING' THEN 0 ELSE 1 END, updatedAt DESC, title ASC")
    fun observeBooks(): Flow<List<BookEntryEntity>>

    @Query("SELECT * FROM books ORDER BY updatedAt DESC")
    suspend fun getBooks(): List<BookEntryEntity>

    @Query("SELECT * FROM books WHERE sourceId = :sourceId LIMIT 1")
    suspend fun getBySourceId(sourceId: String): BookEntryEntity?

    @Query("SELECT * FROM books WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): BookEntryEntity?

    @Upsert
    suspend fun upsert(book: BookEntryEntity)

    @Upsert
    suspend fun upsertAll(books: List<BookEntryEntity>)

    @Query("DELETE FROM books WHERE id = :id")
    suspend fun deleteById(id: String)
}

