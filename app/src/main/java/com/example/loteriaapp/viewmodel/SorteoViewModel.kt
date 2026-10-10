package com.example.loteriaapp.viewmodel

import androidx.lifecycle.ViewModel
import com.example.loteriaapp.model.Bolilla
import com.example.loteriaapp.model.Jugada
import com.example.loteriaapp.model.Sorteo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SorteoViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Estado del Sorteo (Tiempo Real)
    private val _sorteo = MutableStateFlow<Sorteo?>(null)
    val sorteo: StateFlow<Sorteo?> = _sorteo.asStateFlow()

    // Estado de Bolillas extraídas (Tiempo Real)
    private val _bolillas = MutableStateFlow<List<Bolilla>>(emptyList())
    val bolillas: StateFlow<List<Bolilla>> = _bolillas.asStateFlow()

    // Jugada del usuario actual
    private val _miJugada = MutableStateFlow<Jugada?>(null)
    val miJugada: StateFlow<Jugada?> = _miJugada.asStateFlow()

    // Estado de carga o error general
    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // Control de listeners para removerlos cuando ya no se ocupen
    private var sorteoListener: ListenerRegistration? = null
    private var bolillasListener: ListenerRegistration? = null

    /**
     * Inicia la escucha en tiempo real de un Sorteo específico y sus bolillas,
     * y obtiene la jugada del usuario si existe.
     */
    fun iniciarSeguimiento(sorteoId: String) {
        if (sorteoId.isBlank()) return

        escucharSorteo(sorteoId)
        escucharBolillas(sorteoId)
        obtenerMiJugada(sorteoId)
    }

    private fun escucharSorteo(sorteoId: String) {
        sorteoListener?.remove()
        sorteoListener = db.collection("Sorteos").document(sorteoId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _uiState.value = UiState.Error("Error al escuchar sorteo: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val s = snapshot.toObject(Sorteo::class.java)
                    // Si el id no viene en el documento, se lo asignamos del snapshot.id
                    _sorteo.value = s?.copy(id = snapshot.id)
                }
            }
    }

    private fun escucharBolillas(sorteoId: String) {
        bolillasListener?.remove()
        bolillasListener = db.collection("Bolillas")
            .whereEqualTo("sorteoId", sorteoId)
            // Se puede ordenar del lado del cliente o usar orderBy("ordenSalida") si hay índice
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _uiState.value = UiState.Error("Error al escuchar bolillas: ${error.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val lista = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Bolilla::class.java)?.copy(id = doc.id)
                    }.sortedBy { it.ordenSalida } // Ordenar localmente por precaución
                    
                    _bolillas.value = lista
                }
            }
    }

    private fun obtenerMiJugada(sorteoId: String) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("Jugadas")
            .whereEqualTo("sorteoId", sorteoId)
            .whereEqualTo("usuarioUid", uid)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    val jugadaDoc = snapshot.documents[0]
                    _miJugada.value = jugadaDoc.toObject(Jugada::class.java)?.copy(id = jugadaDoc.id)
                } else {
                    _miJugada.value = null
                }
            }
            .addOnFailureListener {
                _uiState.value = UiState.Error("Error al cargar la jugada: ${it.message}")
            }
    }

    /**
     * Guarda una nueva jugada para el sorteo especificado.
     * @param numeros Lista exacta de 6 números distintos (1 al 45)
     */
    fun registrarJugada(sorteoId: String, numeros: List<Int>) {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            _uiState.value = UiState.Error("Usuario no autenticado")
            return
        }

        if (numeros.size != 6 || numeros.any { it !in 1..45 } || numeros.distinct().size != 6) {
            _uiState.value = UiState.Error("Debes elegir exactamente 6 números distintos entre 1 y 45.")
            return
        }

        val nuevaJugada = Jugada(
            sorteoId = sorteoId,
            usuarioUid = uid,
            numeros = numeros.sorted()
        )

        _uiState.value = UiState.Loading
        db.collection("Jugadas")
            .add(nuevaJugada)
            .addOnSuccessListener { documentRef ->
                // Actualizar el estado local
                _miJugada.value = nuevaJugada.copy(id = documentRef.id)
                _uiState.value = UiState.Success("Jugada registrada con éxito")
            }
            .addOnFailureListener {
                _uiState.value = UiState.Error(it.message ?: "Error al guardar la jugada")
            }
    }

    fun resetUiState() {
        if (_uiState.value !is UiState.Idle) {
            _uiState.value = UiState.Idle
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Liberar listeners para no dejar procesos en memoria
        sorteoListener?.remove()
        bolillasListener?.remove()
    }
}

sealed class UiState {
    object Idle : UiState()
    object Loading : UiState()
    data class Success(val message: String) : UiState()
    data class Error(val message: String) : UiState()
}
