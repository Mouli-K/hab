package com.mouli.habittracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mouli.habittracker.model.HabitDraft
import com.mouli.habittracker.model.HabitUnit
import com.mouli.habittracker.model.ReminderTime

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey val id: Int = 0,
    val userId: String? = null,
    val displayName: String = "",
    val notificationTimesSerialized: String = "",
    val onboardingCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun notificationTimes(): List<ReminderTime> =
        notificationTimesSerialized
            .split("|")
            .filter { it.isNotBlank() }
            .mapNotNull { token ->
                val parts = token.split(":")
                val hour = parts.getOrNull(0)?.toIntOrNull()
                val minute = parts.getOrNull(1)?.toIntOrNull()
                if (hour == null || minute == null) null else ReminderTime(hour, minute)
            }

    companion object {
        fun serializeTimes(times: List<ReminderTime>): String =
            times.joinToString("|") { it.formatted() }
    }
}

data class ProfileSeed(
    val name: String,
    val reminders: List<ReminderTime>,
    val habits: List<HabitDraft>
)

fun HabitDraft.asFirestoreMap(): Map<String, Any?> = mapOf(
    "id" to id,
    "title" to title.trim(),
    "target" to target.toIntOrNull().coerceAtLeastOrFallback(1),
    "cadence" to cadence.name,
    "unit" to unit.name,
    "customUnit" to customUnit,
    "trackBacklog" to trackBacklog,
    "supportivePrompt" to supportivePrompt.ifBlank {
        when (unit) {
            HabitUnit.STEPS -> "Take a few soft steps whenever the day gives you room."
            HabitUnit.PAGES -> "A page or two still counts as a warm little win."
            HabitUnit.MINUTES -> "A short burst keeps the habit feeling kind."
            HabitUnit.GLASSES -> "Small sips build the rhythm."
            HabitUnit.CHECK_INS -> "Showing up once is already momentum."
            HabitUnit.OTHER -> "Every bit of effort matters."
        }
    }
)

private fun Int?.coerceAtLeastOrFallback(fallback: Int): Int = when {
    this == null -> fallback
    this < fallback -> fallback
    else -> this
}

