package com.mouli.habittracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.TypeConverters
import com.mouli.habittracker.data.local.dao.BookDao
import com.mouli.habittracker.data.local.dao.HabitDao
import com.mouli.habittracker.data.local.dao.HabitLogDao
import com.mouli.habittracker.data.local.dao.ProfileDao
import com.mouli.habittracker.data.local.entity.BookEntryEntity
import com.mouli.habittracker.data.local.entity.HabitEntity
import com.mouli.habittracker.data.local.entity.HabitLogEntity
import com.mouli.habittracker.data.local.entity.UserProfileEntity

@Database(
    entities = [UserProfileEntity::class, HabitEntity::class, HabitLogEntity::class, BookEntryEntity::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class HabDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun habitDao(): HabitDao
    abstract fun habitLogDao(): HabitLogDao
    abstract fun bookDao(): BookDao

    companion object {
        @Volatile
        private var instance: HabDatabase? = null

        private val migration1To2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS books (
                        id TEXT NOT NULL PRIMARY KEY,
                        sourceId TEXT,
                        title TEXT NOT NULL,
                        author TEXT NOT NULL,
                        totalPages INTEGER NOT NULL,
                        pagesRead INTEGER NOT NULL,
                        status TEXT NOT NULL,
                        coverUrl TEXT NOT NULL,
                        rating INTEGER NOT NULL DEFAULT 0,
                        updatedAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        private val migration2To3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE habits ADD COLUMN customUnit TEXT")
                database.execSQL("ALTER TABLE habits ADD COLUMN trackBacklog INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getInstance(context: Context): HabDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    HabDatabase::class.java,
                    "hab_database"
                )
                    .addMigrations(migration1To2, migration2To3)
                    .build()
                    .also { instance = it }
            }
    }
}
