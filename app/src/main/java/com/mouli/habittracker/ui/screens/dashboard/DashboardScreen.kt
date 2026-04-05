@file:OptIn(
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class
)

package com.mouli.habittracker.ui.screens.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocalDrink
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.mouli.habittracker.model.BacklogDayUiModel
import com.mouli.habittracker.model.BookCardUiModel
import com.mouli.habittracker.model.BookStatus
import com.mouli.habittracker.model.BookSuggestionUiModel
import com.mouli.habittracker.model.DashboardUiState
import com.mouli.habittracker.model.HabitCadence
import com.mouli.habittracker.model.HabitCardUiModel
import com.mouli.habittracker.model.HabitDraft
import com.mouli.habittracker.model.HabitUnit
import com.mouli.habittracker.model.toLocalDateOrNull
import com.mouli.habittracker.ui.components.GlassPanel
import com.mouli.habittracker.ui.components.PandaMascot
import com.mouli.habittracker.ui.components.WeeklyProgressChart
import com.mouli.habittracker.ui.theme.AccentBlue
import com.mouli.habittracker.ui.theme.CrystalBlue
import com.mouli.habittracker.ui.theme.IceBlue
import com.mouli.habittracker.ui.theme.LightBlue
import com.mouli.habittracker.ui.theme.MistyBlue
import com.mouli.habittracker.ui.theme.habBackgroundBrush
import java.time.LocalDate
import java.util.UUID
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val dashboardDateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("EEEE, d MMM", Locale.getDefault())

private enum class DashboardSection(
    val label: String,
    val icon: ImageVector
) {
    HOME("Home", Icons.Rounded.Home),
    TRACKER("Tracker", Icons.Rounded.CalendarMonth),
    BOOKS("Books", Icons.Rounded.AutoStories)
}

private data class HabitTone(
    val accent: Color,
    val soft: Color,
    val icon: ImageVector
)

