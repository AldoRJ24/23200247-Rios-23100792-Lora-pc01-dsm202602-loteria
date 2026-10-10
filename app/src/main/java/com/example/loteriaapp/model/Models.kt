package com.example.loteriaapp.model

import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Representa un Sorteo en Firestore.
 * Colección: 'Sorteos'
 */
data class Sorteo(
    val id: String = "",
    val nombre: String = "",
    val fecha: Any? = null, // Cambiado a Any? para soportar String o Timestamp y no crashear
    val estado: Any? = "abierto" // Valores permitidos: "abierto", "en curso", "finalizado"
) {
    // Función auxiliar para obtener la fecha como String siempre
    fun getFechaString(): String {
        return when (fecha) {
            is String -> fecha
            is Timestamp -> {
                val date = fecha.toDate()
                val format = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                format.format(date)
            }
            else -> "Fecha no especificada"
        }
    }
    
    // Función auxiliar para obtener el estado como String siempre
    fun getEstadoString(): String {
        return when (estado) {
            is String -> estado
            is Boolean -> if (estado) "abierto" else "finalizado"
            else -> "abierto"
        }
    }
}

/**
 * Representa la jugada de un usuario.
 * Colección: 'Jugadas'
 */
data class Jugada(
    val id: String = "",
    val sorteoId: String = "",
    val usuarioUid: String = "",
    val numeros: List<Int> = emptyList() // Debe contener exactamente 6 números distintos entre 1 y 45
)

/**
 * Representa una bolilla extraída en un sorteo.
 * Colección: 'Bolillas'
 */
data class Bolilla(
    val id: String = "",
    val sorteoId: String = "",
    val numero: Int = 0,
    val ordenSalida: Int = 0 // Utilizado para ordenar las bolillas en tiempo real
)
