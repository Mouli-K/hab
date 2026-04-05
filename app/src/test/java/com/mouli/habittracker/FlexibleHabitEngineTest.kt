package com.mouli.habittracker

import com.mouli.habittracker.data.local.entity.HabitEntity
import com.mouli.habittracker.data.local.entity.HabitLogEntity
import com.mouli.habittracker.domain.FlexibleHabitEngine
import com.mouli.habittracker.model.HabitCadence
import com.mouli.habittracker.model.HabitUnit
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FlexibleHabitEngineTest {
    @Test
    fun freezeKeepsDailyStreakAlive() {
        val today = LocalDate.of(2026, 4, 4)
        val habit = HabitEntity(
            id = "reading",
            title = "Reading",
            cadence = HabitCadence.DAILY,
            target = 10,
            unit = HabitUnit.PAGES,
            supportivePrompt = "Keep it cozy.",
            createdAt = today.minusDays(10).atStartOfDay().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        val logs = listOf(
            HabitLogEntity(habitId = habit.id, date = today.toString(), amount = 12),
            HabitLogEntity(habitId = habit.id, date = today.minusDays(1).toString(), isFrozen = true),
            HabitLogEntity(habitId = habit.id, date = today.minusDays(2).toString(), amount = 11)
        )

        val card = FlexibleHabitEngine.buildHabitCard(habit, logs, today)

        assertEquals("2 day glow", card.streakLabel)
        assertTrue(card.isComplete)
    }

    @Test
    fun streakIncludesYesterdayEvenIfTodayIsNotComplete() {
        val today = LocalDate.of(2026, 4, 4)
        val habit = HabitEntity(
            id = "reading",
            title = "Reading",
            cadence = HabitCadence.DAILY,
            target = 10,
            unit = HabitUnit.PAGES,
            supportivePrompt = "Keep it cozy.",
            createdAt = today.minusDays(10).atStartOfDay().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        val logs = listOf(
            // today incomplete
            HabitLogEntity(habitId = habit.id, date = today.toString(), amount = 2),
            HabitLogEntity(habitId = habit.id, date = today.minusDays(1).toString(), amount = 11)
        )

        val card = FlexibleHabitEngine.buildHabitCard(habit, logs, today)

        // Streak should still be 1 (from yesterday)
        assertEquals("1 day glow", card.streakLabel)
        assertTrue(!card.isComplete)
    }

    @Test
    fun streakIncrementsWhenTodayIsComplete() {
        val today = LocalDate.of(2026, 4, 4)
        val habit = HabitEntity(
            id = "reading",
            title = "Reading",
            cadence = HabitCadence.DAILY,
            target = 10,
            unit = HabitUnit.PAGES,
            supportivePrompt = "Keep it cozy.",
            createdAt = today.minusDays(10).atStartOfDay().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        val logs = listOf(
            HabitLogEntity(habitId = habit.id, date = today.toString(), amount = 12),
            HabitLogEntity(habitId = habit.id, date = today.minusDays(1).toString(), amount = 11)
        )

        val card = FlexibleHabitEngine.buildHabitCard(habit, logs, today)

        assertEquals("2 day glow", card.streakLabel)
        assertTrue(card.isComplete)
    }
}
