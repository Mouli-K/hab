package com.mouli.habittracker.data.local

import androidx.room.TypeConverter
import com.mouli.habittracker.model.BookStatus
import com.mouli.habittracker.model.HabitCadence
import com.mouli.habittracker.model.HabitUnit

class Converters {
    @TypeConverter
    fun fromCadence(value: HabitCadence): String = value.name

    @TypeConverter
    fun toCadence(value: String): HabitCadence = HabitCadence.valueOf(value)

    @TypeConverter
    fun fromUnit(value: HabitUnit): String = value.name

    @TypeConverter
    fun toUnit(value: String): HabitUnit = HabitUnit.valueOf(value)

    @TypeConverter
    fun fromBookStatus(value: BookStatus): String = value.name

    @TypeConverter
    fun toBookStatus(value: String): BookStatus = BookStatus.valueOf(value)
}
