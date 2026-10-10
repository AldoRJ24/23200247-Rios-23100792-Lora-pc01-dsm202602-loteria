package com.example.loteriaapp.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loteriaapp.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val emailInput: String = "",
    val passwordInput: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isLoggedIn: Boolean = false,
    val userEmail: String? = null
)

class AuthViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        checkSession()
    }

    fun checkSession() {
        val user = authRepository.currentUser
        if (user != null) {
            _uiState.update {
                it.copy(
                    isLoggedIn = true,
                    userEmail = user.email
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    isLoggedIn = false,
                    userEmail = null
                )
            }
        }
    }

    fun onEmailChanged(email: String) {
        _uiState.update { it.copy(emailInput = email, errorMessage = null) }
    }

    fun onPasswordChanged(password: String) {
        _uiState.update { it.copy(passwordInput = password, errorMessage = null) }
    }

    fun login(onSuccess: () -> Unit) {
        val email = _uiState.value.emailInput.trim()
        val password = _uiState.value.passwordInput.trim()

        if (email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Por favor, complete todos los campos.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = authRepository.login(email, password)
            result.fold(
                onSuccess = { user ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isLoggedIn = true,
                            userEmail = user.email,
                            errorMessage = null
                        )
                    }
                    onSuccess()
                },
                onFailure = { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = exception.localizedMessage ?: "Error al iniciar sesión."
                        )
                    }
                }
            )
        }
    }

    fun register(onSuccess: () -> Unit) {
        val email = _uiState.value.emailInput.trim()
        val password = _uiState.value.passwordInput.trim()

        if (email.isBlank() || password.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Por favor, complete todos los campos.") }
            return
        }

        if (password.length < 6) {
            _uiState.update { it.copy(errorMessage = "La contraseña debe tener al menos 6 caracteres.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = authRepository.register(email, password)
            result.fold(
                onSuccess = { user ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isLoggedIn = true,
                            userEmail = user.email,
                            errorMessage = null
                        )
                    }
                    onSuccess()
                },
                onFailure = { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = exception.localizedMessage ?: "Error al registrarse."
                        )
                    }
                }
            )
        }
    }

    fun logout() {
        authRepository.logout()
        _uiState.update {
            AuthUiState(
                isLoggedIn = false,
                userEmail = null
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
