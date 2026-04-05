package com.mouli.habittracker.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.mouli.habittracker.model.HabitCadence
import com.mouli.habittracker.model.HabitDraft
import com.mouli.habittracker.model.HabitUnit
import java.util.UUID

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val cadence: HabitCadence,
    val target: Int,
    val unit: HabitUnit,
    val customUnit: String? = null,
    val trackBacklog: Boolean = false,
    val supportivePrompt: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val sortOrder: Int = 0
)

fun HabitDraft.toEntity(sortOrder: Int): HabitEntity {
    val safeTarget = target.toIntOrNull()?.coerceAtLeast(1) ?: 1
    val fallbackPrompt = supportivePrompt.ifBlank {
        when (unit) {
            HabitUnit.STEPS -> "Every extra step gives the panda a little bounce."
            HabitUnit.PAGES -> "A chapter grows page by page."
            HabitUnit.MINUTES -> "A focused pocket of time is enough for today."
            HabitUnit.GLASSES -> "Hydration counts in quiet ways too."
            HabitUnit.CHECK_INS -> "Showing up is the whole point."
            HabitUnit.OTHER -> "Every small bit of progress matters."
        }
    }

    return HabitEntity(
        id = if (id.isBlank()) UUID.randomUUID().toString() else id,
        title = title.trim().ifBlank { "Gentle Habit" },
        cadence = cadence,
        target = safeTarget,
        unit = unit,
        customUnit = customUnit,
        trackBacklog = trackBacklog,
        supportivePrompt = fallbackPrompt,
        sortOrder = sortOrder
    )
}

