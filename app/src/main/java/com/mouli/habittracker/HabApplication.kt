package com.mouli.habittracker

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.google.firebase.FirebaseApp
import com.mouli.habittracker.core.AppContainer

class HabApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        container = AppContainer(this)
        createReminderChannel()
    }

    private fun createReminderChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            REMINDER_CHANNEL_ID,
            getString(R.string.reminder_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = getString(R.string.reminder_channel_description)
        }

        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val REMINDER_CHANNEL_ID = "hab_reminders"
    }
}

