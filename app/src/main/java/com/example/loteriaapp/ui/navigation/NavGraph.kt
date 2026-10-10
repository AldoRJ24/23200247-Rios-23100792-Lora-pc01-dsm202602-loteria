package com.example.loteriaapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.loteriaapp.ui.auth.AuthViewModel
import com.example.loteriaapp.ui.auth.LoginScreen
import com.example.loteriaapp.ui.auth.RegisterScreen
import com.example.loteriaapp.ui.jugada.RegistroJugadaScreen
import com.example.loteriaapp.ui.jugada.RegistroJugadaViewModel
import com.example.loteriaapp.ui.seguimiento.SeguimientoScreen
import com.example.loteriaapp.ui.seguimiento.SeguimientoViewModel

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel = viewModel()
) {
    val authState by authViewModel.uiState.collectAsState()

    val startDestination = if (authState.isLoggedIn) {
        Screen.RegistroJugada.route
    } else {
        Screen.Login.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                viewModel = authViewModel,
                onNavigateToRegister = {
                    authViewModel.clearError()
                    navController.navigate(Screen.Register.route)
                },
                onLoginSuccess = {
                    navController.navigate(Screen.RegistroJugada.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                viewModel = authViewModel,
                onNavigateToLogin = {
                    authViewModel.clearError()
                    navController.popBackStack()
                },
                onRegisterSuccess = {
                    navController.navigate(Screen.RegistroJugada.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.RegistroJugada.route) {
            val registroViewModel: RegistroJugadaViewModel = viewModel()
            RegistroJugadaScreen(
                viewModel = registroViewModel,
                userEmail = authState.userEmail,
                onLogout = {
                    authViewModel.logout()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToSeguimiento = { sorteoId ->
                    navController.navigate(Screen.Seguimiento.createRoute(sorteoId))
                }
            )
        }

        composable(
            route = Screen.Seguimiento.route,
            arguments = listOf(
                navArgument("sorteoId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val sorteoId = backStackEntry.arguments?.getString("sorteoId") ?: ""
            val seguimientoViewModel: SeguimientoViewModel = viewModel()

            SeguimientoScreen(
                viewModel = seguimientoViewModel,
                sorteoId = sorteoId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
