package com.example.loteriaapp.ui.seguimiento

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loteriaapp.data.model.Bolilla
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

data class SeguimientoUiState(
    val sorteoId: String = "",
    val sorteo: Sorteo? = null,
    val jugada: Jugada? = null,
    val bolillas: List<Bolilla> = emptyList(),
    val isLoading: Boolean = true,
    val cantidadBolillasPublicadas: Int = 0,
    val aciertosCount: Int = 0,
    val numerosAcertados: Set<Int> = emptySet(),
    val mensajeEstado: String = "",
    val error: String? = null
)

class SeguimientoViewModel(
    private val loteriaRepository: LoteriaRepository = LoteriaRepository(),
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SeguimientoUiState())
    val uiState: StateFlow<SeguimientoUiState> = _uiState.asStateFlow()

    private var sorteoJob: Job? = null
    private var jugadaJob: Job? = null
    private var bolillasJob: Job? = null

    fun iniciarSeguimiento(sorteoId: String) {
        if (sorteoId.isBlank()) return

        val userId = authRepository.currentUserId.orEmpty()

        _uiState.update {
            it.copy(
                sorteoId = sorteoId,
                isLoading = true,
                error = null
            )
        }

        // Cancelar suscripciones previas si existían
        detenerSeguimiento()

        // 1. Escuchar Sorteo en tiempo real
        sorteoJob = viewModelScope.launch {
            loteriaRepository.getSorteoFlow(sorteoId).collect { sorteo ->
                _uiState.update { currentState ->
                    val newState = currentState.copy(sorteo = sorteo, isLoading = false)
                    calcularIndicadoresYMensaje(newState)
                }
            }
        }

        // 2. Escuchar Jugada del Usuario en tiempo real
        if (userId.isNotBlank()) {
            jugadaJob = viewModelScope.launch {
                loteriaRepository.getJugadaFlow(sorteoId, userId).collect { jugada ->
                    _uiState.update { currentState ->
                        val newState = currentState.copy(jugada = jugada)
                        calcularIndicadoresYMensaje(newState)
                    }
                }
            }
        }

        // 3. Escuchar Bolillas publicadas en tiempo real
        bolillasJob = viewModelScope.launch {
            loteriaRepository.getBolillasFlow(sorteoId).collect { bolillas ->
                _uiState.update { currentState ->
                    val newState = currentState.copy(bolillas = bolillas)
                    calcularIndicadoresYMensaje(newState)
                }
            }
        }
    }

    /**
     * CÁLCULOS EN APP (NO EN FIRESTORE)
     * Reglas de negocio e indicadores:
     * - Contar bolillas publicadas (0 a 6).
     * - Calcular aciertos comparando los 6 números del usuario con las bolillas publicadas.
     * - Resaltar visualmente los números acertados.
     * - Definir mensajes de estado según las reglas de la consigna.
     */
    private fun calcularIndicadoresYMensaje(state: SeguimientoUiState): SeguimientoUiState {
        val sorteo = state.sorteo
        val jugada = state.jugada
        val bolillas = state.bolillas

        val cantidadBolillas = bolillas.size
        val numerosBolillasPublicadas = bolillas.map { it.numero }.toSet()

        val userNumeros = jugada?.numeros ?: emptyList()

        // Números que el usuario acertó
        val acertadosSet = userNumeros.filter { numerosBolillasPublicadas.contains(it) }.toSet()
        val aciertosCount = acertadosSet.size

        val estadoSorteo = sorteo?.estado ?: Sorteo.ESTADO_ABIERTO

        val mensaje = when (estadoSorteo) {
            Sorteo.ESTADO_ABIERTO -> "Esperando inicio del sorteo"
            Sorteo.ESTADO_EN_CURSO -> "Sorteo en curso"
            Sorteo.ESTADO_FINALIZADO -> {
                if (cantidadBolillas >= 6) {
                    if (aciertosCount == 6) {
                        "¡GANADOR!"
                    } else {
                        "SIGUE INTENTANDO"
                    }
                } else {
                    "Esperando resultados finales..."
                }
            }
            else -> "Estado desconocido"
        }

        return state.copy(
            cantidadBolillasPublicadas = cantidadBolillas,
            aciertosCount = aciertosCount,
            numerosAcertados = acertadosSet,
            mensajeEstado = mensaje
        )
    }

    // Funciones de simulación para pruebas en vivo
    fun simularSiguienteBolilla() {
        val state = _uiState.value
        val sorteoId = state.sorteoId
        if (sorteoId.isBlank()) return

        val bolillasActuales = state.bolillas
        val orden = bolillasActuales.size + 1
        if (orden > 6) return

        val numerosUsados = bolillasActuales.map { it.numero }.toSet()
        
        // Si el usuario tiene una jugada, preferir sacar un número de su jugada o uno aleatorio
        val userNumeros = state.jugada?.numeros ?: emptyList()
        val candidatoNoUsado = userNumeros.firstOrNull { !numerosUsados.contains(it) }
            ?: ((1..45) - numerosUsados).shuffled().firstOrNull()
            ?: (1..45).random()

        viewModelScope.launch {
            loteriaRepository.agregarBolilla(sorteoId, candidatoNoUsado, orden)
            // Si llega a 6 bolillas y el sorteo está en curso, lo finalizamos automáticamente o dejamos control al usuario
            if (orden == 6 && state.sorteo?.estado == Sorteo.ESTADO_EN_CURSO) {
                loteriaRepository.actualizarEstadoSorteo(sorteoId, Sorteo.ESTADO_FINALIZADO)
            }
        }
    }

    fun cambiarEstadoSorteo(nuevoEstado: String) {
        val sorteoId = _uiState.value.sorteoId
        if (sorteoId.isBlank()) return

        viewModelScope.launch {
            loteriaRepository.actualizarEstadoSorteo(sorteoId, nuevoEstado)
        }
    }

    fun reiniciarSimulacion() {
        val sorteoId = _uiState.value.sorteoId
        if (sorteoId.isBlank()) return

        viewModelScope.launch {
            loteriaRepository.reiniciarBolillas(sorteoId)
            loteriaRepository.actualizarEstadoSorteo(sorteoId, Sorteo.ESTADO_ABIERTO)
        }
    }

    private fun detenerSeguimiento() {
        sorteoJob?.cancel()
        jugadaJob?.cancel()
        bolillasJob?.cancel()
        sorteoJob = null
        jugadaJob = null
        bolillasJob = null
    }

    override fun onCleared() {
        super.onCleared()
        // Remoción garantizada de los listeners al destruir el ViewModel
        detenerSeguimiento()
    }
}
