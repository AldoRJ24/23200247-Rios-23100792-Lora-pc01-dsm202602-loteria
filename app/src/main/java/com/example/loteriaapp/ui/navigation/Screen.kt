package com.example.loteriaapp.ui.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object RegistroJugada : Screen("registro_jugada")
    object Seguimiento : Screen("seguimiento/{sorteoId}") {
        fun createRoute(sorteoId: String) = "seguimiento/$sorteoId"
    }
}
