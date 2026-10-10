package com.example.loteriaapp.viewmodel

import androidx.lifecycle.ViewModel
import com.example.loteriaapp.model.Sorteo
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HomeViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _sorteos = MutableStateFlow<List<Sorteo>>(emptyList())
    val sorteos: StateFlow<List<Sorteo>> = _sorteos.asStateFlow()

    private var sorteosListener: ListenerRegistration? = null

    init {
        escucharSorteos()
    }

    private fun escucharSorteos() {
        sorteosListener = db.collection("Sorteos")
            .orderBy("fecha", Query.Direction.DESCENDING) // Asume que la fecha permite ordenamiento
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val lista = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Sorteo::class.java)?.copy(id = doc.id)
                    }
                    _sorteos.value = lista
                }
            }
    }

    override fun onCleared() {
        super.onCleared()
        sorteosListener?.remove()
    }
}
