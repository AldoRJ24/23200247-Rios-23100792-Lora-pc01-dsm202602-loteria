package com.example.loteriaaaa.data.model

/**
 * Modelo de datos para la Jugada realizada por un usuario en Cloud Firestore.
 * numeros: Lista de exactamente 6 números únicos entre 1 y 45.
 */
data class Jugada(
    val id: String = "",
    val sorteoId: String = "",
    val userId: String = "",
    val numeros: List<Int> = emptyList()
)
