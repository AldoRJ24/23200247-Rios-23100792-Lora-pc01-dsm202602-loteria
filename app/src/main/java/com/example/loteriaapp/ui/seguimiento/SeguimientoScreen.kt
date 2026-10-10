package com.example.loteriaapp.ui.seguimiento

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SeguimientoScreen(
    viewModel: SeguimientoViewModel,
    sorteoId: String,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    // Iniciar el listener de Firestore al cargar el composable
    LaunchedEffect(sorteoId) {
        viewModel.iniciarSeguimiento(sorteoId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Seguimiento en Tiempo Real") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
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
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Conectando con Firestore en tiempo real...")
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Tarjeta de Información del Sorteo
                uiState.sorteo?.let { sorteo ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = sorteo.nombre,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "ID: ${sorteo.id} | Fecha: ${sorteo.fecha}",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                EstadoSorteoBadge(estado = sorteo.estado)
                            }
                        }
                    }
                }

                // 🌟 Mensaje de Estado Prominente (Banner de Negocio)
                BannerMensajeEstado(
                    mensaje = uiState.mensajeEstado,
                    estado = uiState.sorteo?.estado ?: "abierto",
                    aciertos = uiState.aciertosCount,
                    bolillasPublicadas = uiState.cantidadBolillasPublicadas
                )

                // 🎯 Sección Jugada del Usuario con Resaltado Visual de Aciertos
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Tu Jugada Registrada",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                color = if (uiState.aciertosCount > 0) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "Aciertos: ${uiState.aciertosCount} / 6",
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = if (uiState.aciertosCount > 0) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        val userNumeros = uiState.jugada?.numeros ?: emptyList()
                        if (userNumeros.isEmpty()) {
                            Text(
                                text = "Aún no has registrado una jugada para este sorteo.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                            ) {
                                userNumeros.forEach { numero ->
                                    val isHit = uiState.numerosAcertados.contains(numero)
                                    BolillaJugadaUsuario(numero = numero, isHit = isHit)
                                }
                            }
                        }
                    }
                }

                // 🎱 Sección de Bolillas Publicadas (0 a 6 en Tiempo Real)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Bolillas Publicadas",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${uiState.cantidadBolillasPublicadas} / 6",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                        ) {
                            val bolillas = uiState.bolillas
                            for (i in 1..6) {
                                val bolilla = bolillas.firstOrNull { it.orden == i }
                                BolillaPublicadaItem(
                                    orden = i,
                                    numero = bolilla?.numero
                                )
                            }
                        }
                    }
                }

                // 🧪 Panel de Control de Simulación (Útil para Pruebas en Vivo)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "🛠️ Panel de Simulación en Vivo (Firestore)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "Permite simular la publicación de bolillas y cambios de estado en tiempo real para verificar la aplicación.",
                            style = MaterialTheme.typography.bodySmall
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.cambiarEstadoSorteo("abierto") },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(4.dp)
                            ) {
                                Text("Abrir", style = MaterialTheme.typography.labelSmall)
                            }
                            Button(
                                onClick = { viewModel.cambiarEstadoSorteo("en_curso") },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(4.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                            ) {
                                Text("Iniciar", style = MaterialTheme.typography.labelSmall)
                            }
                            Button(
                                onClick = { viewModel.cambiarEstadoSorteo("finalizado") },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(4.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Finalizar", style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.simularSiguienteBolilla() },
                                enabled = uiState.cantidadBolillasPublicadas < 6,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("+ Publicar Bolilla")
                            }

                            OutlinedButton(
                                onClick = { viewModel.reiniciarSimulacion() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Reiniciar")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BannerMensajeEstado(
    mensaje: String,
    estado: String,
    aciertos: Int,
    bolillasPublicadas: Int
) {
    val (bgColor, textColor, icon) = when {
        estado == "finalizado" && bolillasPublicadas == 6 && aciertos == 6 -> Triple(
            Color(0xFF2E7D32),
            Color.White,
            Icons.Default.Star
        )
        estado == "finalizado" && bolillasPublicadas == 6 -> Triple(
            Color(0xFFC62828),
            Color.White,
            null
        )
        estado == "en_curso" -> Triple(
            Color(0xFFEF6C00),
            Color.White,
            Icons.Default.PlayArrow
        )
        else -> Triple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            null
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
            }
            Text(
                text = mensaje,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = textColor,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun BolillaJugadaUsuario(numero: Int, isHit: Boolean) {
    val scale by animateFloatAsState(targetValue = if (isHit) 1.15f else 1.0f, label = "scale")

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .scale(scale)
            .size(46.dp)
            .clip(CircleShape)
            .background(
                if (isHit) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primaryContainer
            )
            .border(
                width = if (isHit) 2.dp else 1.dp,
                color = if (isHit) Color(0xFFFFD700) else MaterialTheme.colorScheme.outline,
                shape = CircleShape
            )
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = numero.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isHit) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
            )
            if (isHit) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Acierto",
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(10.dp)
                )
            }
        }
    }
}

@Composable
fun BolillaPublicadaItem(orden: Int, numero: Int?) {
    val isPublished = numero != null

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "#$orden",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(
                    if (isPublished) Color(0xFF1565C0) else MaterialTheme.colorScheme.surfaceVariant
                )
                .border(
                    width = 1.dp,
                    color = if (isPublished) Color(0xFF90CAF9) else MaterialTheme.colorScheme.outline,
                    shape = CircleShape
                )
        ) {
            Text(
                text = numero?.toString() ?: "-",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isPublished) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun EstadoSorteoBadge(estado: String) {
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
