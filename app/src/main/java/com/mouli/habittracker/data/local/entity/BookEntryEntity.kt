package com.mouli.habittracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mouli.habittracker.model.BookStatus
import java.util.UUID

@Entity(tableName = "books")
data class BookEntryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val sourceId: String? = null,
    val title: String,
    val author: String,
    val totalPages: Int = 0,
    val pagesRead: Int = 0,
    val status: BookStatus = BookStatus.READING,
    val coverUrl: String = "",
    val rating: Int = 0,
    val updatedAt: Long = System.currentTimeMillis()
)

