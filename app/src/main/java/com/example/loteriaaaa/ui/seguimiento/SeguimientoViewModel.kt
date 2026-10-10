package com.example.loteriaaaa.ui.seguimiento

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loteriaaaa.data.model.Bolilla
import com.example.loteriaaaa.data.model.Jugada
import com.example.loteriaaaa.data.model.Sorteo
import com.example.loteriaaaa.data.repository.LoteriaRepository
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SeguimientoUiState(
    val sorteo: Sorteo? = null,
    val bolillasPublicadas: List<Bolilla> = emptyList(),
    val jugadaUsuario: Jugada? = null,
    val aciertosCount: Int = 0,
    val numerosAcertados: Set<Int> = emptySet(),
    val mensajeEstado: String = "Cargando sorteo...",
    val esGanador: Boolean = false,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

class SeguimientoViewModel(
    private val loteriaRepository: LoteriaRepository = LoteriaRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SeguimientoUiState())
    val uiState: StateFlow<SeguimientoUiState> = _uiState.asStateFlow()

    // Referencias explícitas a ListenerRegistration para evitar fugas de recursos
    private var sorteoListenerRegistration: ListenerRegistration? = null
    private var bolillasListenerRegistration: ListenerRegistration? = null

    /**
     * Inicia la escucha en tiempo real del sorteo y sus bolillas.
     */
    fun iniciarSeguimiento(sorteoId: String, userId: String) {
        detenerSeguimiento()

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        // 1. Obtener jugada del usuario si existe
        if (userId.isNotBlank() && sorteoId.isNotBlank()) {
            viewModelScope.launch {
                val resultJugada = loteriaRepository.obtenerJugadaUsuario(sorteoId, userId)
                val jugada = resultJugada.getOrNull()
                _uiState.update { currentState ->
                    val newState = currentState.copy(jugadaUsuario = jugada)
                    calcularLogicaLocal(newState)
                }
            }
        }

        // 2. Escuchar el Sorteo en tiempo real con Firestore
        sorteoListenerRegistration = loteriaRepository.escucharSorteo(
            sorteoId = sorteoId,
            onUpdate = { sorteoActualizado ->
                _uiState.update { currentState ->
                    val newState = currentState.copy(
                        sorteo = sorteoActualizado,
                        isLoading = false
                    )
                    calcularLogicaLocal(newState)
                }
            },
            onError = { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Error al escuchar el sorteo: ${error.localizedMessage}"
                    )
                }
            }
        )

        // 3. Escuchar las Bolillas publicadas en tiempo real con Firestore
        bolillasListenerRegistration = loteriaRepository.escucharBolillas(
            sorteoId = sorteoId,
            onUpdate = { bolillasActualizadas ->
                val bolillasOrdenadas = bolillasActualizadas.sortedBy { it.orden }
                _uiState.update { currentState ->
                    val newState = currentState.copy(bolillasPublicadas = bolillasOrdenadas)
                    calcularLogicaLocal(newState)
                }
            },
            onError = { error ->
                _uiState.update {
                    it.copy(
                        errorMessage = "Error al escuchar las bolillas: ${error.localizedMessage}"
                    )
                }
            }
        )
    }

    /**
     * Lógica de Negocio y Cálculos Locales en la App.
     * Ningún resultado ni acierto se almacena en Firestore.
     */
    private fun calcularLogicaLocal(state: SeguimientoUiState): SeguimientoUiState {
        val estadoSorteo = state.sorteo?.estado?.lowercase() ?: "abierto"
        val bolillasNumeros = state.bolillasPublicadas.map { it.numero }.toSet()
        val jugadaNumeros = state.jugadaUsuario?.numeros?.toSet() ?: emptySet()

        // Cálculo de aciertos entre jugada del usuario y bolillas publicadas
        val aciertos = jugadaNumeros.intersect(bolillasNumeros)
        val aciertosCount = aciertos.size
        val esGanador = (aciertosCount == 6)

        // Reglas estrictas de Mensajes de Estado:
        // * Si estado es "abierto": "Esperando inicio del sorteo..."
        // * Si estado es "en curso": "Sorteo en curso"
        // * Si estado es "finalizado" y hay < 6 bolillas: "Esperando resultados..."
        // * Si estado es "finalizado" y se recibieron las 6 bolillas:
        //   - 6 aciertos: "GANADOR"
        //   - Menos de 6 aciertos: "SIGUE INTENTANDO"
        val mensajeEstado = when (estadoSorteo) {
            "abierto" -> "Esperando inicio del sorteo..."
            "en curso" -> "Sorteo en curso"
            "finalizado" -> {
                if (state.bolillasPublicadas.size < 6) {
                    "Esperando resultados..."
                } else {
                    if (esGanador) "GANADOR" else "SIGUE INTENTANDO"
                }
            }
            else -> "Esperando inicio del sorteo..."
        }

        return state.copy(
            aciertosCount = aciertosCount,
            numerosAcertados = aciertos,
            mensajeEstado = mensajeEstado,
            esGanador = esGanador
        )
    }

    /**
     * Limpia explícitamente las suscripciones de Firestore.
     */
    fun detenerSeguimiento() {
        sorteoListenerRegistration?.remove()
        sorteoListenerRegistration = null

        bolillasListenerRegistration?.remove()
        bolillasListenerRegistration = null
    }

    /**
     * Limpieza automática de recursos al destruir el ViewModel para evitar Memory Leaks.
     */
    override fun onCleared() {
        super.onCleared()
        detenerSeguimiento()
    }
}
