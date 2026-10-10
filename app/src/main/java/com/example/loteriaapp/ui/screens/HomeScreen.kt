package com.example.loteriaapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.loteriaapp.model.Sorteo
import com.example.loteriaapp.navigation.Screen
import com.example.loteriaapp.viewmodel.AuthViewModel
import com.example.loteriaapp.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    authViewModel: AuthViewModel = viewModel(),
    homeViewModel: HomeViewModel = viewModel()
) {
    val currentUserEmail by authViewModel.currentUserEmail.collectAsState()
    val sorteos by homeViewModel.sorteos.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lotería - Sorteos") },
                actions = {
                    IconButton(onClick = {
                        authViewModel.logout()
                        navController.navigate(Screen.Auth.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }) {
                        Text("Salir")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Bienvenido,\n$currentUserEmail",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))
            Text("Sorteos Disponibles", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))

            if (sorteos.isEmpty()) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text("No hay sorteos registrados.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(sorteos) { sorteo ->
                        SorteoCard(
                            sorteo = sorteo,
                            onRegistrarJugada = {
                                navController.navigate(Screen.RegistroJugada.createRoute(sorteo.id))
                            },
                            onVerSeguimiento = {
                                navController.navigate(Screen.Seguimiento.createRoute(sorteo.id))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SorteoCard(
    sorteo: Sorteo,
    onRegistrarJugada: () -> Unit,
    onVerSeguimiento: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(text = sorteo.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(text = "Fecha: ${sorteo.getFechaString()}", style = MaterialTheme.typography.bodyMedium)
            
            val estadoReal = sorteo.getEstadoString()
            val estadoColor = when (estadoReal) {
                "abierto" -> MaterialTheme.colorScheme.primary
                "en curso" -> MaterialTheme.colorScheme.secondary
                else -> MaterialTheme.colorScheme.error
            }
            
            Text(
                text = "Estado: ${estadoReal.uppercase()}",
                style = MaterialTheme.typography.bodyMedium,
                color = estadoColor,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(
                    onClick = onRegistrarJugada,
                    enabled = estadoReal == "abierto"
                ) {
                    Text("Jugar")
                }
                
                Button(onClick = onVerSeguimiento) {
                    Text("Seguimiento")
                }
            }
        }
    }
}
