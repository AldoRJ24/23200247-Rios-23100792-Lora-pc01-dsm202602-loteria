package com.example.loteriaaaa.data.model

/**
 * Modelo de datos para representar un Sorteo de Lotería en Cloud Firestore.
 * Estados válidos: "abierto", "en curso", "finalizado".
 */
data class Sorteo(
    val id: String = "",
    val nombre: String = "",
    val fecha: String = "",
    val estado: String = "abierto"
)