@Composable
fun DashboardScreen(
    state: DashboardUiState,
    onIncrement: (String, LocalDate, Int) -> Unit,
    onToggleFreeze: (String, LocalDate) -> Unit,
    onBacklogAmountChanged: (String, LocalDate, Int) -> Unit,
    onBacklogFreezeChanged: (String, LocalDate, Boolean) -> Unit,
    onBookQueryChanged: (String) -> Unit,
    onAddBookSuggestion: (BookSuggestionUiModel) -> Unit,
    onManualAddBook: (String, String, Int) -> Unit,
    onIncrementBookProgress: (String, Int) -> Unit,
    onUpdateBookPages: (String, Int) -> Unit,
    onMoveBookToStatus: (String, BookStatus, Int) -> Unit,
    onDeleteBook: (String) -> Unit,
    onAddHabit: (HabitDraft) -> Unit,
    onUpdateHabit: (HabitDraft) -> Unit,
    onDeleteHabit: (String) -> Unit,
    onRefreshSync: () -> Unit,
    onSignOut: () -> Unit
) {
    val today = LocalDate.now()
    val weekDays = remember(state.cards) {
        state.cards.firstOrNull()?.backlogDays?.map(BacklogDayUiModel::date) ?: currentWeek(today)
    }
    var backlogCard by remember { mutableStateOf<HabitCardUiModel?>(null) }
    var isAddingHabit by remember { mutableStateOf(false) }
    var isAddingBookManually by remember { mutableStateOf(false) }
    var editingHabit by remember { mutableStateOf<HabitCardUiModel?>(null) }
    var selectedSectionKey by rememberSaveable { mutableStateOf(DashboardSection.HOME.name) }
    var selectedDateKey by rememberSaveable { mutableStateOf(today.toString()) }

    val selectedSection = runCatching { DashboardSection.valueOf(selectedSectionKey) }
        .getOrDefault(DashboardSection.HOME)
    val selectedDate = selectedDateKey.toLocalDateOrNull()
        ?.takeIf(weekDays::contains)
        ?: weekDays.firstOrNull()
        ?: today

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(habBackgroundBrush())
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            bottomBar = {
                DashboardBottomBar(
                    selectedSection = selectedSection,
                    onSelected = { selectedSectionKey = it.name }
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { isAddingHabit = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = CircleShape
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add habit")
                }
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
                contentPadding = PaddingValues(
                    start = 20.dp,
                    end = 20.dp,
                    top = 18.dp,
                    bottom = innerPadding.calculateBottomPadding() + 80.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    DashboardHeader(
                        state = state,
                        section = selectedSection,
                        onRefreshSync = onRefreshSync,
                        onSignOut = onSignOut
                    )
                }

                when (selectedSection) {
                    DashboardSection.HOME -> {
                        item { QuotePanel(quote = state.quote, author = state.quoteAuthor) }
                        item {
                            HomeOverviewCard(progress = state.overallProgress)
                        }
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                PandaMascot(mood = state.mascotMood, mascotSize = 200.dp)
                                Text(
                                    text = "Welcome back, ${state.userName.ifBlank { "Mouli" }}.",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
                                )
                                Text(
                                    text = "This is your quiet space to track what matters. Use the tabs below to check your habits or update your reading shelf.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.padding(top = 8.dp).padding(horizontal = 16.dp)
                                )
                            }
                        }
                    }
                    DashboardSection.TRACKER -> {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SectionLabel("Daily habits")
                                TextButton(onClick = { isAddingHabit = true }) {
                                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add habit")
                                }
                            }
                        }
                        item { WeeklyProgressChart(cards = state.cards) }
                        item { TrackerSummaryCard(state = state, selectedDate = selectedDate) }
                        item {
                            DaySelectorRow(
                                weekDays = weekDays,
                                selectedDate = selectedDate,
                                onSelected = { selectedDateKey = it.toString() }
                            )
                        }
                        if (state.cards.isEmpty()) {
                            item {
                                EmptyPanel(
                                    title = "No habits yet",
                                    body = "Once your habits are added, this tracker will let you care for them one day at a time."
                                )
                            }
                        } else {
                            items(state.cards, key = { it.id }) { card ->
                                TrackerHabitCard(
                                    card = card,
                                    selectedDate = selectedDate,
                                    onAdjust = onIncrement,
                                    onSetAmount = onBacklogAmountChanged,
                                    onToggleFreeze = onToggleFreeze,
                                    onEdit = { editingHabit = it },
                                    onDelete = onDeleteHabit,
                                    onOpenBacklog = { backlogCard = card }
                                )
                            }
                        }
                    }
                    DashboardSection.BOOKS -> {
                        item { BooksOverviewCard(state = state) }
                        item {
                            BooksSearchPanel(
                                query = state.bookSearchQuery,
                                suggestions = state.bookSuggestions,
                                isLoading = state.isBookSearchLoading,
                                errorMessage = state.bookSearchError,
                                onQueryChanged = onBookQueryChanged,
                                onAddSuggestion = onAddBookSuggestion
                            )
                        }
                        item {
                            TextButton(
                                onClick = { isAddingBookManually = true },
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Add book manually")
                            }
                        }
                        if (state.readingBooks.isEmpty() && state.onHoldBooks.isEmpty() && state.completedBooks.isEmpty()) {
                            item {
                                EmptyPanel(
                                    title = "Reading shelf is empty",
                                    body = "Search a title above and add it to your shelf. Completed books will stay tucked away in their own calm lane."
                                )
                            }
                        } else {
                            if (state.readingBooks.isNotEmpty()) {
                                item { SectionLabel("Currently reading") }
                                items(state.readingBooks, key = { it.id }) { book ->
                                    ReadingBookCard(
                                        book = book,
                                        onIncrement = { delta -> onIncrementBookProgress(book.id, delta) },
                                        onUpdatePages = { pages -> onUpdateBookPages(book.id, pages) },
                                        onComplete = { rating -> onMoveBookToStatus(book.id, BookStatus.COMPLETED, rating) },
                                        onHold = { onMoveBookToStatus(book.id, BookStatus.ON_HOLD, 0) },
                                        onDelete = { onDeleteBook(book.id) }
                                    )
                                }
                            }
                            
                            if (state.onHoldBooks.isNotEmpty()) {
                                item { Spacer(modifier = Modifier.height(24.dp)) }
                                item { SectionLabel("On hold / Not interested") }
                                items(state.onHoldBooks, key = { it.id }) { book ->
                                    ReadingBookCard(
                                        book = book,
                                        onIncrement = { delta -> onIncrementBookProgress(book.id, delta) },
                                        onUpdatePages = { pages -> onUpdateBookPages(book.id, pages) },
                                        onComplete = { rating -> onMoveBookToStatus(book.id, BookStatus.COMPLETED, rating) },
                                        onHold = { onMoveBookToStatus(book.id, BookStatus.READING, 0) },
                                        onDelete = { onDeleteBook(book.id) }
                                    )
                                }
                            }

                            if (state.completedBooks.isNotEmpty()) {
                                item { Spacer(modifier = Modifier.height(24.dp)) }
                                item { SectionLabel("Completed") }
                                items(state.completedBooks, key = { it.id }) { book ->
                                    CompletedBookCard(
                                        book = book,
                                        onDelete = { onDeleteBook(book.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        backlogCard?.let { selectedCard ->
            BacklogEditorDialog(
                card = selectedCard,
                onDismiss = { backlogCard = null },
                onAmountChanged = { date, amount -> onBacklogAmountChanged(selectedCard.id, date, amount) },
                onFreezeChanged = { date, frozen -> onBacklogFreezeChanged(selectedCard.id, date, frozen) }
            )
        }

        if (isAddingHabit) {
            HabitEditorDialog(
                onDismiss = { isAddingHabit = false },
                onSave = { draft ->
                    onAddHabit(draft)
                    isAddingHabit = false
                }
            )
        }

        if (isAddingBookManually) {
            ManualBookDialog(
                onDismiss = { isAddingBookManually = false },
                onSave = { title, author, pages ->
                    onManualAddBook(title, author, pages)
                    isAddingBookManually = false
                }
            )
        }

        editingHabit?.let { habit ->
            HabitEditorDialog(
                existing = habit,
                onDismiss = { editingHabit = null },
                onSave = { draft ->
                    onUpdateHabit(draft)
                    editingHabit = null
                }
            )
        }
    }
}

@Composable
private fun HomeOverviewCard(
    progress: Float
) {
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "home-overall-progress")

    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Your total consistency",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
        )
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .padding(top = 12.dp),
            color = AccentBlue,
            trackColor = LightBlue
        )
        Text(
            text = "${(progress * 100).toInt()}% of today's intentions are glowing.",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 12.dp)
        )
    }
}

