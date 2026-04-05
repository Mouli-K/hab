package com.mouli.habittracker.ui.screens.onboarding

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mouli.habittracker.core.AppContainer
import com.mouli.habittracker.data.local.entity.ProfileSeed
import com.mouli.habittracker.data.repository.ProfileRepository
import com.mouli.habittracker.model.HabitCadence
import com.mouli.habittracker.model.HabitDraft
import com.mouli.habittracker.model.HabitUnit
import com.mouli.habittracker.model.ReminderTime
import com.mouli.habittracker.widget.HabWidget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val displayName: String = "Mouli",
    val habits: List<HabitDraft> = ProfileRepository.defaultHabitDrafts(),
    val reminderTimes: List<ReminderTime> = ProfileRepository.defaultReminderTimes(),
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)

class OnboardingViewModel(
    private val appContext: Context,
    private val container: AppContainer
) : ViewModel() {
    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState = _uiState.asStateFlow()

    fun updateName(value: String) {
        _uiState.update { it.copy(displayName = value) }
    }

    fun addHabitDraft() {
        _uiState.update {
            it.copy(
                habits = it.habits + HabitDraft(
                    title = "",
                    target = "1",
                    cadence = HabitCadence.DAILY,
                    unit = HabitUnit.CHECK_INS
                )
            )
        }
    }

    fun removeHabitDraft(id: String) {
        _uiState.update { state ->
            val remaining = state.habits.filterNot { it.id == id }
            state.copy(habits = remaining.ifEmpty { listOf(HabitDraft()) })
        }
    }

    fun updateHabitDraft(id: String, updated: HabitDraft) {
        _uiState.update { state ->
            state.copy(habits = state.habits.map { if (it.id == id) updated else it })
        }
    }

    fun addReminderTime(reminderTime: ReminderTime) {
        _uiState.update { state ->
            state.copy(
                reminderTimes = (state.reminderTimes + reminderTime)
                    .distinctBy { it.formatted() }
                    .sortedWith(compareBy(ReminderTime::hour, ReminderTime::minute))
            )
        }
    }

    fun removeReminderTime(reminderTime: ReminderTime) {
        _uiState.update { state ->
            val remaining = state.reminderTimes.filterNot { it == reminderTime }
            state.copy(reminderTimes = remaining.ifEmpty { ProfileRepository.defaultReminderTimes() })
        }
    }

    fun completeInterview(
        userId: String?,
        fallbackDisplayName: String? = null
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            runCatching {
                val snapshot = _uiState.value
                val seed = ProfileSeed(
                    name = snapshot.displayName,
                    reminders = snapshot.reminderTimes,
                    habits = snapshot.habits
                )
                container.profileRepository.completeOnboarding(
                    userId = userId,
                    seed = seed,
                    fallbackDisplayName = fallbackDisplayName
                )
                container.reminderScheduler.schedule()
                HabWidget().updateAll(appContext)
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = throwable.message ?: "The panda interview could not be saved yet."
                    )
                }
            }.onSuccess {
                _uiState.update { it.copy(isSaving = false, errorMessage = null) }
            }
        }
    }

    companion object {
        fun Factory(
            appContext: Context,
            container: AppContainer
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                OnboardingViewModel(appContext, container)
            }
        }
    }
}
