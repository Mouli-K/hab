package com.mouli.habittracker.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.mouli.habittracker.data.local.dao.HabitDao
import com.mouli.habittracker.data.local.dao.ProfileDao
import com.mouli.habittracker.data.local.entity.ProfileSeed
import com.mouli.habittracker.data.local.entity.UserProfileEntity
import com.mouli.habittracker.data.local.entity.asFirestoreMap
import com.mouli.habittracker.data.local.entity.toEntity
import com.mouli.habittracker.model.HabitCadence
import com.mouli.habittracker.model.HabitDraft
import com.mouli.habittracker.model.HabitUnit
import com.mouli.habittracker.model.ReminderTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

class ProfileRepository(
    private val profileDao: ProfileDao,
    private val habitDao: HabitDao,
    private val firestore: FirebaseFirestore
) {
    fun observeProfile(): Flow<UserProfileEntity?> = profileDao.observeProfile()

    suspend fun getProfile(): UserProfileEntity? = profileDao.getProfile()

    suspend fun beginOfflineMode() {
        val existing = profileDao.getProfile()
        val now = System.currentTimeMillis()
        profileDao.upsert(
            UserProfileEntity(
                userId = null,
                displayName = existing?.takeIf { it.userId == null }?.displayName.orEmpty(),
                notificationTimesSerialized = existing?.takeIf { it.userId == null }?.notificationTimesSerialized.orEmpty(),
                onboardingCompleted = existing?.takeIf { it.userId == null }?.onboardingCompleted == true,
                createdAt = existing?.takeIf { it.userId == null }?.createdAt ?: now,
                updatedAt = now
            )
        )
    }

    suspend fun completeOnboarding(
        userId: String?,
        seed: ProfileSeed,
        fallbackDisplayName: String? = null
    ) {
        val sanitizedHabits = seed.habits
            .filter { it.title.isNotBlank() }
        val now = System.currentTimeMillis()
        val profile = UserProfileEntity(
            userId = userId,
            displayName = seed.name.trim().ifBlank { fallbackDisplayName?.trim() ?: "Mouli" },
            notificationTimesSerialized = UserProfileEntity.serializeTimes(seed.reminders.ifEmpty { defaultReminderTimes() }),
            onboardingCompleted = true,
            createdAt = now,
            updatedAt = now
        )
        profileDao.upsert(profile)
        habitDao.upsertAll(sanitizedHabits.mapIndexed { index, draft -> draft.toEntity(index) })

        if (userId != null) {
            pushProfileToRemote(userId, profile, sanitizedHabits)
        }
    }

    suspend fun pushProfileToRemote(userId: String, profile: UserProfileEntity, habits: List<HabitDraft>) {
        firestore.collection("users").document(userId)
            .collection("profile").document("default")
            .set(mapOf(
                "displayName" to profile.displayName,
                "notificationTimes" to profile.notificationTimes().map { it.formatted() },
                "onboardingCompleted" to true,
                "updatedAt" to System.currentTimeMillis(),
                "createdAt" to profile.createdAt,
                "habits" to habits.map { it.asFirestoreMap() }
            )).await()
    }

    suspend fun syncFromRemoteIfNeeded(userId: String) {
        val snapshot = firestore.collection("users")
            .document(userId)
            .collection("profile")
            .document("default")
            .get()
            .await()

        if (!snapshot.exists()) return

        val now = System.currentTimeMillis()
        val remoteHabits = (snapshot.get("habits") as? List<*>)
            .orEmpty()
            .mapNotNull { item ->
                val map = item as? Map<*, *> ?: return@mapNotNull null
                HabitDraft(
                    id = map["id"]?.toString() ?: java.util.UUID.randomUUID().toString(),
                    title = map["title"]?.toString().orEmpty(),
                    target = (map["target"] as? Number)?.toInt()?.toString() ?: "1",
                    cadence = runCatching { HabitCadence.valueOf(map["cadence"]?.toString().orEmpty()) }.getOrDefault(HabitCadence.DAILY),
                    unit = runCatching { HabitUnit.valueOf(map["unit"]?.toString().orEmpty()) }.getOrDefault(HabitUnit.CHECK_INS),
                    customUnit = map["customUnit"]?.toString(),
                    trackBacklog = map["trackBacklog"] as? Boolean ?: false,
                    supportivePrompt = map["supportivePrompt"]?.toString().orEmpty()
                )
            }

        // 1. Sync Profile
        profileDao.upsert(
            UserProfileEntity(
                userId = userId,
                displayName = snapshot.getString("displayName") ?: "Mouli",
                notificationTimesSerialized = UserProfileEntity.serializeTimes(
                    (snapshot.get("notificationTimes") as? List<*>)?.mapNotNull { it?.toString() }?.map {
                        val p = it.split(":"); ReminderTime(p[0].toInt(), p[1].toInt())
                    } ?: defaultReminderTimes()
                ),
                onboardingCompleted = true,
                createdAt = snapshot.getLong("createdAt") ?: now,
                updatedAt = now
            )
        )

        // 2. Sync Habits
        habitDao.upsertAll(remoteHabits.mapIndexed { i, d -> d.toEntity(i) })
    }

    companion object {
        fun defaultHabitDrafts(): List<HabitDraft> = listOf(
            HabitDraft(
                title = "Daily check-in",
                target = "1",
                cadence = HabitCadence.DAILY,
                unit = HabitUnit.CHECK_INS,
                supportivePrompt = "A small moment to breathe and reflect."
            )
        )

        fun defaultReminderTimes(): List<ReminderTime> = listOf(
            ReminderTime(hour = 8, minute = 0),
            ReminderTime(hour = 20, minute = 0)
        )
    }
}