@Composable
private fun DashboardHeader(
    state: DashboardUiState,
    section: DashboardSection,
    onRefreshSync: () -> Unit,
    onSignOut: () -> Unit
) {
    val title = when (section) {
        DashboardSection.HOME -> state.userName.ifBlank { "Mouli" }
        DashboardSection.TRACKER -> "Habit tracker"
        DashboardSection.BOOKS -> "Books"
    }
    val subtitle = when (section) {
        DashboardSection.HOME -> state.greeting
        DashboardSection.TRACKER -> "Track your habits by day, keep streaks kind, and celebrate quiet wins."
        DashboardSection.BOOKS -> "A softer reading dashboard with suggestions, progress, and finished books."
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = if (state.isSyncing) "Restoring your rhythm..." else subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = if (state.isSyncing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f),
                modifier = Modifier.padding(top = 6.dp)
            )
            Text(
                text = LocalDate.now().format(dashboardDateFormatter),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 10.dp)
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onRefreshSync, enabled = !state.isSyncing) {
                Icon(
                    imageVector = Icons.Rounded.Sync,
                    contentDescription = "Sync data",
                    tint = if (state.isSyncing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            IconButton(onClick = onSignOut) {
                Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = "Sign out")
            }
        }
    }
}

@Composable
private fun HomeOverviewCard(
    state: DashboardUiState
) {
    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.subGreeting,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                text = "Your home stays light: one focus, a quick look at progress, and room to breathe.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 8.dp)
                )
                FlowRow(
                    modifier = Modifier.padding(top = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatusPill("${state.cards.size} habits")
                    StatusPill("${state.cards.count { it.isComplete }} in rhythm")
                    StatusPill("${state.readingBooks.size} books reading")
                }
            }
            PandaMascot(
                mood = state.mascotMood,
                mascotSize = 108.dp
            )
        }
    }
}

