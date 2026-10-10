package com.example.loteriaaaa.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loteriaaaa.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val emailInput: String = "",
    val passwordInput: String = "",
    val isLoading: Boolean = false,
    val userEmail: String? = null,
    val userId: String? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class AuthViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        checkCurrentUser()
    }

    fun checkCurrentUser() {
        val user = authRepository.getCurrentUser()
        _uiState.update {
            it.copy(
                userEmail = user?.email,
                userId = user?.uid
            )
        }
    }

    fun onEmailChanged(email: String) {
        _uiState.update { it.copy(emailInput = email, errorMessage = null) }
    }

    fun onPasswordChanged(password: String) {
        _uiState.update { it.copy(passwordInput = password, errorMessage = null) }
    }

    fun login() {
        val email = _uiState.value.emailInput
        val pass = _uiState.value.passwordInput

        if (email.isBlank() || pass.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Ingresa tu correo y contraseña.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            val result = authRepository.loginWithEmail(email, pass)
            result.onSuccess { user ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        userEmail = user.email,
                        userId = user.uid,
                        successMessage = "¡Sesión iniciada correctamente!",
                        errorMessage = null
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.localizedMessage ?: "Error al iniciar sesión."
                    )
                }
            }
        }
    }

    fun register() {
        val email = _uiState.value.emailInput
        val pass = _uiState.value.passwordInput

        if (email.isBlank() || pass.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Ingresa un correo y contraseña para registrarte.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            val result = authRepository.registerWithEmail(email, pass)
            result.onSuccess { user ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        userEmail = user.email,
                        userId = user.uid,
                        successMessage = "¡Registro exitoso! Sesión iniciada.",
                        errorMessage = null
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.localizedMessage ?: "Error al registrar usuario."
                    )
                }
            }
        }
    }

    fun logout() {
        authRepository.logout()
        _uiState.update {
            it.copy(
                userEmail = null,
                userId = null,
                emailInput = "",
                passwordInput = "",
                successMessage = "Sesión cerrada.",
                errorMessage = null
            )
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
