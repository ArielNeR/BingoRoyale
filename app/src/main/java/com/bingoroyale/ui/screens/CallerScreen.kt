package com.bingoroyale.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bingoroyale.network.ServerState
import com.bingoroyale.ui.components.*
import com.bingoroyale.ui.theme.*
import com.bingoroyale.viewmodel.CallerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallerScreen(
    viewModel: CallerViewModel = viewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val serverState by viewModel.serverState.collectAsState()
    val connectedClients by viewModel.connectedClients.collectAsState()

    var showResetDialog by remember { mutableStateOf(false) }
    var showModeDialog by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(BackgroundPrimary, BackgroundPurple, BackgroundPrimary)
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Bar
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "CANTADOR",
                            fontFamily = Orbitron,
                            fontWeight = FontWeight.Bold
                        )
                        ConnectionIndicator(
                            isConnected = serverState is ServerState.Running,
                            clientCount = connectedClients,
                            serverMode = true
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { showResetDialog = true }) {
                        Icon(Icons.Default.Refresh, "Reiniciar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Estado del servidor
                ServerStatusCard(
                    serverState = serverState,
                    onStart = { viewModel.startServer() },
                    onStop = { viewModel.stopServer() }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Modo de juego
                GlassCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Modo: ${uiState.mode} bolas",
                            style = MaterialTheme.typography.titleMedium,
                            color = GoldPrimary
                        )
                        TextButton(onClick = { showModeDialog = true }) {
                            Text("Cambiar")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Bola actual
                CurrentBallDisplay(
                    currentBall = uiState.currentBall,
                    mode = uiState.mode,
                    isAnimating = uiState.isAnimating
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Contador
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    StatBox(
                        label = "BOLAS",
                        value = "${uiState.drawnBalls.size}/${uiState.mode}"
                    )
                    StatBox(
                        label = "RESTANTES",
                        value = "${uiState.mode - uiState.drawnBalls.size}"
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Botón extraer
                GradientButton(
                    text = "EXTRAER NÚMERO",
                    onClick = { viewModel.drawBall() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp),
                    enabled = !uiState.isGameOver && uiState.drawnBalls.size < uiState.mode,
                    icon = { Text("🎱", fontSize = 24.sp) }
                )

                if (uiState.isGameOver) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "¡Todas las bolas han sido extraídas!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = GoldPrimary,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Historial de bolas
                if (uiState.drawnBalls.isNotEmpty()) {
                    BallHistory(
                        balls = uiState.drawnBalls,
                        mode = uiState.mode
                    )
                }
            }
        }
    }

    // Diálogos
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reiniciar partida") },
            text = { Text("¿Estás seguro de reiniciar? Se perderán todas las bolas extraídas.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.resetGame()
                        showResetDialog = false
                    }
                ) {
                    Text("Reiniciar", color = RedPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showModeDialog) {
        AlertDialog(
            onDismissRequest = { showModeDialog = false },
            title = { Text("Seleccionar modo") },
            text = {
                Column {
                    listOf(75, 90).forEach { mode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = uiState.mode == mode,
                                onClick = {
                                    viewModel.setMode(mode)
                                    showModeDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (mode == 75) "USA 75 (B-I-N-G-O)" else "EU 90 (Tradicional)"
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showModeDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }
}

@Composable
fun ServerStatusCard(
    serverState: ServerState,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Red Local",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                when (serverState) {
                    is ServerState.Running -> {
                        Text(
                            text = "IP: ${serverState.ip}",
                            style = MaterialTheme.typography.bodySmall,
                            color = SuccessGreen
                        )
                    }
                    is ServerState.Error -> {
                        Text(
                            text = serverState.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = ErrorRed
                        )
                    }
                    else -> {
                        Text(
                            text = "Servidor detenido",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }
            }

            Button(
                onClick = if (serverState is ServerState.Running) onStop else onStart,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (serverState is ServerState.Running) ErrorRed else SuccessGreen
                )
            ) {
                Text(if (serverState is ServerState.Running) "Detener" else "Iniciar")
            }
        }
    }
}

@Composable
fun CurrentBallDisplay(
    currentBall: Int?,
    mode: Int,
    isAnimating: Boolean
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "BOLA ACTUAL",
                style = MaterialTheme.typography.labelMedium,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier.height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                if (currentBall != null) {
                    BingoBall(
                        number = currentBall,
                        mode = mode,
                        size = BallSize.LARGE,
                        animated = isAnimating
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(BackgroundSecondary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "?",
                            fontSize = 40.sp,
                            color = TextMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatBox(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontFamily = Orbitron,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = GoldPrimary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted
        )
    }
}

@Composable
fun BallHistory(
    balls: List<Int>,
    mode: Int
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Text(
                text = "🎱 Historial de Bolas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            val listState = rememberLazyListState()

            LaunchedEffect(balls.size) {
                if (balls.isNotEmpty()) {
                    listState.animateScrollToItem(balls.size - 1)
                }
            }

            LazyRow(
                state = listState,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                items(balls.reversed()) { ball ->
                    BingoBall(
                        number = ball,
                        mode = mode,
                        size = BallSize.SMALL
                    )
                }
            }
        }
    }
}