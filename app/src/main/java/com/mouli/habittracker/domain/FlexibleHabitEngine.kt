package com.mouli.habittracker.domain

import com.mouli.habittracker.data.local.entity.HabitEntity
import com.mouli.habittracker.data.local.entity.HabitLogEntity
import com.mouli.habittracker.model.BacklogDayUiModel
import com.mouli.habittracker.model.HabitCadence
import com.mouli.habittracker.model.HabitCardUiModel
import com.mouli.habittracker.model.MascotMood
import com.mouli.habittracker.model.toStorageKey
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

data class QuoteLine(
    val text: String,
    val author: String
)

object FlexibleHabitEngine {
    val literaryQuotes: List<QuoteLine> = listOf(
        QuoteLine("The happiness of your life depends upon the quality of your thoughts.", "Marcus Aurelius"),
        QuoteLine("Go confidently in the direction of your dreams.", "Henry David Thoreau"),
        QuoteLine("No act of kindness, no matter how small, is ever wasted.", "Aesop"),
        QuoteLine("There is no charm equal to tenderness of heart.", "Jane Austen"),
        QuoteLine("We are what we repeatedly do.", "Will Durant"),
        QuoteLine("Little by little, one travels far.", "J.R.R. Tolkien"),
        QuoteLine("To live is to find the beauty in the ordinary.", "Fyodor Dostoevsky"),
        QuoteLine("The soul is healed by being with children.", "Fyodor Dostoevsky"),
        QuoteLine("Beauty will save the world.", "Fyodor Dostoevsky"),
        QuoteLine("It is not the brains that matter most, but that which guides them.", "Fyodor Dostoevsky")
    )

    fun buildHabitCard(
        habit: HabitEntity,
        logs: List<HabitLogEntity>,
        today: LocalDate
    ): HabitCardUiModel {
        val logMap = logs.associateBy { it.date }
        val todayKey = today.toStorageKey()
        val todayLog = logMap[todayKey]
        val currentValue = when (habit.cadence) {
            HabitCadence.DAILY -> todayLog?.amount ?: 0
            HabitCadence.WEEKLY -> weekRange(today).sumOf { day -> logMap[day.toStorageKey()]?.amount ?: 0 }
        }
        val targetValue = habit.target
        val isComplete = currentValue >= targetValue
        
        val unitLabel = habit.customUnit ?: habit.unit.displayName
        
        val backlogDays = weekRange(today).map { date ->
            val log = logMap[date.toStorageKey()]
            val amount = log?.amount ?: 0
            val target = if (habit.cadence == HabitCadence.DAILY) habit.target else 1
            BacklogDayUiModel(
                date = date,
                label = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                amount = amount,
                target = target,
                isComplete = if (habit.cadence == HabitCadence.DAILY) {
                    amount >= habit.target
                } else {
                    amount > 0
                },
                isFrozen = log?.isFrozen == true,
                isToday = date == today
            )
        }
        
        var backlogBalance = 0
        if (habit.trackBacklog) {
            val createdDate = Instant.ofEpochMilli(habit.createdAt)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            
            var cursor = createdDate
            while (cursor.isBefore(today)) {
                val log = logMap[cursor.toStorageKey()]
                if (log?.isFrozen != true) {
                    val amount = log?.amount ?: 0
                    backlogBalance += (amount - habit.target)
                }
                cursor = cursor.plusDays(1)
            }
        }

        val streak = calculateStreak(habit, logMap, today)
        val cadenceLabel = when (habit.cadence) {
            HabitCadence.DAILY -> "${habit.target} $unitLabel each day"
            HabitCadence.WEEKLY -> "${habit.target} $unitLabel this week"
        }
        val progressLabel = when (habit.cadence) {
            HabitCadence.DAILY -> "$currentValue/${habit.target} $unitLabel"
            HabitCadence.WEEKLY -> "$currentValue/${habit.target} $unitLabel this week"
        }
        val progress = (currentValue.toFloat() / targetValue.toFloat()).coerceIn(0f, 1f)
        val isFrozenToday = todayLog?.isFrozen == true
        val supportLine = when {
            isFrozenToday -> "Today is safely tucked into a freeze. Your streak stays warm."
            isComplete -> "Beautiful work. Panda is already bragging about this one."
            habit.cadence == HabitCadence.WEEKLY -> {
                val remaining = (habit.target - currentValue).coerceAtLeast(0)
                if (remaining == 0) {
                    "This week's goal is already wrapped up."
                } else {
                    "$remaining $unitLabel left this week. Plenty of room to catch up gently."
                }
            }
            else -> {
                val remaining = (habit.target - currentValue).coerceAtLeast(0)
                "$remaining $unitLabel left today. A soft push is enough."
            }
        }
        val streakLabel = when (habit.cadence) {
            HabitCadence.DAILY -> "$streak day glow"
            HabitCadence.WEEKLY -> "$streak week rhythm"
        }

        return HabitCardUiModel(
            id = habit.id,
            title = habit.title,
            cadenceLabel = cadenceLabel,
            progressLabel = progressLabel,
            supportLine = supportLine,
            streakLabel = streakLabel,
            quickAddLabel = habit.unit.shortLabel,
            quickAddAmount = habit.unit.quickAddAmount,
            progress = progress,
            currentValue = currentValue,
            targetValue = targetValue,
            isComplete = isComplete,
            isFrozenToday = isFrozenToday,
            backlogDays = backlogDays,
            unit = habit.unit,
            customUnit = habit.customUnit,
            cadence = habit.cadence,
            isBacklogEnabled = habit.trackBacklog,
            backlogBalance = backlogBalance
        )
    }

