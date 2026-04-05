package com.mouli.habittracker.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.mouli.habittracker.data.local.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM user_profile WHERE id = 0")
    fun observeProfile(): Flow<UserProfileEntity?>

    @Query("SELECT * FROM user_profile WHERE id = 0")
    suspend fun getProfile(): UserProfileEntity?

    @Upsert
    suspend fun upsert(profile: UserProfileEntity)
}

