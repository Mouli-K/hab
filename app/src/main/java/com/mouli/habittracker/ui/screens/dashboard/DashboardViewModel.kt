package com.mouli.habittracker.ui.screens.dashboard

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mouli.habittracker.core.AppContainer
import com.mouli.habittracker.model.BookStatus
import com.mouli.habittracker.model.BookSuggestionUiModel
import com.mouli.habittracker.domain.FlexibleHabitEngine
import com.mouli.habittracker.model.BookCardUiModel
import com.mouli.habittracker.model.DashboardUiState
import com.mouli.habittracker.model.HabitDraft
import com.mouli.habittracker.widget.HabWidget
import java.time.LocalDate
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class DashboardViewModel(
    private val appContext: Context,
    private val container: AppContainer
) : ViewModel() {
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState = _uiState.asStateFlow()
    private val bookSearchQuery = MutableStateFlow("")
    private val bookSuggestions = MutableStateFlow<List<BookSuggestionUiModel>>(emptyList())
    private val isBookSearchLoading = MutableStateFlow(false)
    private val bookSearchError = MutableStateFlow<String?>(null)
    private val isSyncing = MutableStateFlow(false)
    private val lastSyncTime = MutableStateFlow(0L)

    init {
        viewModelScope.launch {
            bookSearchQuery
                .debounce(350)
                .distinctUntilChanged()
                .collectLatest { query ->
                    if (query.trim().length < 2) {
                        bookSuggestions.value = emptyList()
                        bookSearchError.value = null
                        isBookSearchLoading.value = false
                        return@collectLatest
                    }

                    isBookSearchLoading.value = true
                    bookSearchError.value = null
                    runCatching { container.bookRepository.searchBooks(query) }
                        .onSuccess { suggestions ->
                            bookSuggestions.value = suggestions
                            bookSearchError.value = if (suggestions.isEmpty()) {
                                "No matching books yet. Try a full title or author."
                            } else {
                                null
                            }
                        }
                        .onFailure {
                            bookSuggestions.value = emptyList()
                            bookSearchError.value = "Book suggestions need a quick network check right now."
                        }
                    isBookSearchLoading.value = false
                }
        }

        viewModelScope.launch {
            val dashboardFlow = combine(
                container.profileRepository.observeProfile(),
                container.habitRepository.observeHabitCards(),
                container.bookRepository.observeBooks()
            ) { profile, cards, books ->
                val name = profile?.displayName
                    ?.takeUnless { it.equals("Friend", ignoreCase = true) }
                    .orEmpty()
                val quote = FlexibleHabitEngine.quoteForDay(LocalDate.now())
                val (greeting, subGreeting) = FlexibleHabitEngine.buildGreeting(name, cards)
                val readingBooks = books
                    .filter { it.status == BookStatus.READING }
                    .map { book ->
                        val progress = if (book.totalPages > 0) {
                            book.pagesRead.toFloat() / book.totalPages.toFloat()
                        } else if (book.pagesRead > 0) {
                            1f
                        } else {
                            0f
                        }.coerceIn(0f, 1f)

                        BookCardUiModel(
                            id = book.id,
                            title = book.title,
                            author = book.author,
                            totalPages = book.totalPages,
                            pagesRead = book.pagesRead,
                            progress = progress,
                            progressLabel = if (book.totalPages > 0) {
                                "${book.pagesRead}/${book.totalPages} pages"
                            } else {
                                "${book.pagesRead} pages logged"
                            },
                            status = book.status,
                            coverUrl = book.coverUrl,
                            rating = book.rating
                        )
                    }
                val completedBooks = books
                    .filter { it.status == BookStatus.COMPLETED }
                    .map { book ->
                        BookCardUiModel(
                            id = book.id,
                            title = book.title,
                            author = book.author,
                            totalPages = book.totalPages,
                            pagesRead = book.pagesRead,
                            progress = 1f,
                            progressLabel = if (book.totalPages > 0) {
                                "${book.totalPages} pages finished"
                            } else {
                                "Completed"
                            },
                            status = book.status,
                            coverUrl = book.coverUrl,
                            rating = book.rating
                        )
                    }
                val onHoldBooks = books
                    .filter { it.status == BookStatus.ON_HOLD }
                    .map { book ->
                        val progress = if (book.totalPages > 0) {
                            book.pagesRead.toFloat() / book.totalPages.toFloat()
                        } else {
                            0f
                        }.coerceIn(0f, 1f)

                        BookCardUiModel(
                            id = book.id,
                            title = book.title,
                            author = book.author,
                            totalPages = book.totalPages,
                            pagesRead = book.pagesRead,
                            progress = progress,
                            progressLabel = "${book.pagesRead} pages (On hold)",
                            status = book.status,
                            coverUrl = book.coverUrl,
                            rating = 0
                        )
                    }

                DashboardUiState(
                    isLoading = false,
                    userName = name,
                    greeting = greeting,
                    subGreeting = subGreeting,
                    mascotMood = FlexibleHabitEngine.pickMood(cards),
                    cards = cards,
                    overallProgress = cards.map { it.progress }.average().takeUnless { it.isNaN() }?.toFloat() ?: 0f,
                    quote = quote.text,
                    quoteAuthor = quote.author,
                    readingBooks = readingBooks,
                    completedBooks = completedBooks,
                    onHoldBooks = onHoldBooks
                )
            }

            val bookSearchFlow = combine(
                bookSearchQuery,
                bookSuggestions,
                isBookSearchLoading,
                bookSearchError
            ) { query, suggestions, searching, searchError ->
                SearchState(
                    query = query,
                    suggestions = suggestions,
                    searching = searching,
                    searchError = searchError
                )
            }

            combine(
                dashboardFlow, 
                bookSearchFlow,
                isSyncing,
                lastSyncTime
            ) { dashboardState, searchState, syncing, syncTime ->
                dashboardState.copy(
                    bookSearchQuery = searchState.query,
                    bookSuggestions = searchState.suggestions,
                    isBookSearchLoading = searchState.searching,
                    bookSearchError = searchState.searchError,
                    isSyncing = syncing,
                    lastSyncTime = syncTime
                )
            }.collectLatest { state ->
                _uiState.value = state
            }
        }
    }

    fun triggerManualSync() {
        val userId = container.authRepository.authState.value?.uid ?: return
        viewModelScope.launch {
            isSyncing.value = true
            runCatching {
                container.profileRepository.syncFromRemoteIfNeeded(userId)
                container.habitRepository.syncLogsFromRemote(userId)
                container.bookRepository.syncBooksFromRemote(userId)
            }
            lastSyncTime.value = System.currentTimeMillis()
            isSyncing.value = false
        }
    }

    fun incrementHabit(habitId: String, date: LocalDate, delta: Int) {
        viewModelScope.launch {
            container.habitRepository.incrementHabit(habitId, date, delta)
            refreshWidget()
        }
    }

    fun toggleFreeze(habitId: String, date: LocalDate) {
        viewModelScope.launch {
            container.habitRepository.toggleFreeze(habitId, date)
            refreshWidget()
        }
    }

    fun setBacklogAmount(habitId: String, date: LocalDate, amount: Int) {
        viewModelScope.launch {
            container.habitRepository.setAmount(habitId, date, amount)
            refreshWidget()
        }
    }

    fun setBacklogFreeze(habitId: String, date: LocalDate, frozen: Boolean) {
        viewModelScope.launch {
            container.habitRepository.setFreeze(habitId, date, frozen)
            refreshWidget()
        }
    }

    fun addHabit(draft: HabitDraft) {
        viewModelScope.launch {
            container.habitRepository.addHabit(draft)
            refreshWidget()
        }
    }

    fun updateHabit(draft: HabitDraft) {
        viewModelScope.launch {
            container.habitRepository.updateHabit(draft)
            refreshWidget()
        }
    }

    fun deleteHabit(habitId: String) {
        viewModelScope.launch {
            container.habitRepository.deleteHabit(habitId)
            refreshWidget()
        }
    }

    fun updateBookSearchQuery(query: String) {
        bookSearchQuery.value = query
        if (query.isBlank()) {
            bookSuggestions.value = emptyList()
            bookSearchError.value = null
            isBookSearchLoading.value = false
        }
    }

    fun addBookSuggestion(suggestion: BookSuggestionUiModel) {
        viewModelScope.launch {
            container.bookRepository.addBookSuggestion(suggestion)
            bookSearchQuery.value = ""
            bookSuggestions.value = emptyList()
            bookSearchError.value = null
        }
    }

    fun manualAddBook(title: String, author: String, totalPages: Int) {
        viewModelScope.launch {
            container.bookRepository.manualAddBook(title, author, totalPages)
            bookSearchQuery.value = ""
            bookSuggestions.value = emptyList()
            bookSearchError.value = null
        }
    }

    fun incrementBookProgress(bookId: String, delta: Int) {
        viewModelScope.launch {
            container.bookRepository.incrementProgress(bookId, delta)
        }
    }

    fun updateBookPages(bookId: String, pages: Int) {
        viewModelScope.launch {
            container.bookRepository.setPagesRead(bookId, pages)
        }
    }

    fun moveBookToStatus(bookId: String, status: BookStatus, rating: Int = 0) {
        viewModelScope.launch {
            container.bookRepository.setStatus(bookId, status, rating)
        }
    }

    fun deleteBook(bookId: String) {
        viewModelScope.launch {
            container.bookRepository.deleteBook(bookId)
        }
    }

    private suspend fun refreshWidget() {
        HabWidget().updateAll(appContext)
    }

    companion object {
        fun Factory(
            appContext: Context,
            container: AppContainer
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                DashboardViewModel(appContext, container)
            }
        }
    }
}

private data class SearchState(
    val query: String,
    val suggestions: List<BookSuggestionUiModel>,
    val searching: Boolean,
    val searchError: String?
)
