package com.example.loteriaapp.ui.jugada

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loteriaapp.data.model.Jugada
import com.example.loteriaapp.data.model.Sorteo
import com.example.loteriaapp.data.repository.AuthRepository
import com.example.loteriaapp.data.repository.LoteriaRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RegistroJugadaUiState(
    val sorteoIdInput: String = "SORTEO-001",
    val sorteo: Sorteo? = null,
    val selectedNumeros: Set<Int> = emptySet(),
    val isJugadaExistente: Boolean = false,
    val isLoadingSorteo: Boolean = false,
    val isSaving: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false,
    val saveSuccess: Boolean = false
)

class RegistroJugadaViewModel(
    private val loteriaRepository: LoteriaRepository = LoteriaRepository(),
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegistroJugadaUiState())
    val uiState: StateFlow<RegistroJugadaUiState> = _uiState.asStateFlow()

    private var sorteoJob: Job? = null
    private var jugadaJob: Job? = null

    init {
        // Cargar el sorteo por defecto al iniciar
        cargarSorteo(_uiState.value.sorteoIdInput)
    }

    fun onSorteoIdChanged(newSorteoId: String) {
        _uiState.update { it.copy(sorteoIdInput = newSorteoId, message = null) }
        cargarSorteo(newSorteoId)
    }

    fun cargarSorteo(sorteoId: String) {
        val cleanId = sorteoId.trim()
        if (cleanId.isBlank()) return

        sorteoJob?.cancel()
        jugadaJob?.cancel()

        _uiState.update {
            it.copy(
                isLoadingSorteo = true,
                message = null,
                saveSuccess = false
            )
        }

        val userId = authRepository.currentUserId.orEmpty()

        // Escuchar cambios en el documento del Sorteo
        sorteoJob = viewModelScope.launch {
            loteriaRepository.getSorteoFlow(cleanId).collect { sorteoObtenido ->
                if (sorteoObtenido != null) {
                    _uiState.update {
                        it.copy(
                            sorteo = sorteoObtenido,
                            isLoadingSorteo = false
                        )
                    }
                } else {
                    // Si el sorteo no existe aún en Firestore, creamos uno por defecto para facilitar pruebas
                    val nuevoSorteo = Sorteo(
                        id = cleanId,
                        nombre = "Sorteo Lotería Real-Time $cleanId",
                        fecha = "2025-03-01",
                        estado = Sorteo.ESTADO_ABIERTO
                    )
                    loteriaRepository.crearOActualizarSorteo(nuevoSorteo)
                    _uiState.update {
                        it.copy(
                            sorteo = nuevoSorteo,
                            isLoadingSorteo = false
                        )
                    }
                }
            }
        }

        // Escuchar cambios en la jugada del usuario si ya existe
        if (userId.isNotBlank()) {
            jugadaJob = viewModelScope.launch {
                loteriaRepository.getJugadaFlow(cleanId, userId).collect { jugadaObtenida ->
                    if (jugadaObtenida != null && jugadaObtenida.numeros.isNotEmpty()) {
                        _uiState.update {
                            it.copy(
                                selectedNumeros = jugadaObtenida.numeros.toSet(),
                                isJugadaExistente = true
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isJugadaExistente = false
                            )
                        }
                    }
                }
            }
        }
    }

    fun toggleNumero(numero: Int) {
        if (numero !in 1..45) return

        val currentSelection = _uiState.value.selectedNumeros.toMutableSet()
        if (currentSelection.contains(numero)) {
            currentSelection.remove(numero)
        } else {
            if (currentSelection.size >= 6) {
                _uiState.update {
                    it.copy(
                        message = "Solo puedes seleccionar exactamente 6 números.",
                        isError = true
                    )
                }
                return
            }
            currentSelection.add(numero)
        }

        _uiState.update {
            it.copy(
                selectedNumeros = currentSelection,
                message = null,
                isError = false
            )
        }
    }

    fun limpiarSeleccion() {
        _uiState.update {
            it.copy(
                selectedNumeros = emptySet(),
                message = null,
                isError = false
            )
        }
    }

    fun guardarJugada() {
        val state = _uiState.value
        val userId = authRepository.currentUserId

        if (userId.isNullOrBlank()) {
            _uiState.update {
                it.copy(
                    message = "Debe iniciar sesión para guardar la jugada.",
                    isError = true
                )
            }
            return
        }

        val sorteo = state.sorteo
        if (sorteo == null) {
            _uiState.update {
                it.copy(
                    message = "El sorteo no está cargado.",
                    isError = true
                )
            }
            return
        }

        // Regla de Negocio: Solo se permite guardar si el estado es "abierto"
        if (sorteo.estado != Sorteo.ESTADO_ABIERTO) {
            _uiState.update {
                it.copy(
                    message = "No se puede guardar la jugada. El sorteo está en estado '${sorteo.estado}'. Solo se permite en estado 'abierto'.",
                    isError = true
                )
            }
            return
        }

        // Regla de Negocio: Exactamente 6 números distintos entre 1 y 45
        if (state.selectedNumeros.size != 6) {
            _uiState.update {
                it.copy(
                    message = "Debes seleccionar exactamente 6 números distintos entre 1 y 45.",
                    isError = true
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, message = null) }
            val jugada = Jugada(
                sorteoId = sorteo.id,
                userId = userId,
                numeros = state.selectedNumeros.sorted()
            )

            val result = loteriaRepository.guardarJugada(jugada)
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            message = "¡Jugada registrada exitosamente con los números: ${jugada.numeros}!",
                            isError = false,
                            saveSuccess = true,
                            isJugadaExistente = true
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            message = "Error al registrar la jugada: ${e.localizedMessage}",
                            isError = true
                        )
                    }
                }
            )
        }
    }

    fun clearMessage() {
        _uiState.update { it.copy(message = null) }
    }

    override fun onCleared() {
        // Cancelación explícita de listeners y corrutinas
        sorteoJob?.cancel()
        jugadaJob?.cancel()
    }
}
