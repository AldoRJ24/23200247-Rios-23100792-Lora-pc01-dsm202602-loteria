package com.example.loteriaapp.ui.jugada

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistroJugadaScreen(
    viewModel: RegistroJugadaViewModel,
    userEmail: String?,
    onLogout: () -> Unit,
    onNavigateToSeguimiento: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var tempSorteoId by remember { mutableStateOf(uiState.sorteoIdInput) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Registro de Jugada") },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Cerrar sesión"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Cabecera del usuario
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Usuario conectado:",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = userEmail ?: "Usuario Desconocido",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Búsqueda / Selección de Sorteo
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Identificador del Sorteo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = tempSorteoId,
                            onValueChange = { tempSorteoId = it },
                            label = { Text("ID del Sorteo (ej. SORTEO-001)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = { viewModel.onSorteoIdChanged(tempSorteoId) },
                            contentPadding = PaddingValues(12.dp)
                        ) {
                            Icon(Icons.Default.Search, contentDescription = "Buscar")
                        }
                    }
                }
            }

            // Información del Sorteo Actual
            uiState.sorteo?.let { sorteo ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = when (sorteo.estado) {
                            "abierto" -> MaterialTheme.colorScheme.primaryContainer
                            "en_curso" -> MaterialTheme.colorScheme.tertiaryContainer
                            else -> MaterialTheme.colorScheme.secondaryContainer
                        }
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = sorteo.nombre,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            EstadoBadge(estado = sorteo.estado)
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Fecha: ${sorteo.fecha}",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        if (sorteo.estado != "abierto") {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "⚠️ El sorteo no está 'abierto' (Estado: '${sorteo.estado}'). No se pueden guardar o modificar jugadas.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            if (uiState.isJugadaExistente) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "Ya tienes una jugada registrada para este sorteo. Puedes verla o modificarla si el sorteo continúa abierto.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Indicador de números seleccionados
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Selecciona 6 números (1 - 45)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${uiState.selectedNumeros.size} / 6",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (uiState.selectedNumeros.size == 6) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Muestra los números seleccionados en orden
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                    ) {
                        val sortedNumbers = uiState.selectedNumeros.sorted()
                        for (i in 0 until 6) {
                            val num = sortedNumbers.getOrNull(i)
                            NumeroBolaItem(numero = num, isSelected = num != null)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { viewModel.limpiarSeleccion() },
                            enabled = uiState.selectedNumeros.isNotEmpty()
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Limpiar selección")
                        }
                    }
                }
            }

            // Grilla de números del 1 al 45
            Text(
                text = "Grilla de Números (1 a 45):",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(7),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items((1..45).toList()) { numero ->
                        val isSelected = uiState.selectedNumeros.contains(numero)
                        val isAbierto = uiState.sorteo?.estado == "abierto"

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isSelected -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.surfaceVariant
                                    }
                                )
                                .clickable(enabled = isAbierto || isSelected) {
                                    viewModel.toggleNumero(numero)
                                }
                        ) {
                            Text(
                                text = numero.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Mensajes de resultado/error
            uiState.message?.let { msg ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (uiState.isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = msg,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (uiState.isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Botones de acción
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val isAbierto = uiState.sorteo?.estado == "abierto"
                val canGuardar = isAbierto && uiState.selectedNumeros.size == 6 && !uiState.isSaving

                Button(
                    onClick = { viewModel.guardarJugada() },
                    enabled = canGuardar,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Guardar Jugada (6 Números)")
                    }
                }

                OutlinedButton(
                    onClick = {
                        val currentSorteoId = uiState.sorteo?.id ?: tempSorteoId
                        onNavigateToSeguimiento(currentSorteoId)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text("Ir a Seguimiento en Tiempo Real")
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                }
            }
        }
    }
}

@Composable
fun NumeroBolaItem(numero: Int?, isSelected: Boolean, isHit: Boolean = false) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(
                when {
                    isHit -> Color(0xFF2E7D32) // Verde resaltado para aciertos
                    isSelected -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            )
    ) {
        Text(
            text = numero?.toString() ?: "-",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = when {
                isHit || isSelected -> Color.White
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

@Composable
fun EstadoBadge(estado: String) {
    val (bgColor, textColor, label) = when (estado) {
        "abierto" -> Triple(Color(0xFFE8F5E9), Color(0xFF2E7D32), "ABIERTO")
        "en_curso" -> Triple(Color(0xFFFFF3E0), Color(0xFFE65100), "EN CURSO")
        "finalizado" -> Triple(Color(0xFFFFEBEE), Color(0xFFC62828), "FINALIZADO")
        else -> Triple(Color.LightGray, Color.Black, estado.uppercase())
    }

    Surface(
        color = bgColor,
        shape = CircleShape
    ) {
        Text(
            text = label,
            color = textColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}
