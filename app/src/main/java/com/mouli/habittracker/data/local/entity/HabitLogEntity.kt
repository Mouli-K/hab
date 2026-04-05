package com.mouli.habittracker.data.local.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "habit_logs",
    primaryKeys = ["habitId", "date"],
    indices = [Index("date")]
)
data class HabitLogEntity(
    val habitId: String,
    val date: String,
    val amount: Int = 0,
    val isFrozen: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

