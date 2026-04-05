@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class
)

package com.mouli.habittracker.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mouli.habittracker.model.HabitCadence
import com.mouli.habittracker.model.HabitDraft
import com.mouli.habittracker.model.HabitUnit
import com.mouli.habittracker.model.MascotMood
import com.mouli.habittracker.model.ReminderTime
import com.mouli.habittracker.ui.components.GlassPanel
import com.mouli.habittracker.ui.components.PandaMascot
import com.mouli.habittracker.ui.theme.habBackgroundBrush

@Composable
fun PandaInterviewScreen(
    state: OnboardingUiState,
    onNameChanged: (String) -> Unit,
    onAddHabit: () -> Unit,
    onRemoveHabit: (String) -> Unit,
    onHabitChanged: (String, HabitDraft) -> Unit,
    onAddReminder: (ReminderTime) -> Unit,
    onRemoveReminder: (ReminderTime) -> Unit,
    onFinish: () -> Unit
) {
    var showTimePicker by remember { mutableStateOf(false) }
    val timePickerState = rememberTimePickerState(initialHour = 8, initialMinute = 0, is24Hour = false)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(habBackgroundBrush())
            .padding(horizontal = 20.dp)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    PandaMascot(mood = MascotMood.CALM)
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Panda Interview",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tell Hab your name, the habits you care about, and when a literary nudge should arrive.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.74f)
                    )
                }
            }

            item {
                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                    Text("What should Panda call you?", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = state.displayName,
                        onValueChange = onNameChanged,
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("Mouli") }
                    )
                }
            }

            item {
                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Starter habits", style = MaterialTheme.typography.titleLarge)
                        IconButton(onClick = onAddHabit) {
                            Icon(Icons.Rounded.Add, contentDescription = "Add habit")
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        state.habits.forEach { draft ->
                            HabitDraftEditor(
                                draft = draft,
                                onChanged = { onHabitChanged(draft.id, it) },
                                onRemove = { onRemoveHabit(draft.id) }
                            )
                        }
                    }
                }
            }

            item {
                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Reminder times", style = MaterialTheme.typography.titleLarge)
                        TextButton(onClick = { showTimePicker = true }) {
                            Text("Add time")
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        state.reminderTimes.forEach { reminderTime ->
                            FilterChip(
                                selected = false,
                                onClick = { onRemoveReminder(reminderTime) },
                                label = { Text("${reminderTime.formatted()}  remove") }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Hab will use these for WorkManager reminders and the widget mood.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
                    )
                }
            }

            item {
                Button(
                    onClick = onFinish,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !state.isSaving
                ) {
                    Text(if (state.isSaving) "Saving..." else "Finish Interview")
                }
                if (!state.errorMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = state.errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f)
                    )
                }
            }
        }

        if (showTimePicker) {
            AlertDialog(
                onDismissRequest = { showTimePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            onAddReminder(ReminderTime(timePickerState.hour, timePickerState.minute))
                            showTimePicker = false
                        }
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showTimePicker = false }) {
                        Text("Cancel")
                    }
                },
                text = {
                    Column {
                        Text(
                            text = "Pick a gentle nudge time",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        TimePicker(state = timePickerState)
                    }
                }
            )
        }
    }
}

@Composable
private fun HabitDraftEditor(
    draft: HabitDraft,
    onChanged: (HabitDraft) -> Unit,
    onRemove: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Habit", style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = onRemove) {
                Icon(Icons.Rounded.Close, contentDescription = "Remove habit")
            }
        }
        OutlinedTextField(
            value = draft.title,
            onValueChange = { onChanged(draft.copy(title = it)) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Title") },
            singleLine = true
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = draft.target,
                onValueChange = { value ->
                    onChanged(draft.copy(target = value.filter { char -> char.isDigit() }))
                },
                modifier = Modifier.weight(1f),
                label = { Text("Target") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text("Cadence", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HabitCadence.entries.forEach { cadence ->
                        FilterChip(
                            selected = draft.cadence == cadence,
                            onClick = { onChanged(draft.copy(cadence = cadence)) },
                            label = { Text(cadence.name.lowercase().replaceFirstChar(Char::uppercase)) }
                        )
                    }
                }
            }
        }
        Text("Unit", style = MaterialTheme.typography.bodyMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            HabitUnit.entries.forEach { unit ->
                FilterChip(
                    selected = draft.unit == unit,
                    onClick = { onChanged(draft.copy(unit = unit)) },
                    label = { Text(unit.displayName) }
                )
            }
        }
    }
}