    fun buildGreeting(name: String, cards: List<HabitCardUiModel>): Pair<String, String> {
        val firstIncomplete = cards.firstOrNull { !it.isComplete && !it.isFrozenToday }
        val cleanName = name.trim().takeUnless { it.equals("Friend", ignoreCase = true) || it.equals("You", ignoreCase = true) }.orEmpty()
        return if (cleanName.isBlank()) {
            "A calm little dashboard for today" to "Your panda picked a cozy pace and left lots of breathing room."
        } else if (firstIncomplete != null) {
            "$cleanName, ready for ${firstIncomplete.title.lowercase()}?" to firstIncomplete.supportLine
        } else {
            "$cleanName, your habits are glowing." to "This is a lovely time to rest, freeze a day, or celebrate a streak."
        }
    }

    fun pickMood(cards: List<HabitCardUiModel>): MascotMood {
        if (cards.isEmpty()) return MascotMood.RESTING
        val average = cards.map { it.progress }.average().toFloat()
        val anyFrozen = cards.any { it.isFrozenToday }
        return when {
            average >= 0.95f -> MascotMood.SPARKLY
            average >= 0.55f -> MascotMood.CHEERING
            anyFrozen -> MascotMood.CALM
            else -> MascotMood.RESTING
        }
    }

    fun quoteForDay(day: LocalDate): QuoteLine =
        literaryQuotes[(day.toEpochDay().mod(literaryQuotes.size.toLong())).toInt()]

    private fun calculateStreak(
        habit: HabitEntity,
        logs: Map<String, HabitLogEntity>,
        today: LocalDate
    ): Int = when (habit.cadence) {
        HabitCadence.DAILY -> calculateDailyStreak(habit, logs, today)
        HabitCadence.WEEKLY -> calculateWeeklyStreak(habit, logs, today)
    }

    private fun calculateDailyStreak(
        habit: HabitEntity,
        logs: Map<String, HabitLogEntity>,
        today: LocalDate
    ): Int {
        var streak = 0
        var cursor = today
        
        val todayLog = logs[today.toStorageKey()]
        if (todayLog == null || (todayLog.amount < habit.target && !todayLog.isFrozen)) {
            cursor = today.minusDays(1)
        }

        val createdDate = Instant.ofEpochMilli(habit.createdAt)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            
        while (true) {
            if (cursor.isBefore(createdDate)) return streak
            val log = logs[cursor.toStorageKey()]
            when {
                log?.isFrozen == true -> cursor = cursor.minusDays(1)
                (log?.amount ?: 0) >= habit.target -> {
                    streak += 1
                    cursor = cursor.minusDays(1)
                }
                else -> return streak
            }
        }
    }

    private fun calculateWeeklyStreak(
        habit: HabitEntity,
        logs: Map<String, HabitLogEntity>,
        today: LocalDate
    ): Int {
        var streak = 0
        var cursor = startOfWeek(today)
        
        val week = weekRange(cursor)
        val amount = week.sumOf { day -> logs[day.toStorageKey()]?.amount ?: 0 }
        val frozenWeek = week.any { day -> logs[day.toStorageKey()]?.isFrozen == true }
        if (amount < habit.target && !frozenWeek) {
            cursor = cursor.minusWeeks(1)
        }

        val createdDate = Instant.ofEpochMilli(habit.createdAt)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        while (true) {
            if (cursor.plusDays(6).isBefore(createdDate)) return streak
            val w = weekRange(cursor)
            val a = w.sumOf { day -> logs[day.toStorageKey()]?.amount ?: 0 }
            val f = w.any { day -> logs[day.toStorageKey()]?.isFrozen == true }
            when {
                a >= habit.target -> {
                    streak += 1
                    cursor = cursor.minusWeeks(1)
                }
                f -> cursor = cursor.minusWeeks(1)
                else -> return streak
            }
        }
    }

    private fun weekRange(anchor: LocalDate): List<LocalDate> {
        val start = startOfWeek(anchor)
        return (0..6).map { offset -> start.plusDays(offset.toLong()) }
    }

    fun startOfWeek(anchor: LocalDate): LocalDate {
        var cursor = anchor
        while (cursor.dayOfWeek != DayOfWeek.MONDAY) {
            cursor = cursor.minusDays(1)
        }
        return cursor
    }
}
