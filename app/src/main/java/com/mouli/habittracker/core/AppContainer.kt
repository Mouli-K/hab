package com.mouli.habittracker.core

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mouli.habittracker.data.local.HabDatabase
import com.mouli.habittracker.data.repository.AuthRepository
import com.mouli.habittracker.data.repository.BookRepository
import com.mouli.habittracker.data.repository.HabitRepository
import com.mouli.habittracker.data.repository.ProfileRepository
import com.mouli.habittracker.notifications.ReminderScheduler

class AppContainer(context: Context) {
    private val database = HabDatabase.getInstance(context)
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    val authRepository = AuthRepository(auth)
    val profileRepository = ProfileRepository(
        profileDao = database.profileDao(),
        habitDao = database.habitDao(),
        firestore = firestore
    )
    val habitRepository = HabitRepository(
        habitDao = database.habitDao(),
        habitLogDao = database.habitLogDao(),
        bookDao = database.bookDao()
    )
    val bookRepository = BookRepository(
        bookDao = database.bookDao()
    )
    val reminderScheduler = ReminderScheduler(context)
}