@Composable
private fun HomeFocusCard(
    state: DashboardUiState,
    today: LocalDate,
    onQuickAdd: (String, LocalDate, Int) -> Unit,
    onFreeze: (String, LocalDate) -> Unit,
    onDelete: (String) -> Unit,
    onOpenTracker: () -> Unit
) {
    val focusCard = state.cards.firstOrNull { !it.isComplete && !it.isFrozenToday } ?: state.cards.firstOrNull()
    if (focusCard == null) {
        EmptyPanel(
            title = "Your board is ready",
            body = "Once habits are added, the home page will keep just the gentlest spotlight here."
        )
        return
    }

    val tone = habitTone(focusCard)
    val isComplete = focusCard.isComplete
    val animatedProgress by animateFloatAsState(targetValue = if (isComplete) 1f else 0f, label = "home-focus-progress")

    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconBubble(icon = tone.icon, tint = tone.accent, background = tone.soft)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Today's soft focus",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f)
                    )
                    Text(
                        text = focusCard.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = focusCard.cadenceLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
            IconButton(onClick = { onDelete(focusCard.id) }) {
                Icon(
                    Icons.Rounded.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                )
            }
        }
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .padding(top = 16.dp),
            color = tone.accent,
            trackColor = tone.soft
        )
        Text(
            text = if (isComplete) "All done for today!" else "In focus right now",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 12.dp)
        )
        Text(
            text = focusCard.supportLine,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            modifier = Modifier.padding(top = 6.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { 
                    val delta = if (isComplete) -focusCard.targetValue else focusCard.targetValue
                    onQuickAdd(focusCard.id, today, delta) 
                },
                modifier = Modifier.weight(1.5f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isComplete) Color.Gray else MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    if (isComplete) Icons.Rounded.Close else Icons.Rounded.DoneAll, 
                    contentDescription = null, 
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isComplete) "Mark incomplete" else "Mark done")
            }
            OutlinedButton(
                onClick = { onFreeze(focusCard.id, today) },
                modifier = Modifier.weight(1f)
            ) {
                Text(if (focusCard.isFrozenToday) "Unfreeze" else "Freeze")
            }
        }
        TextButton(
            onClick = onOpenTracker,
            modifier = Modifier
                .align(Alignment.End)
                .padding(top = 8.dp)
        ) {
            Text("Open tracker")
        }
    }
}

@Composable
private fun TrackerSummaryCard(
    state: DashboardUiState,
    selectedDate: LocalDate
) {
    val completedCount = state.cards.count { card ->
        card.backlogDays.firstOrNull { it.date == selectedDate }?.isComplete == true
    }
    val frozenCount = state.cards.count { card ->
        card.backlogDays.firstOrNull { it.date == selectedDate }?.isFrozen == true
    }

    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = selectedDate.format(dashboardDateFormatter),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "$completedCount of ${state.cards.size} habits were cared for on this day. $frozenCount can stay protected without guilt.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun DaySelectorRow(
    weekDays: List<LocalDate>,
    selectedDate: LocalDate,
    onSelected: (LocalDate) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(weekDays, key = { it.toString() }) { day ->
            DayChip(
                date = day,
                isSelected = day == selectedDate,
                onClick = { onSelected(day) }
            )
        }
    }
}

