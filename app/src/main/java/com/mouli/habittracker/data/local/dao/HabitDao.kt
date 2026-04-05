package com.mouli.habittracker.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.mouli.habittracker.data.local.entity.HabitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits ORDER BY sortOrder ASC, createdAt ASC")
    fun observeHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits ORDER BY sortOrder ASC, createdAt ASC")
    suspend fun getHabits(): List<HabitEntity>

    @Upsert
    suspend fun upsertAll(habits: List<HabitEntity>)

    @Upsert
    suspend fun upsert(habit: HabitEntity)

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun deleteById(id: String)
}

