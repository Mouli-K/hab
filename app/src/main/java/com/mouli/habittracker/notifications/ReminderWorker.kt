package com.mouli.habittracker.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.mouli.habittracker.HabApplication
import com.mouli.habittracker.MainActivity
import com.mouli.habittracker.R
import com.mouli.habittracker.data.local.HabDatabase
import com.mouli.habittracker.domain.FlexibleHabitEngine
import java.time.LocalDate

class ReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val database = HabDatabase.getInstance(applicationContext)
        val profile = database.profileDao().getProfile()
        val habits = database.habitDao().getHabits()
        if (habits.isEmpty()) return Result.success()

        val logs = database.habitLogDao().getLogs()
        val today = LocalDate.now()
        val hour = java.time.LocalTime.now().hour
        val userName = profile?.displayName?.takeIf { it.isNotBlank() } ?: "friend"
        
        val (title, body) = when {
            hour in 6..10 -> {
                val quote = FlexibleHabitEngine.quoteForDay(today)
                "Good morning, $userName." to "\"${quote.text}\" — Let's have a kind day."
            }
            hour in 16..18 -> {
                "Time for a little movement?" to "A short break for your habits keeps the momentum soft and steady."
            }
            hour in 20..23 -> {
                val habits = database.habitDao().getHabits()
                val logs = database.habitLogDao().getLogs()
                val cards = habits.map { h -> 
                    FlexibleHabitEngine.buildHabitCard(h, logs.filter { it.habitId == h.id }, today)
                }
                val remaining = cards.count { !it.isComplete && !it.isFrozenToday }
                if (remaining > 0) {
                    "Day wrap-up" to "You have $remaining habits left to log. A quick check-in before you rest?"
                } else {
                    "Beautifully done" to "All your habits are tucked away for today. Sleep well, $userName."
                }
            }
            else -> {
                val quote = FlexibleHabitEngine.quoteForDay(today)
                "Hey $userName" to "\"${quote.text}\" — Checking in with your progress."
            }
        }

        val openAppIntent = Intent(applicationContext, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, HabApplication.REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_hab_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext)
            .notify((today.toEpochDay() % 10_000).toInt(), notification)

        return Result.success()
    }

    companion object {
        const val KEY_HOUR = "hour"
        const val KEY_MINUTE = "minute"
    }
}

