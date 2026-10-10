package com.example.loteriaaaa.data.repository

import com.example.loteriaaaa.data.model.Bolilla
import com.example.loteriaaaa.data.model.Jugada
import com.example.loteriaaaa.data.model.Sorteo
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

/**
 * Repositorio para la gestión de Sorteos, Bolillas y Jugadas en Cloud Firestore con fallback seguro.
 */
class LoteriaRepository {

    private val firestore: FirebaseFirestore?
        get() = try {
            FirebaseFirestore.getInstance()
        } catch (_: Exception) {
            null
        }

    companion object {
        const val COLLECTION_SORTEOS = "sorteos"
        const val COLLECTION_BOLILLAS = "bolillas"
        const val COLLECTION_JUGADAS = "jugadas"
    }

    /**
     * Obtiene los datos de un sorteo por su ID.
     */
    suspend fun obtenerSorteo(sorteoId: String): Result<Sorteo> {
        val db = firestore
            ?: return Result.success(
                Sorteo(
                    id = sorteoId.ifBlank { "sorteo_demo_1" },
                    nombre = "Sorteo Especial Lotería (Modo Offline)",
                    fecha = "2025-03-30 20:00",
                    estado = "abierto"
                )
            )

        return try {
            val doc = db.collection(COLLECTION_SORTEOS).document(sorteoId).get().await()
            val sorteo = doc.toObject(Sorteo::class.java)?.copy(id = doc.id)
            if (sorteo != null) {
                Result.success(sorteo)
            } else {
                Result.failure(Exception("No se encontró el sorteo con ID: $sorteoId"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Obtiene el sorteo activo actual o crea uno por defecto.
     */
    suspend fun obtenerOEnviarSorteoInicial(): Result<Sorteo> {
        val db = firestore
            ?: return Result.success(
                Sorteo(
                    id = "sorteo_demo_1",
                    nombre = "Sorteo Especial Lotería",
                    fecha = "2025-03-30 20:00",
                    estado = "abierto"
                )
            )

        return try {
            val snapshot = db.collection(COLLECTION_SORTEOS).get().await()
            if (!snapshot.isEmpty) {
                val doc = snapshot.documents.first()
                val sorteo = doc.toObject(Sorteo::class.java)?.copy(id = doc.id)
                if (sorteo != null) {
                    return Result.success(sorteo)
                }
            }

            val nuevoSorteo = Sorteo(
                id = "sorteo_demo_1",
                nombre = "Sorteo Especial Lotería",
                fecha = "2025-03-30 20:00",
                estado = "abierto"
            )
            db.collection(COLLECTION_SORTEOS).document(nuevoSorteo.id).set(nuevoSorteo).await()
            Result.success(nuevoSorteo)
        } catch (e: Exception) {
            Result.success(
                Sorteo(
                    id = "sorteo_demo_1",
                    nombre = "Sorteo Especial Lotería",
                    fecha = "2025-03-30 20:00",
                    estado = "abierto"
                )
            )
        }
    }

    /**
     * Escucha en tiempo real los cambios de un Sorteo.
     */
    fun escucharSorteo(
        sorteoId: String,
        onUpdate: (Sorteo?) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration? {
        val db = firestore ?: run {
            onUpdate(
                Sorteo(
                    id = sorteoId.ifBlank { "sorteo_demo_1" },
                    nombre = "Sorteo Especial Lotería",
                    fecha = "2025-03-30 20:00",
                    estado = "abierto"
                )
            )
            return null
        }

        return try {
            db.collection(COLLECTION_SORTEOS).document(sorteoId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        onError(error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null && snapshot.exists()) {
                        val sorteo = snapshot.toObject(Sorteo::class.java)?.copy(id = snapshot.id)
                        onUpdate(sorteo)
                    } else {
                        onUpdate(null)
                    }
                }
        } catch (e: Exception) {
            onError(e)
            null
        }
    }

    /**
     * Escucha en tiempo real las Bolillas publicadas para un Sorteo.
     */
    fun escucharBolillas(
        sorteoId: String,
        onUpdate: (List<Bolilla>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration? {
        val db = firestore ?: run {
            onUpdate(emptyList())
            return null
        }

        return try {
            db.collection(COLLECTION_SORTEOS)
                .document(sorteoId)
                .collection(COLLECTION_BOLILLAS)
                .orderBy("orden", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        onError(error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val bolillas = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(Bolilla::class.java)?.copy(id = doc.id)
                        }
                        onUpdate(bolillas)
                    } else {
                        onUpdate(emptyList())
                    }
                }
        } catch (e: Exception) {
            onError(e)
            null
        }
    }

    /**
     * Guarda la Jugada en Firestore previa validación de que el estado del sorteo sea "abierto".
     */
    suspend fun guardarJugada(jugada: Jugada): Result<String> {
        val db = firestore
            ?: return Result.success("jugada_demo_id")

        return try {
            if (jugada.sorteoId.isBlank()) {
                return Result.failure(IllegalArgumentException("ID de sorteo no válido."))
            }
            if (jugada.userId.isBlank()) {
                return Result.failure(IllegalArgumentException("Debes estar autenticado para guardar una jugada."))
            }
            if (jugada.numeros.size != 6 || jugada.numeros.distinct().size != 6 || jugada.numeros.any { it !in 1..45 }) {
                return Result.failure(IllegalArgumentException("Debes seleccionar exactamente 6 números distintos entre 1 y 45."))
            }

            val resultSorteo = obtenerSorteo(jugada.sorteoId)
            val sorteo = resultSorteo.getOrNull()
            if (sorteo != null && sorteo.estado.lowercase() != "abierto") {
                return Result.failure(IllegalStateException("No se puede guardar la jugada porque el sorteo no está abierto (Estado actual: '${sorteo.estado}')."))
            }

            val docRef = if (jugada.id.isBlank()) {
                db.collection(COLLECTION_JUGADAS).document()
            } else {
                db.collection(COLLECTION_JUGADAS).document(jugada.id)
            }

            val jugadaFinal = jugada.copy(id = docRef.id)
            docRef.set(jugadaFinal).await()

            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Consulta si el usuario ya realizó una jugada en el sorteo especificado.
     */
    suspend fun obtenerJugadaUsuario(sorteoId: String, userId: String): Result<Jugada?> {
        val db = firestore
            ?: return Result.success(null)

        return try {
            val snapshot = db.collection(COLLECTION_JUGADAS)
                .whereEqualTo("sorteoId", sorteoId)
                .whereEqualTo("userId", userId)
                .get()
                .await()

            val jugada = snapshot.documents.firstOrNull()?.let { doc ->
                doc.toObject(Jugada::class.java)?.copy(id = doc.id)
            }
            Result.success(jugada)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
