package com.mouli.habittracker.model

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

private val isoFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

enum class HabitCadence {
    DAILY,
    WEEKLY
}

enum class HabitUnit(
    val displayName: String,
    val quickAddAmount: Int,
    val shortLabel: String
) {
    CHECK_INS("check-ins", 1, "1"),
    STEPS("steps", 1_000, "1k"),
    PAGES("pages", 5, "5"),
    MINUTES("minutes", 10, "10"),
    GLASSES("glasses", 1, "1"),
    OTHER("other", 1, "1")
}

enum class MascotMood {
    SPARKLY,
    CHEERING,
    CALM,
    RESTING
}

enum class DashboardTab {
    HABITS,
    BOOKS
}

enum class BookStatus {
    READING,
    COMPLETED,
    ON_HOLD
}

data class HabitDraft(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val target: String = "1",
    val cadence: HabitCadence = HabitCadence.DAILY,
    val unit: HabitUnit = HabitUnit.CHECK_INS,
    val customUnit: String? = null,
    val trackBacklog: Boolean = false,
    val supportivePrompt: String = ""
)

data class ReminderTime(
    val hour: Int,
    val minute: Int
) {
    fun formatted(): String = "%02d:%02d".format(hour, minute)
}

data class BacklogDayUiModel(
    val date: LocalDate,
    val label: String,
    val amount: Int,
    val target: Int,
    val isComplete: Boolean,
    val isFrozen: Boolean,
    val isToday: Boolean
)

data class HabitCardUiModel(
    val id: String,
    val title: String,
    val cadenceLabel: String,
    val progressLabel: String,
    val supportLine: String,
    val streakLabel: String,
    val quickAddLabel: String,
    val quickAddAmount: Int,
    val progress: Float,
    val currentValue: Int,
    val targetValue: Int,
    val isComplete: Boolean,
    val isFrozenToday: Boolean,
    val backlogDays: List<BacklogDayUiModel>,
    val unit: HabitUnit,
    val customUnit: String? = null,
    val cadence: HabitCadence,
    val isBacklogEnabled: Boolean = false,
    val backlogBalance: Int = 0
)

data class BookSuggestionUiModel(
    val id: String,
    val title: String,
    val author: String,
    val totalPages: Int,
    val publishedYear: String,
    val coverUrl: String
)

data class BookCardUiModel(
    val id: String,
    val title: String,
    val author: String,
    val totalPages: Int,
    val pagesRead: Int,
    val progress: Float,
    val progressLabel: String,
    val status: BookStatus,
    val coverUrl: String,
    val rating: Int = 0
)

data class DashboardUiState(
    val isLoading: Boolean = true,
    val userName: String = "",
    val greeting: String = "Your panda is stretching...",
    val subGreeting: String = "Loading a gentle plan for today.",
    val mascotMood: MascotMood = MascotMood.CALM,
    val cards: List<HabitCardUiModel> = emptyList(),
    val overallProgress: Float = 0f,
    val quote: String = "",
    val quoteAuthor: String = "",
    val bookSearchQuery: String = "",
    val isBookSearchLoading: Boolean = false,
    val bookSearchError: String? = null,
    val bookSuggestions: List<BookSuggestionUiModel> = emptyList(),
    val readingBooks: List<BookCardUiModel> = emptyList(),
    val completedBooks: List<BookCardUiModel> = emptyList(),
    val onHoldBooks: List<BookCardUiModel> = emptyList(),
    val isSyncing: Boolean = false,
    val lastSyncTime: Long = 0L
)

fun LocalDate.toStorageKey(): String = format(isoFormatter)

fun String.toLocalDateOrNull(): LocalDate? = runCatching {
    LocalDate.parse(this, isoFormatter)
}.getOrNull()
