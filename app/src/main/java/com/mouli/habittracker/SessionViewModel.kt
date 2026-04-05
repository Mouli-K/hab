package com.mouli.habittracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.google.firebase.auth.FirebaseUser
import com.mouli.habittracker.core.AppContainer
import com.mouli.habittracker.data.local.entity.UserProfileEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SessionUiState(
    val isLoading: Boolean = true,
    val currentUser: FirebaseUser? = null,
    val profile: UserProfileEntity? = null
)

class SessionViewModel(
    private val container: AppContainer
) : ViewModel() {
    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState = _uiState.asStateFlow()
    private var authReady = false
    private var profileReady = false

    init {
        observeSession()
    }

    private fun observeSession() {
        viewModelScope.launch {
            container.authRepository.authState.collectLatest { user ->
                authReady = true
                _uiState.update {
                    it.copy(
                        currentUser = user,
                        isLoading = !(authReady && profileReady)
                    )
                }
                if (user != null) {
                    runCatching { 
                        container.profileRepository.syncFromRemoteIfNeeded(user.uid)
                        container.habitRepository.syncLogsFromRemote(user.uid)
                        container.bookRepository.syncBooksFromRemote(user.uid)
                        container.reminderScheduler.schedule()
                    }
                }
            }
        }

        viewModelScope.launch {
            container.profileRepository.observeProfile().collectLatest { profile ->
                profileReady = true
                _uiState.update { state ->
                    state.copy(
                        profile = profile,
                        isLoading = !(authReady && profileReady)
                    )
                }
            }
        }
    }

    companion object {
        fun Factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                SessionViewModel(container)
            }
        }
    }
}
