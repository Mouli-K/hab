package com.mouli.habittracker.data.repository

import com.mouli.habittracker.data.local.dao.HabitDao
import com.mouli.habittracker.data.local.dao.HabitLogDao
import com.mouli.habittracker.data.local.entity.HabitLogEntity
import com.mouli.habittracker.data.local.entity.asFirestoreMap
import com.mouli.habittracker.data.local.entity.toEntity
import com.mouli.habittracker.domain.FlexibleHabitEngine
import com.mouli.habittracker.model.HabitCardUiModel
import com.mouli.habittracker.model.HabitDraft
import com.mouli.habittracker.model.toStorageKey
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.tasks.await

class HabitRepository(
    private val habitDao: HabitDao,
    private val habitLogDao: HabitLogDao,
    private val bookDao: com.mouli.habittracker.data.local.dao.BookDao
) {
    fun observeHabitCards(today: LocalDate = LocalDate.now()): Flow<List<HabitCardUiModel>> =
        combine(
            habitDao.observeHabits(), 
            habitLogDao.observeLogs(),
            bookDao.observeBooks()
        ) { habits, logs, books ->
            val habitCards = habits.map { habit ->
                FlexibleHabitEngine.buildHabitCard(
                    habit = habit,
                    logs = logs.filter { it.habitId == habit.id },
                    today = today
                )
            }
            val readingBookCards = books
                .filter { it.status == com.mouli.habittracker.model.BookStatus.READING }
                .map { book ->
                    HabitCardUiModel(
                        id = book.id,
                        title = "Reading: ${book.title}",
                        cadence = com.mouli.habittracker.model.HabitCadence.DAILY,
                        targetValue = if (book.totalPages > 0) book.totalPages else 100,
                        currentValue = book.pagesRead,
                        unit = com.mouli.habittracker.model.HabitUnit.PAGES,
                        progress = if (book.totalPages > 0) book.pagesRead.toFloat() / book.totalPages.toFloat() else 0f,
                        progressLabel = "${book.pagesRead} pages",
                        streakLabel = "Reading shelf",
                        cadenceLabel = "Daily reading",
                        isComplete = book.totalPages > 0 && book.pagesRead >= book.totalPages,
                        isFrozenToday = false,
                        supportLine = "Your progress with ${book.author} grows each day.",
                        quickAddAmount = 10,
                        quickAddLabel = "10",
                        backlogDays = emptyList() // Simplified for books in tracker
                    )
                }
            habitCards + readingBookCards
        }

    suspend fun addHabit(draft: HabitDraft) {
        val habits = habitDao.getHabits()
        val sortOrder = if (draft.id.isBlank()) habits.size else {
            habits.find { it.id == draft.id }?.sortOrder ?: habits.size
        }
        
        // Ensure we use the same ID for both local and remote
        val finalDraft = if (draft.id.isBlank()) draft.copy(id = java.util.UUID.randomUUID().toString()) else draft
        val entity = finalDraft.toEntity(sortOrder)
        
        habitDao.upsert(entity)
        
        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            android.util.Log.d("HabitRepo", "Pushing habit ${entity.title} to Firestore")
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users").document(userId)
                .collection("profile").document("default")
                .update("habits", com.google.firebase.firestore.FieldValue.arrayUnion(finalDraft.asFirestoreMap()))
                .await()
        }
    }

    suspend fun updateHabit(draft: HabitDraft) {
        addHabit(draft)
    }

    suspend fun deleteHabit(habitId: String) {
        habitDao.deleteById(habitId)
        habitLogDao.deleteLogsByHabitId(habitId)
    }

    suspend fun getHabitCards(today: LocalDate = LocalDate.now()): List<HabitCardUiModel> {
        val habits = habitDao.getHabits()
        val logs = habitLogDao.getLogs()
        return habits.map { habit ->
            FlexibleHabitEngine.buildHabitCard(
                habit = habit,
                logs = logs.filter { it.habitId == habit.id },
                today = today
            )
        }
    }

    suspend fun incrementHabit(habitId: String, date: LocalDate, delta: Int) {
        val key = date.toStorageKey()
        val existing = habitLogDao.getLog(habitId, key)
        val newAmount = ((existing?.amount ?: 0) + delta).coerceAtLeast(0)
        val log = HabitLogEntity(
            habitId = habitId,
            date = key,
            amount = newAmount,
            isFrozen = existing?.isFrozen == true,
            updatedAt = System.currentTimeMillis()
        )
        habitLogDao.upsert(log)
        pushLogToRemote(habitId, log)
    }

    suspend fun toggleFreeze(habitId: String, date: LocalDate) {
        val key = date.toStorageKey()
        val existing = habitLogDao.getLog(habitId, key)
        val log = HabitLogEntity(
            habitId = habitId,
            date = key,
            amount = existing?.amount ?: 0,
            isFrozen = !(existing?.isFrozen ?: false),
            updatedAt = System.currentTimeMillis()
        )
        habitLogDao.upsert(log)
        pushLogToRemote(habitId, log)
    }

    suspend fun setAmount(habitId: String, date: LocalDate, amount: Int) {
        val key = date.toStorageKey()
        val existing = habitLogDao.getLog(habitId, key)
        val log = HabitLogEntity(
            habitId = habitId,
            date = key,
            amount = amount.coerceAtLeast(0),
            isFrozen = existing?.isFrozen == true,
            updatedAt = System.currentTimeMillis()
        )
        habitLogDao.upsert(log)
        pushLogToRemote(habitId, log)
    }

    suspend fun setFreeze(habitId: String, date: LocalDate, frozen: Boolean) {
        val key = date.toStorageKey()
        val existing = habitLogDao.getLog(habitId, key)
        val log = HabitLogEntity(
            habitId = habitId,
            date = key,
            amount = existing?.amount ?: 0,
            isFrozen = frozen,
            updatedAt = System.currentTimeMillis()
        )
        habitLogDao.upsert(log)
        pushLogToRemote(habitId, log)
    }

    private suspend fun pushLogToRemote(habitId: String, log: HabitLogEntity) {
        val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
            .collection("users").document(userId)
            .collection("habits").document(habitId)
            .collection("logs").document(log.date)
            .set(mapOf(
                "amount" to log.amount,
                "isFrozen" to log.isFrozen,
                "updatedAt" to log.updatedAt
            ))
    }

    suspend fun syncLogsFromRemote(userId: String) {
        val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
        val habits = habitDao.getHabits()
        android.util.Log.d("HabitRepo", "Starting log sync for ${habits.size} habits")
        habits.forEach { habit ->
            val logsSnapshot = firestore.collection("users").document(userId)
                .collection("habits").document(habit.id)
                .collection("logs")
                .get()
                .await()

            val remoteLogs = logsSnapshot.documents.mapNotNull { doc ->
                HabitLogEntity(
                    habitId = habit.id,
                    date = doc.id,
                    amount = (doc.get("amount") as? Number)?.toInt() ?: 0,
                    isFrozen = doc.getBoolean("isFrozen") ?: false,
                    updatedAt = doc.getLong("updatedAt") ?: 0L
                )
            }
            if (remoteLogs.isNotEmpty()) {
                android.util.Log.d("HabitRepo", "Restored ${remoteLogs.size} logs for ${habit.title}")
                habitLogDao.upsertAll(remoteLogs)
            }
        }
    }
}

