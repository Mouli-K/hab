package com.mouli.habittracker.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.mouli.habittracker.data.local.entity.HabitLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitLogDao {
    @Query("SELECT * FROM habit_logs")
    fun observeLogs(): Flow<List<HabitLogEntity>>

    @Query("SELECT * FROM habit_logs")
    suspend fun getLogs(): List<HabitLogEntity>

    @Query("SELECT * FROM habit_logs WHERE date = :date")
    suspend fun getLogsForDate(date: String): List<HabitLogEntity>

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId AND date = :date LIMIT 1")
    suspend fun getLog(habitId: String, date: String): HabitLogEntity?

    @Upsert
    suspend fun upsert(log: HabitLogEntity)

    @Upsert
    suspend fun upsertAll(logs: List<HabitLogEntity>)

    @Query("DELETE FROM habit_logs WHERE habitId = :habitId")
    suspend fun deleteLogsByHabitId(habitId: String)
}

