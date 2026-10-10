package com.example.loteriaapp.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.loteriaapp.ui.screens.AuthScreen
import com.example.loteriaapp.ui.screens.HomeScreen
import com.example.loteriaapp.ui.screens.RegistroJugadaScreen
import com.example.loteriaapp.ui.screens.SeguimientoScreen

@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Screen.Auth.route) {
        
        composable(Screen.Auth.route) {
            AuthScreen(navController)
        }
        
        composable(Screen.Home.route) {
            HomeScreen(navController)
        }
        
        composable(Screen.RegistroJugada.route) { backStackEntry ->
            val sorteoId = backStackEntry.arguments?.getString("sorteoId") ?: ""
            RegistroJugadaScreen(navController, sorteoId)
        }
        
        composable(Screen.Seguimiento.route) { backStackEntry ->
            val sorteoId = backStackEntry.arguments?.getString("sorteoId") ?: ""
            SeguimientoScreen(navController, sorteoId)
        }
    }
}