@Composable
private fun DayChip(
    date: LocalDate,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val isFuture = date.isAfter(LocalDate.now())
    TextButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
        colors = ButtonDefaults.textButtonColors(
            containerColor = if (isSelected) AccentBlue.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.08f),
            contentColor = if (isFuture) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.42f) else MaterialTheme.colorScheme.onSurface
        ),
        modifier = Modifier
            .height(74.dp)
            .width(74.dp)
            .border(
                width = 1.dp,
                color = if (isSelected) AccentBlue.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.1f),
                shape = MaterialTheme.shapes.large
            )
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun TrackerHabitCard(
    card: HabitCardUiModel,
    selectedDate: LocalDate,
    onAdjust: (String, LocalDate, Int) -> Unit,
    onSetAmount: (String, LocalDate, Int) -> Unit,
    onToggleFreeze: (String, LocalDate) -> Unit,
    onEdit: (HabitCardUiModel) -> Unit,
    onDelete: (String) -> Unit,
    onOpenBacklog: () -> Unit
) {
    val selectedDay = card.backlogDays.firstOrNull { it.date == selectedDate }
    val tone = habitTone(card)
    val isFuture = selectedDate.isAfter(LocalDate.now())
    val isFrozen = selectedDay?.isFrozen == true
    val dayAmount = selectedDay?.amount ?: 0
    val isComplete = selectedDay?.isComplete == true
    val animatedProgress by animateFloatAsState(targetValue = if (isComplete) 1f else 0f, label = "tracker-day-progress")
    
    var localAmount by remember(dayAmount) { mutableStateOf(dayAmount.toString()) }

    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                IconBubble(icon = tone.icon, tint = tone.accent, background = tone.soft)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = card.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    FlowRow(
                        modifier = Modifier.padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatusPill(card.streakLabel)
                        StatusPill(card.cadenceLabel)
                        if (card.isBacklogEnabled) {
                            val balance = card.backlogBalance
                            val label = when {
                                balance > 0 -> "+$balance gain"
                                balance < 0 -> "${balance} debt"
                                else -> "Steady"
                            }
                            StatusPill(label)
                        }
                    }
                }
            }
            Row {
                IconButton(onClick = { onEdit(card) }) {
                    Icon(
                        imageVector = Icons.Rounded.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(onClick = { onDelete(card.id) }) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isComplete) "Goal met!" else "Progress",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { onAdjust(card.id, selectedDate, -1) },
                    enabled = !isFuture && !isFrozen,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Rounded.Remove, contentDescription = "-1", modifier = Modifier.size(18.dp))
                }
                
                OutlinedTextField(
                    value = localAmount,
                    onValueChange = { newValue ->
                        localAmount = newValue.filter { c -> c.isDigit() }
                        newValue.toIntOrNull()?.let { a -> onSetAmount(card.id, selectedDate, a) }
                    },
                    enabled = !isFuture && !isFrozen,
                    modifier = Modifier.width(70.dp),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                
                IconButton(
                    onClick = { onAdjust(card.id, selectedDate, 1) },
                    enabled = !isFuture && !isFrozen,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "+1", modifier = Modifier.size(18.dp))
                }
            }
        }

        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .padding(top = 12.dp),
            color = tone.accent,
            trackColor = tone.soft
        )
        
        Text(
            text = trackerAppreciation(card, selectedDay, selectedDate, isFuture),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
            modifier = Modifier.padding(top = 8.dp)
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = { onToggleFreeze(card.id, selectedDate) },
                enabled = !isFuture,
                modifier = Modifier.height(36.dp),
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Text(if (isFrozen) "Unfreeze" else "Freeze")
            }
            Spacer(modifier = Modifier.width(8.dp))
            TextButton(
                onClick = onOpenBacklog,
                modifier = Modifier.height(36.dp)
            ) {
                Text("History")
            }
        }
    }
}

@Composable
private fun BooksOverviewCard(
    state: DashboardUiState
) {
    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconBubble(
                icon = Icons.AutoMirrored.Rounded.MenuBook,
                tint = CrystalBlue,
                background = LightBlue
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "A gentler shelf",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${state.readingBooks.size} reading now - ${state.completedBooks.size} completed",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                )
            }
        }
    }
}

