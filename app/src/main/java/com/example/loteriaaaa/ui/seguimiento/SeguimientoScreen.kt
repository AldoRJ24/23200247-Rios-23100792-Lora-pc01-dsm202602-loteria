package com.example.loteriaaaa.ui.seguimiento

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeguimientoScreen(
    viewModel: SeguimientoViewModel,
    sorteoId: String,
    userId: String,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    // Manejo de suscripción y limpieza con DisposableEffect
    DisposableEffect(sorteoId, userId) {
        viewModel.iniciarSeguimiento(sorteoId, userId)
        onDispose {
            viewModel.detenerSeguimiento()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Seguimiento en Tiempo Real") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Text("←", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Sorteo Info Header
            val sorteo = state.sorteo
            if (sorteo != null) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = sorteo.nombre.ifBlank { "Sorteo Oficial" },
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )

                            val isAbierto = sorteo.estado.lowercase() == "abierto"
                            val isEnCurso = sorteo.estado.lowercase() == "en curso"

                            AssistChip(
                                onClick = {},
                                label = { Text(sorteo.estado.uppercase(), fontWeight = FontWeight.Bold) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = when {
                                        isAbierto -> Color(0xFFE8F5E9)
                                        isEnCurso -> Color(0xFFFFF3E0)
                                        else -> Color(0xFFFFEBEE)
                                    },
                                    labelColor = when {
                                        isAbierto -> Color(0xFF2E7D32)
                                        isEnCurso -> Color(0xFFE65100)
                                        else -> Color(0xFFC62828)
                                    }
                                )
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Fecha: ${sorteo.fecha}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Banner con el Mensaje de Estado (Reglas de negocio locales)
            val estadoColor = when (state.mensajeEstado) {
                "GANADOR" -> Color(0xFF2E7D32)
                "SIGUE INTENTANDO" -> Color(0xFFD84315)
                "Sorteo en curso" -> Color(0xFFE65100)
                else -> MaterialTheme.colorScheme.primary
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = estadoColor.copy(alpha = 0.15f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, estadoColor, RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "ESTADO DEL SORTEO",
                        style = MaterialTheme.typography.labelMedium,
                        color = estadoColor,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = state.mensajeEstado,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = estadoColor,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Sección: Bolillas Publicadas (0 a 6)
            val bolillasCount = state.bolillasPublicadas.size
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Bolillas Publicadas",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = "$bolillasCount / 6",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Visualización de las 6 Bolillas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                (1..6).forEach { index ->
                    val bolilla = state.bolillasPublicadas.find { it.orden == index }
                    val numeroVal = bolilla?.numero
                    val esAcertado = numeroVal != null && state.numerosAcertados.contains(numeroVal)

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "#$index",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    color = when {
                                        esAcertado -> Color(0xFF4CAF50) // Verde si es acertado
                                        numeroVal != null -> MaterialTheme.colorScheme.primaryContainer
                                        else -> MaterialTheme.colorScheme.surface
                                    }
                                )
                                .border(
                                    width = if (esAcertado || numeroVal != null) 2.dp else 1.dp,
                                    color = when {
                                        esAcertado -> Color(0xFF2E7D32)
                                        numeroVal != null -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.outline
                                    },
                                    shape = CircleShape
                                )
                        ) {
                            Text(
                                text = numeroVal?.toString() ?: "-",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    esAcertado -> Color.White
                                    numeroVal != null -> MaterialTheme.colorScheme.onPrimaryContainer
                                    else -> MaterialTheme.colorScheme.outline
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Sección: Jugada del Usuario y Resaltado de Aciertos
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Tu Jugada Registrada",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )

                        Text(
                            text = "Aciertos: ${state.aciertosCount} / 6",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = if (state.aciertosCount > 0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurface
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val jugada = state.jugadaUsuario
                    if (jugada != null && jugada.numeros.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            jugada.numeros.sorted().forEach { num ->
                                val esAcertado = state.numerosAcertados.contains(num)

                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(
                                            color = if (esAcertado) Color(0xFF4CAF50) else MaterialTheme.colorScheme.secondaryContainer
                                        )
                                        .border(
                                            width = if (esAcertado) 2.dp else 1.dp,
                                            color = if (esAcertado) Color(0xFF1B5E20) else MaterialTheme.colorScheme.outline,
                                            shape = CircleShape
                                        )
                                ) {
                                    Text(
                                        text = num.toString(),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (esAcertado) Color.White else MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "No has registrado ninguna jugada para este sorteo.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                        )
                    }
                }
            }

            // Banner Final si el sorteo finalizó y se recibieron las 6 bolillas
            AnimatedVisibility(
                visible = sorteo?.estado?.lowercase() == "finalizado" && state.bolillasPublicadas.size >= 6,
                enter = fadeIn() + slideInVertically()
            ) {
                Spacer(modifier = Modifier.height(20.dp))
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (state.esGanador) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (state.esGanador) "¡FELICIDADES! ¡ACERTASTE LOS 6 NÚMEROS!" else "¡Sigue participando en el próximo sorteo!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (state.esGanador) Color(0xFF1B5E20) else Color(0xFFE65100),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
