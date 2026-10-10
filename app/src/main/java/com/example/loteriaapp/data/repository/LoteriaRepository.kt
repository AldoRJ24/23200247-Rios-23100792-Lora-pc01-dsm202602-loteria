package com.example.loteriaapp.data.repository

import com.example.loteriaapp.data.model.Bolilla
import com.example.loteriaapp.data.model.Jugada
import com.example.loteriaapp.data.model.Sorteo
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class LoteriaRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /**
     * Escucha en tiempo real el documento de un sorteo en `sorteos/{sorteoId}`.
     * Cancela el snapshot listener automáticamente cuando el Flow finaliza o se cancela.
     */
    fun getSorteoFlow(sorteoId: String): Flow<Sorteo?> = callbackFlow {
        if (sorteoId.isBlank()) {
            trySend(null)
            awaitClose { }
            return@callbackFlow
        }

        val docRef = db.collection("sorteos").document(sorteoId)
        val listener: ListenerRegistration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val sorteo = snapshot.toObject(Sorteo::class.java)?.copy(id = snapshot.id)
                trySend(sorteo)
            } else {
                trySend(null)
            }
        }

        awaitClose {
            listener.remove()
        }
    }

    /**
     * Escucha en tiempo real la jugada del usuario en `sorteos/{sorteoId}/jugadas/{userId}`.
     */
    fun getJugadaFlow(sorteoId: String, userId: String): Flow<Jugada?> = callbackFlow {
        if (sorteoId.isBlank() || userId.isBlank()) {
            trySend(null)
            awaitClose { }
            return@callbackFlow
        }

        val docRef = db.collection("sorteos")
            .document(sorteoId)
            .collection("jugadas")
            .document(userId)

        val listener: ListenerRegistration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null && snapshot.exists()) {
                val jugada = snapshot.toObject(Jugada::class.java)
                trySend(jugada)
            } else {
                trySend(null)
            }
        }

        awaitClose {
            listener.remove()
        }
    }

    /**
     * Escucha en tiempo real la subcolección de bolillas en `sorteos/{sorteoId}/bolillas`.
     * Ordena las bolillas por el campo `orden`.
     */
    fun getBolillasFlow(sorteoId: String): Flow<List<Bolilla>> = callbackFlow {
        if (sorteoId.isBlank()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        val colRef = db.collection("sorteos")
            .document(sorteoId)
            .collection("bolillas")
            .orderBy("orden", Query.Direction.ASCENDING)

        val listener: ListenerRegistration = colRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val bolillas = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Bolilla::class.java)?.copy(id = doc.id)
                }
                trySend(bolillas)
            } else {
                trySend(emptyList())
            }
        }

        awaitClose {
            listener.remove()
        }
    }

    /**
     * Registra o actualiza la jugada del usuario en `sorteos/{sorteoId}/jugadas/{userId}`.
     */
    suspend fun guardarJugada(jugada: Jugada): Result<Unit> {
        return try {
            val docRef = db.collection("sorteos")
                .document(jugada.sorteoId)
                .collection("jugadas")
                .document(jugada.userId)

            val data = mapOf(
                "sorteoId" to jugada.sorteoId,
                "userId" to jugada.userId,
                "numeros" to jugada.numeros
            )

            docRef.set(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Crea o actualiza la información básica del sorteo en `sorteos/{sorteoId}`.
     */
    suspend fun crearOActualizarSorteo(sorteo: Sorteo): Result<Unit> {
        return try {
            val docRef = db.collection("sorteos").document(sorteo.id)
            val data = mapOf(
                "id" to sorteo.id,
                "nombre" to sorteo.nombre,
                "fecha" to sorteo.fecha,
                "estado" to sorteo.estado
            )
            docRef.set(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Actualiza el estado de un sorteo ("abierto", "en_curso", "finalizado").
     */
    suspend fun actualizarEstadoSorteo(sorteoId: String, nuevoEstado: String): Result<Unit> {
        return try {
            db.collection("sorteos")
                .document(sorteoId)
                .update("estado", nuevoEstado)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Agrega una bolilla publicada en `sorteos/{sorteoId}/bolillas/{bolillaId}`.
     */
    suspend fun agregarBolilla(sorteoId: String, numero: Int, orden: Int): Result<Unit> {
        return try {
            val bolillasRef = db.collection("sorteos")
                .document(sorteoId)
                .collection("bolillas")

            val bolillaDoc = bolillasRef.document("bolilla_$orden")
            val data = mapOf(
                "sorteoId" to sorteoId,
                "numero" to numero,
                "orden" to orden
            )
            bolillaDoc.set(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Limpia todas las bolillas de un sorteo (utilidad para reiniciar pruebas).
     */
    suspend fun reiniciarBolillas(sorteoId: String): Result<Unit> {
        return try {
            val snapshot = db.collection("sorteos")
                .document(sorteoId)
                .collection("bolillas")
                .get()
                .await()

            for (doc in snapshot.documents) {
                doc.reference.delete().await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