@Composable
private fun BooksSearchPanel(
    query: String,
    suggestions: List<BookSuggestionUiModel>,
    isLoading: Boolean,
    errorMessage: String?,
    onQueryChanged: (String) -> Unit,
    onAddSuggestion: (BookSuggestionUiModel) -> Unit
) {
    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Search for a book or author",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Suggestions come from the web and can be added straight to your shelf.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 8.dp)
        )
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChanged,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
            singleLine = true,
            leadingIcon = {
                Icon(Icons.Rounded.Search, contentDescription = null)
            },
            placeholder = { Text("Search books or authors") }
        )
        when {
            isLoading -> {
                Text(
                    text = "Looking up suggestions...",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
            suggestions.isNotEmpty() -> {
                Column(
                    modifier = Modifier.padding(top = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    suggestions.forEach { suggestion ->
                        BookSuggestionRow(
                            suggestion = suggestion,
                            onAdd = { onAddSuggestion(suggestion) }
                        )
                    }
                }
            }
            !errorMessage.isNullOrBlank() -> {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun BookSuggestionRow(
    suggestion: BookSuggestionUiModel,
    onAdd: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBubble(
            icon = Icons.Rounded.AutoStories,
            tint = CrystalBlue,
            background = LightBlue
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = suggestion.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = buildString {
                    append(suggestion.author)
                    if (suggestion.publishedYear.isNotBlank()) append(" - ${suggestion.publishedYear}")
                    if (suggestion.totalPages > 0) append(" - ${suggestion.totalPages} pages")
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f)
            )
        }
        Button(onClick = onAdd) {
            Text("Add")
        }
    }
}

@Composable
private fun ReadingBookCard(
    book: BookCardUiModel,
    onIncrement: (Int) -> Unit,
    onUpdatePages: (Int) -> Unit,
    onComplete: (Int) -> Unit,
    onHold: () -> Unit,
    onDelete: () -> Unit
) {
    var showRatingDialog by remember { mutableStateOf(false) }
    val animatedProgress by animateFloatAsState(targetValue = book.progress, label = "reading-book-progress")
    var localPages by remember(book.pagesRead) { mutableStateOf(book.pagesRead.toString()) }

    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconBubble(
                    icon = Icons.AutoMirrored.Rounded.MenuBook,
                    tint = MistyBlue,
                    background = LightBlue
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = book.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = book.author,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Rounded.Delete,
                    contentDescription = "Delete book",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatusPill(if (book.status == BookStatus.ON_HOLD) "On hold" else "In progress")
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { onIncrement(-1) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Rounded.Remove, contentDescription = "-1", modifier = Modifier.size(18.dp))
                }
                
                OutlinedTextField(
                    value = localPages,
                    onValueChange = { newValue ->
                        localPages = newValue.filter { c -> c.isDigit() }
                        newValue.toIntOrNull()?.let { p -> onUpdatePages(p) }
                    },
                    modifier = Modifier.width(80.dp),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
                
                IconButton(
                    onClick = { onIncrement(1) },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "+1", modifier = Modifier.size(18.dp))
                }
            }
        }

        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .padding(top = 16.dp),
            color = CrystalBlue,
            trackColor = LightBlue
        )
        Text(
            text = book.progressLabel,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 10.dp)
        )
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { showRatingDialog = true },
                modifier = Modifier.weight(1f)
            ) {
                Text("Complete")
            }
            OutlinedButton(
                onClick = onHold,
                modifier = Modifier.weight(1.5f)
            ) {
                Text(if (book.status == BookStatus.ON_HOLD) "Give another chance" else "Not for now")
            }
        }
    }

    if (showRatingDialog) {
        BookRatingDialog(
            title = "Nice work!",
            body = "How would you rate your time with ${book.title}?",
            onDismiss = { showRatingDialog = false },
            onConfirm = { rating ->
                onComplete(rating)
                showRatingDialog = false
            }
        )
    }
}

@Composable
private fun BookRatingDialog(
    title: String,
    body: String,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var rating by remember { mutableStateOf(5) }

    Dialog(onDismissRequest = onDismiss) {
        GlassPanel(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 4.dp)
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                (1..5).forEach { index ->
                    IconButton(onClick = { rating = index }) {
                        Icon(
                            imageVector = if (index <= rating) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                            contentDescription = null,
                            tint = if (index <= rating) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = { onConfirm(rating) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Complete")
                }
            }
        }
    }
}

@Composable
private fun CompletedBookCard(
    book: BookCardUiModel,
    onDelete: () -> Unit
) {
    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconBubble(
                    icon = Icons.Rounded.Celebration,
                    tint = AccentBlue,
                    background = LightBlue
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = book.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = book.author,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
                    )
                }
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Rounded.Delete,
                    contentDescription = "Delete book",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
                )
            }
        }
        StatusPill("Finished")
        
        Row(
            modifier = Modifier.padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            (1..5).forEach { index ->
                Icon(
                    imageVector = if (index <= book.rating) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                    contentDescription = null,
                    tint = if (index <= book.rating) Color(0xFFFFD700) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Text(
            text = book.progressLabel,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 12.dp)
        )
        Text(
            text = "Finished books stay here so the shelf feels rewarding, not noisy.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun DashboardBottomBar(
    selectedSection: DashboardSection,
    onSelected: (DashboardSection) -> Unit
) {
    androidx.compose.material3.Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        shadowElevation = 16.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DashboardSection.entries.forEach { section ->
                val isSelected = section == selectedSection
                TextButton(
                    onClick = { onSelected(section) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = if (isSelected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        } else {
                            Color.Transparent
                        },
                        contentColor = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        }
                    ),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(section.icon, contentDescription = null)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = section.label,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackerActionButton(
    label: String,
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label)
    }
}

@Composable
private fun StatusPill(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .background(
                color = AccentBlue.copy(alpha = 0.12f),
                shape = MaterialTheme.shapes.large
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
    )
}

@Composable
private fun IconBubble(
    icon: ImageVector,
    tint: Color,
    background: Color
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .background(background, shape = MaterialTheme.shapes.medium),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint
        )
    }
}

