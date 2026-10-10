package com.example.loteriaapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.loteriaapp.viewmodel.SorteoViewModel
import com.example.loteriaapp.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistroJugadaScreen(
    navController: NavController,
    sorteoId: String,
    sorteoViewModel: SorteoViewModel = viewModel()
) {
    val sorteo by sorteoViewModel.sorteo.collectAsState()
    val miJugada by sorteoViewModel.miJugada.collectAsState()
    val uiState by sorteoViewModel.uiState.collectAsState()

    val numerosSeleccionados = remember { mutableStateListOf<Int>() }

    LaunchedEffect(sorteoId) {
        sorteoViewModel.iniciarSeguimiento(sorteoId)
    }

    // Efecto para retroceder si la jugada fue exitosa
    LaunchedEffect(uiState) {
        if (uiState is UiState.Success) {
            navController.popBackStack()
            sorteoViewModel.resetUiState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Registrar Jugada") },
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

            if (sorteo!!.getEstadoString() != "abierto") {
                Text(
                    text = "El sorteo ya no está abierto.",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.error
                )
                return@Scaffold
            }

            if (miJugada != null) {
                Text(
                    text = "Ya has registrado tu jugada para este sorteo.",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    miJugada!!.numeros.forEach { num ->
                        BolillaUi(numero = num, isSelected = true)
                    }
                }
                return@Scaffold
            }

            Text("Elige 6 números distintos (1 al 45):", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Seleccionados: ${numerosSeleccionados.size} / 6", fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(16.dp))

            LazyVerticalGrid(
                columns = GridCells.Adaptive(48.dp),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items((1..45).toList()) { numero ->
                    val isSelected = numerosSeleccionados.contains(numero)
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable {
                                if (isSelected) {
                                    numerosSeleccionados.remove(numero)
                                } else if (numerosSeleccionados.size < 6) {
                                    numerosSeleccionados.add(numero)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = numero.toString(),
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (uiState is UiState.Error) {
                Text((uiState as UiState.Error).message, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(8.dp))
            }

            Button(
                onClick = { sorteoViewModel.registrarJugada(sorteoId, numerosSeleccionados) },
                enabled = numerosSeleccionados.size == 6 && uiState !is UiState.Loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState is UiState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Text("Confirmar Jugada")
                }
            }
        }
    }
}

@Composable
fun BolillaUi(numero: Int, isSelected: Boolean = false, isHit: Boolean = false) {
    val bgColor = when {
        isHit -> Color(0xFF4CAF50) // Verde para acierto
        isSelected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = if (isHit || isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = numero.toString(),
            color = textColor,
            fontWeight = FontWeight.Bold
        )
    }
}
