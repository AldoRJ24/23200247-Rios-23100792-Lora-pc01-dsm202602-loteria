package com.example.loteriaaaa.data.model

/**
 * Modelo de datos para una Bolilla publicada en Cloud Firestore.
 * numero: Valor entero entre 1 y 45.
 * orden: Posición de salida de la bolilla (1 al 6).
 */
data class Bolilla(
    val id: String = "",
    val sorteoId: String = "",
    val numero: Int = 0,
    val orden: Int = 0
)
