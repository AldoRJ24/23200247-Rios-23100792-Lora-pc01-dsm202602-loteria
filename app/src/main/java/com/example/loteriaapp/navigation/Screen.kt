package com.example.loteriaapp.navigation

/**
 * Define las rutas de navegación de la aplicación para Navigation Compose.
 */
sealed class Screen(val route: String) {
    object Auth : Screen("auth")
    
    // Ruta intermedia para listar sorteos disponibles o redirigir al activo
    object Home : Screen("home") 
    
    // Registro de jugada, requiere el ID del sorteo
    object RegistroJugada : Screen("registro_jugada/{sorteoId}") {
        fun createRoute(sorteoId: String) = "registro_jugada/$sorteoId"
    }
    
    // Pantalla de seguimiento en tiempo real, requiere el ID del sorteo
    object Seguimiento : Screen("seguimiento/{sorteoId}") {
        fun createRoute(sorteoId: String) = "seguimiento/$sorteoId"
    }
}
