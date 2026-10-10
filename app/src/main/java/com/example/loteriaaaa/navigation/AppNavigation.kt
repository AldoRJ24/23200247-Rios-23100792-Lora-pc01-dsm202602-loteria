package com.example.loteriaaaa.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.loteriaaaa.ui.auth.AuthViewModel
import com.example.loteriaaaa.ui.auth.LoginScreen
import com.example.loteriaaaa.ui.registro.RegistroJugadaScreen
import com.example.loteriaaaa.ui.registro.RegistroJugadaViewModel
import com.example.loteriaaaa.ui.seguimiento.SeguimientoScreen
import com.example.loteriaaaa.ui.seguimiento.SeguimientoViewModel

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object RegistroJugada : Screen("registro_jugada")
    data object Seguimiento : Screen("seguimiento/{sorteoId}") {
        fun createRoute(sorteoId: String) = "seguimiento/$sorteoId"
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    val authViewModel: AuthViewModel = viewModel()
    val registroViewModel: RegistroJugadaViewModel = viewModel()
    val seguimientoViewModel: SeguimientoViewModel = viewModel()

    val authState by authViewModel.uiState.collectAsState()
    val registroState by registroViewModel.uiState.collectAsState()

    NavHost(
        navController = navController,
        startDestination = Screen.Login.route
    ) {
        // 1. Pantalla Login / Registro
        composable(Screen.Login.route) {
            LoginScreen(
                viewModel = authViewModel,
                onNavigateToRegistro = {
                    navController.navigate(Screen.RegistroJugada.route)
                },
                onNavigateToSeguimiento = {
                    val sorteoId = registroState.sorteo?.id ?: "sorteo_demo_1"
                    navController.navigate(Screen.Seguimiento.createRoute(sorteoId))
                }
            )
        }

        // 2. Pantalla Registro de Jugada
        composable(Screen.RegistroJugada.route) {
            val userId = authState.userId ?: "user_demo"
            RegistroJugadaScreen(
                viewModel = registroViewModel,
                userId = userId,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToSeguimiento = { sorteoId ->
                    val id = sorteoId.ifBlank { "sorteo_demo_1" }
                    navController.navigate(Screen.Seguimiento.createRoute(id))
                }
            )
        }

        // 3. Pantalla Seguimiento del Sorteo en Tiempo Real
        composable(
            route = Screen.Seguimiento.route,
            arguments = listOf(navArgument("sorteoId") { type = NavType.StringType })
        ) { backStackEntry ->
            val sorteoId = backStackEntry.arguments?.getString("sorteoId") ?: "sorteo_demo_1"
            val userId = authState.userId ?: "user_demo"

            SeguimientoScreen(
                viewModel = seguimientoViewModel,
                sorteoId = sorteoId,
                userId = userId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
