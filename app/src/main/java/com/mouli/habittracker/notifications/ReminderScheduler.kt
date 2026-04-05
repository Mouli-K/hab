package com.mouli.habittracker.notifications

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.mouli.habittracker.model.ReminderTime
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

class ReminderScheduler(
    private val context: Context
) {
    fun schedule() {
        val manager = WorkManager.getInstance(context)
        val slots = listOf(
            8 to 0,   // Morning Greeting
            17 to 0,  // Activity Reminder
            21 to 0   // Status Log (9 PM IST)
        )

        slots.forEachIndexed { index, (hour, minute) ->
            val request = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(initialDelay(hour, minute), TimeUnit.MILLISECONDS)
                .build()

            manager.enqueueUniquePeriodicWork(
                "hab_auto_reminder_$index",
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }
    }

    private fun initialDelay(hour: Int, minute: Int): Long {
        val now = LocalDateTime.now()
        var next = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (!next.isAfter(now)) {
            next = next.plusDays(1)
        }
        return Duration.between(now, next).toMillis()
    }
}

