package com.mouli.habittracker.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mouli.habittracker.core.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val isBusy: Boolean = false,
    val errorMessage: String? = null
)

class AuthViewModel(
    private val container: AppContainer
) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState = _uiState.asStateFlow()

    fun onGoogleTokenReceived(idToken: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, errorMessage = null) }
            container.authRepository.signInWithGoogle(idToken)
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            errorMessage = throwable.message ?: "Google sign-in was almost there, but it could not finish."
                        )
                    }
                }
                .onSuccess {
                    _uiState.update { it.copy(isBusy = false, errorMessage = null) }
                }
        }
    }

    fun onError(message: String) {
        _uiState.update { it.copy(errorMessage = message, isBusy = false) }
    }

    fun signOut() {
        container.authRepository.signOut()
    }

    companion object {
        fun Factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                AuthViewModel(container)
            }
        }
    }
}
