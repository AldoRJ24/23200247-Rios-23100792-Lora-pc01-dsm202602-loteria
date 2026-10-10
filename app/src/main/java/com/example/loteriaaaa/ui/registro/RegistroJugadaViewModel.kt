package com.example.loteriaaaa.ui.registro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loteriaaaa.data.model.Jugada
import com.example.loteriaaaa.data.model.Sorteo
import com.example.loteriaaaa.data.repository.LoteriaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RegistroJugadaUiState(
    val sorteo: Sorteo? = null,
    val selectedNumbers: Set<Int> = emptySet(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val jugadaGuardada: Jugada? = null
)

class RegistroJugadaViewModel(
    private val loteriaRepository: LoteriaRepository = LoteriaRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegistroJugadaUiState())
    val uiState: StateFlow<RegistroJugadaUiState> = _uiState.asStateFlow()

    init {
        cargarSorteoInicial()
    }

    fun cargarSorteoInicial() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = loteriaRepository.obtenerOEnviarSorteoInicial()
            result.onSuccess { sorteo ->
                _uiState.update {
                    it.copy(
                        sorteo = sorteo,
                        isLoading = false
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Error al cargar sorteo: ${error.localizedMessage}"
                    )
                }
            }
        }
    }

    fun toggleNumber(numero: Int) {
        if (numero !in 1..45) return

        val current = _uiState.value.selectedNumbers.toMutableSet()
        if (current.contains(numero)) {
            current.remove(numero)
            _uiState.update {
                it.copy(
                    selectedNumbers = current,
                    errorMessage = null,
                    successMessage = null
                )
            }
        } else {
            if (current.size >= 6) {
                _uiState.update {
                    it.copy(errorMessage = "Ya has seleccionado exactamente 6 números. Deselecciona uno para cambiarlo.")
                }
            } else {
                current.add(numero)
                _uiState.update {
                    it.copy(
                        selectedNumbers = current,
                        errorMessage = null,
                        successMessage = null
                    )
                }
            }
        }
    }

    fun guardarJugada(userId: String) {
        val state = _uiState.value
        val sorteo = state.sorteo

        if (userId.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Debes estar autenticado para guardar tu jugada.") }
            return
        }

        if (sorteo == null) {
            _uiState.update { it.copy(errorMessage = "No hay información del sorteo disponible.") }
            return
        }

        if (sorteo.estado.lowercase() != "abierto") {
            _uiState.update {
                it.copy(errorMessage = "Solo se permite guardar la jugada si el estado del sorteo es 'abierto'. Estado actual: '${sorteo.estado}'.")
            }
            return
        }

        val numerosList = state.selectedNumbers.toList().sorted()
        if (numerosList.size != 6) {
            _uiState.update {
                it.copy(errorMessage = "Debes seleccionar exactamente 6 números antes de guardar. Llevas ${numerosList.size}/6.")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null, successMessage = null) }

            val jugada = Jugada(
                sorteoId = sorteo.id,
                userId = userId,
                numeros = numerosList
            )

            val result = loteriaRepository.guardarJugada(jugada)
            result.onSuccess { jugadaId ->
                val jugadaGuardadaConId = jugada.copy(id = jugadaId)
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        jugadaGuardada = jugadaGuardadaConId,
                        successMessage = "¡Jugada guardada con éxito! Tus números: ${numerosList.joinToString(", ")}"
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = error.localizedMessage ?: "Error al guardar la jugada."
                    )
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
