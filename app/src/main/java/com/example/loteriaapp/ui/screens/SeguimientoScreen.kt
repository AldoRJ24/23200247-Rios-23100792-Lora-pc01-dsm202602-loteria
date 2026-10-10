package com.example.loteriaapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.loteriaapp.viewmodel.SorteoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeguimientoScreen(
    navController: NavController,
    sorteoId: String,
    sorteoViewModel: SorteoViewModel = viewModel()
) {
    val sorteo by sorteoViewModel.sorteo.collectAsState()
    val bolillas by sorteoViewModel.bolillas.collectAsState()
    val miJugada by sorteoViewModel.miJugada.collectAsState()

    LaunchedEffect(sorteoId) {
        sorteoViewModel.iniciarSeguimiento(sorteoId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Seguimiento del Sorteo") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
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
            if (sorteo == null) {
                CircularProgressIndicator()
                return@Scaffold
            }

            // Estado del Sorteo
            Text(
                text = when (sorteo!!.getEstadoString()) {
                    "abierto" -> "Esperando inicio del sorteo"
                    "en curso" -> "Sorteo en curso"
                    "finalizado" -> "Sorteo Finalizado"
                    else -> "Estado Desconocido"
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Mi Jugada
            Text("Tu Jugada", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            if (miJugada != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    miJugada!!.numeros.forEach { num ->
                        // Verifica si el número de mi jugada salió en las bolillas
                        val isHit = bolillas.any { it.numero == num }
                        BolillaUi(numero = num, isSelected = true, isHit = isHit)
                    }
                }
            } else {
                Text("No registraste jugada para este sorteo.", style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(modifier = Modifier.height(32.dp))
            Divider()
            Spacer(modifier = Modifier.height(16.dp))

            // Bolillas publicadas
            Text("Bolillas Sorteadas", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                bolillas.forEach { bolilla ->
                    BolillaUi(numero = bolilla.numero, isHit = true) // Aparecen ya pintadas
                }
            }
            if (bolillas.isEmpty()) {
                Text("Aún no hay bolillas...", style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(modifier = Modifier.height(32.dp))

            // RESULTADO FINAL LOCAL
            // Solo si está finalizado y salieron las 6 bolillas
            if (sorteo!!.getEstadoString() == "finalizado" && bolillas.size == 6) {
                val aciertos = miJugada?.numeros?.count { myNum ->
                    bolillas.any { it.numero == myNum }
                } ?: 0

                val (textoResultado, colorResultado) = if (aciertos == 6) {
                    Pair("¡GANADOR!", Color(0xFF4CAF50)) // Verde fuerte
                } else {
                    Pair("SIGUE INTENTANDO\n(Aciertos: $aciertos/6)", MaterialTheme.colorScheme.error)
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = colorResultado.copy(alpha = 0.1f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = textoResultado,
                        style = MaterialTheme.typography.displaySmall,
                        color = colorResultado,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    )
                }
            }
        }
    }
}