@Composable
private fun QuotePanel(
    quote: String,
    author: String
) {
    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "A little line for the day",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "\"$quote\"",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(top = 10.dp)
        )
        Text(
            text = author,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f),
            modifier = Modifier.padding(top = 6.dp)
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun EmptyPanel(
    title: String,
    body: String
) {
    GlassPanel(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

@Composable
private fun BacklogEditorDialog(
    card: HabitCardUiModel,
    onDismiss: () -> Unit,
    onAmountChanged: (LocalDate, Int) -> Unit,
    onFreezeChanged: (LocalDate, Boolean) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        GlassPanel(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Edit this week",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Adjust previous days, protect a day with a freeze, and keep the week feeling flexible.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                card.backlogDays.forEach { day ->
                    BacklogDayRow(
                        day = day,
                        onAmountChanged = { onAmountChanged(day.date, it) },
                        onFreezeChanged = { onFreezeChanged(day.date, it) }
                    )
                }
                OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
private fun BacklogDayRow(
    day: BacklogDayUiModel,
    onAmountChanged: (Int) -> Unit,
    onFreezeChanged: (Boolean) -> Unit
) {
    var textValue by remember(day.amount) { mutableStateOf(day.amount.toString()) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (day.isToday) "${day.label} - today" else day.label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = when {
                    day.isFrozen -> "Safely frozen"
                    day.isComplete -> "Already counted"
                    else -> "Open for a catch-up"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
            )
        }
        OutlinedTextField(
            value = textValue,
            onValueChange = { value ->
                textValue = value.filter(Char::isDigit)
                onAmountChanged(textValue.toIntOrNull() ?: 0)
            },
            modifier = Modifier.width(110.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Freeze", style = MaterialTheme.typography.bodyMedium)
            Switch(checked = day.isFrozen, onCheckedChange = onFreezeChanged)
        }
    }
}

private fun trackerProgressLabel(
    card: HabitCardUiModel,
    selectedDay: BacklogDayUiModel?
): String {
    val amount = selectedDay?.amount ?: 0
    return when (card.cadence) {
        HabitCadence.DAILY -> "$amount/${card.targetValue} ${card.unit.displayName}"
        HabitCadence.WEEKLY -> "$amount ${card.unit.displayName} on ${selectedDay?.label ?: "this day"} - ${card.progressLabel}"
    }
}

private fun trackerAppreciation(
    card: HabitCardUiModel,
    selectedDay: BacklogDayUiModel?,
    selectedDate: LocalDate,
    isFuture: Boolean
): String = when {
    isFuture -> "This day is still ahead of you. Leave it soft for now."
    selectedDay?.isFrozen == true -> "This day is protected by a freeze, so the streak stays kind."
    card.cadence == HabitCadence.DAILY && selectedDay?.isComplete == true ->
        "Lovely work. ${card.title} is complete for ${selectedDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()).lowercase()}."
    card.cadence == HabitCadence.WEEKLY && card.isComplete ->
        "Weekly goal complete. Panda tucked a tiny celebration into the corner."
    selectedDay?.isComplete == true ->
        "Nice. This day moved the habit forward without needing a big push."
    else -> card.supportLine
}

private fun habitTone(card: HabitCardUiModel): HabitTone {
    val lowerTitle = card.title.lowercase(Locale.getDefault())
    return when {
        "walk" in lowerTitle || card.unit == HabitUnit.STEPS -> HabitTone(
            accent = AccentBlue,
            soft = LightBlue,
            icon = Icons.AutoMirrored.Rounded.DirectionsWalk
        )
        "read" in lowerTitle || card.unit == HabitUnit.PAGES -> HabitTone(
            accent = CrystalBlue,
            soft = IceBlue,
            icon = Icons.Rounded.AutoStories
        )
        "water" in lowerTitle || card.unit == HabitUnit.GLASSES -> HabitTone(
            accent = MistyBlue,
            soft = LightBlue,
            icon = Icons.Rounded.LocalDrink
        )
        card.unit == HabitUnit.MINUTES -> HabitTone(
            accent = Color(0xFF87AEE8),
            soft = Color(0xFFEAF2FF),
            icon = Icons.Rounded.Timer
        )
        else -> HabitTone(
            accent = Color(0xFF90B6FF),
            soft = Color(0xFFEDF4FF),
            icon = Icons.Rounded.DoneAll
        )
    }
}

@Composable
private fun HabitEditorDialog(
    existing: HabitCardUiModel? = null,
    onDismiss: () -> Unit,
    onSave: (HabitDraft) -> Unit
) {
    var title by remember { mutableStateOf(existing?.title ?: "") }
    var target by remember { mutableStateOf(existing?.targetValue?.toString() ?: "1") }
    var cadence by remember { mutableStateOf(existing?.cadence ?: HabitCadence.DAILY) }
    var unit by remember { mutableStateOf(existing?.unit ?: HabitUnit.CHECK_INS) }
    var customUnit by remember { mutableStateOf(existing?.customUnit ?: "") }
    var trackBacklog by remember { mutableStateOf(existing?.isBacklogEnabled ?: false) }

    Dialog(onDismissRequest = onDismiss) {
        GlassPanel(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = if (existing == null) "New habit" else "Edit habit",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(top = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("What should we call it?") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = target,
                        onValueChange = { target = it.filter { c -> c.isDigit() } },
                        label = { Text("Goal") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                    
                    Column(modifier = Modifier.weight(1.5f)) {
                        Text("Frequency", style = MaterialTheme.typography.labelSmall)
                        Row(modifier = Modifier.padding(top = 4.dp)) {
                            HabitCadence.entries.forEach { entry ->
                                FilterChip(
                                    selected = cadence == entry,
                                    onClick = { cadence = entry },
                                    label = { Text(entry.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                            }
                        }
                    }
                }

                Column {
                    Text("Unit of measure", style = MaterialTheme.typography.labelSmall)
                    FlowRow(
                        modifier = Modifier.padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HabitUnit.entries.forEach { entry ->
                            FilterChip(
                                selected = unit == entry,
                                onClick = { unit = entry },
                                label = { Text(entry.displayName) }
                            )
                        }
                    }
                    if (unit == HabitUnit.OTHER) {
                        OutlinedTextField(
                            value = customUnit,
                            onValueChange = { customUnit = it },
                            label = { Text("Custom unit (e.g. loops, drawings)") },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            singleLine = true
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Track backlog (Debt/Gain)", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                        Text(
                            "Missed goals become 'debt' to catch up on later.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Switch(checked = trackBacklog, onCheckedChange = { trackBacklog = it })
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = { 
                        if (title.isNotBlank()) {
                            onSave(HabitDraft(
                                id = existing?.id ?: UUID.randomUUID().toString(),
                                title = title, 
                                target = target, 
                                cadence = cadence, 
                                unit = unit,
                                customUnit = customUnit.takeIf { unit == HabitUnit.OTHER },
                                trackBacklog = trackBacklog
                            ))
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = title.isNotBlank() && target.isNotBlank() && (unit != HabitUnit.OTHER || customUnit.isNotBlank())
                ) {
                    Text(if (existing == null) "Create" else "Save")
                }
            }
        }
    }
}

@Composable
private fun ManualBookDialog(
    onDismiss: () -> Unit,
    onSave: (String, String, Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var totalPages by remember { mutableStateOf("200") }

    Dialog(onDismissRequest = onDismiss) {
        GlassPanel(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Add book manually",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(top = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Book title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = author,
                    onValueChange = { author = it },
                    label = { Text("Author") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = totalPages,
                    onValueChange = { totalPages = it.filter { c -> c.isDigit() } },
                    label = { Text("Total pages") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = { 
                        if (title.isNotBlank() && author.isNotBlank()) {
                            onSave(title, author, totalPages.toIntOrNull() ?: 0)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = title.isNotBlank() && author.isNotBlank()
                ) {
                    Text("Add book")
                }
            }
        }
    }
}

private fun currentWeek(today: LocalDate): List<LocalDate> {
    val offset = today.dayOfWeek.value - 1
    val monday = today.minusDays(offset.toLong())
    return (0..6).map { monday.plusDays(it.toLong()) }
}
