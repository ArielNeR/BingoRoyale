package com.bingoroyale.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import com.bingoroyale.network.ConnectionState
import com.bingoroyale.ui.components.*
import com.bingoroyale.ui.theme.*
import com.bingoroyale.viewmodel.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel = viewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val discoveredServers by viewModel.discoveredServers.collectAsState()

    var showConnectionDialog by remember { mutableStateOf(false) }
    var showNewCardDialog by remember { mutableStateOf(false) }
    var showModeDialog by remember { mutableStateOf(false) }
    var manualIp by remember { mutableStateOf("") }

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
                            text = "JUGADOR",
                            fontFamily = Orbitron,
                            fontWeight = FontWeight.Bold
                        )
                        ConnectionIndicator(
                            isConnected = connectionState is ConnectionState.Connected,
                            serverMode = false
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { showNewCardDialog = true }) {
                        Icon(Icons.Default.Refresh, "Nuevo cartón")
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
                // Estado de conexión y botón conectar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GlassCard(
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = when (connectionState) {
                                        is ConnectionState.Connected -> "Conectado"
                                        is ConnectionState.Connecting -> "Conectando..."
                                        is ConnectionState.Scanning -> "Buscando..."
                                        else -> "Sin conexión"
                                    },
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (connectionState is ConnectionState.Connected) {
                                        (connectionState as ConnectionState.Connected).server.name
                                    } else "Modo offline",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted
                                )
                            }

                            Button(
                                onClick = {
                                    if (connectionState is ConnectionState.Connected) {
                                        viewModel.disconnect()
                                    } else {
                                        showConnectionDialog = true
                                        viewModel.discoverServers()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (connectionState is ConnectionState.Connected)
                                        ErrorRed else GoldPrimary
                                )
                            ) {
                                Text(
                                    if (connectionState is ConnectionState.Connected)
                                        "Desconectar" else "Conectar"
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Última bola recibida
                if (uiState.receivedBalls.isNotEmpty()) {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "ÚLTIMA BOLA",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            BingoBall(
                                number = uiState.receivedBalls.last(),
                                mode = uiState.mode,
                                size = BallSize.MEDIUM,
                                animated = true
                            )

                            if (uiState.receivedBalls.size > 1) {
                                Spacer(modifier = Modifier.height(8.dp))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    items(uiState.receivedBalls.takeLast(10).reversed().drop(1)) { ball ->
                                        BingoBall(
                                            number = ball,
                                            mode = uiState.mode,
                                            size = BallSize.SMALL
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Cartón de Bingo
                GlassCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TU CARTÓN",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = GoldPrimary
                            )
                            TextButton(onClick = { showModeDialog = true }) {
                                Text("Modo ${uiState.mode}")
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Header BINGO (solo para modo 75)
                        if (uiState.mode == 75) {
                            BingoHeader()
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        // Grid del cartón
                        BingoCardGrid(
                            card = uiState.card,
                            markedCells = uiState.markedCells,
                            receivedBalls = uiState.receivedBalls,
                            mode = uiState.mode,
                            onCellClick = { index -> viewModel.toggleMark(index) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Logros
                if (uiState.hasLine || uiState.hasBingo) {
                    GlassCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (uiState.hasLine) {
                                AchievementBadge(
                                    emoji = "➖",
                                    text = "LÍNEA"
                                )
                            }
                            if (uiState.hasBingo) {
                                if (uiState.hasLine) Spacer(modifier = Modifier.width(12.dp))
                                AchievementBadge(
                                    emoji = "🏆",
                                    text = "¡BINGO!"
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Botón BINGO
                GradientButton(
                    text = "¡BINGO!",
                    onClick = { viewModel.claimBingo() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    gradient = Brush.horizontalGradient(
                        colors = listOf(GoldDark, GoldPrimary)
                    ),
                    icon = { Text("🏆", fontSize = 24.sp) }
                )
            }
        }
    }

    // Diálogo de conexión
    if (showConnectionDialog) {
        AlertDialog(
            onDismissRequest = { showConnectionDialog = false },
            title = { Text("Conectar a cantador") },
            text = {
                Column {
                    Text(
                        text = "Servidores encontrados:",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (connectionState is ConnectionState.Scanning) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    } else if (discoveredServers.isEmpty()) {
                        Text(
                            text = "No se encontraron servidores",
                            color = TextMuted
                        )
                    } else {
                        discoveredServers.forEach { server ->
                            Card(
                                onClick = {
                                    viewModel.connectToServer(server)
                                    showConnectionDialog = false
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("📡", fontSize = 24.sp)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(server.name, fontWeight = FontWeight.Bold)
                                        Text(server.address, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("O ingresa IP manualmente:")
                    OutlinedTextField(
                        value = manualIp,
                        onValueChange = { manualIp = it },
                        label = { Text("IP del servidor") },
                        placeholder = { Text("192.168.1.100") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (manualIp.isNotBlank()) {
                            viewModel.connectByIp(manualIp)
                            showConnectionDialog = false
                        }
                    }
                ) {
                    Text("Conectar")
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { viewModel.discoverServers() }) {
                        Text("Buscar")
                    }
                    TextButton(onClick = { showConnectionDialog = false }) {
                        Text("Cerrar")
                    }
                }
            }
        )
    }

    // Diálogo nuevo cartón
    if (showNewCardDialog) {
        AlertDialog(
            onDismissRequest = { showNewCardDialog = false },
            title = { Text("Nuevo cartón") },
            text = { Text("¿Generar un nuevo cartón? Se perderán las marcas actuales.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.generateNewCard()
                        showNewCardDialog = false
                    }
                ) {
                    Text("Generar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewCardDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Diálogo cambiar modo
    if (showModeDialog) {
        AlertDialog(
            onDismissRequest = { showModeDialog = false },
            title = { Text("Modo de juego") },
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
                            Text(if (mode == 75) "USA 75 (5x5)" else "EU 90 (3x9)")
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
fun BingoCardGrid(
    card: List<List<Any?>>,
    markedCells: Set<Int>,
    receivedBalls: List<Int>,
    mode: Int,
    onCellClick: (Int) -> Unit
) {
    val columns = if (mode == 75) 5 else 9
    val rows = if (mode == 75) 5 else 3

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        for (row in 0 until rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (col in 0 until columns) {
                    val index = if (mode == 75) row * 5 + col else row * 9 + col
                    val value = if (mode == 75) {
                        card.getOrNull(col)?.getOrNull(row)
                    } else {
                        card.getOrNull(row)?.getOrNull(col)
                    }

                    val isFreeSpace = mode == 75 && row == 2 && col == 2
                    val cellValue = when {
                        isFreeSpace -> "FREE"
                        value == null -> ""
                        else -> value.toString()
                    }
                    val numValue = cellValue.toIntOrNull()
                    val canMark = numValue != null && receivedBalls.contains(numValue)

                    Box(modifier = Modifier.weight(1f)) {
                        BingoCell(
                            value = cellValue,
                            isMarked = markedCells.contains(index),
                            isFreeSpace = isFreeSpace,
                            canMark = canMark,
                            onClick = { onCellClick(index) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AchievementBadge(
    emoji: String,
    text: String
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        GoldPrimary.copy(alpha = 0.3f),
                        OrangePrimary.copy(alpha = 0.3f)
                    )
                )
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji, fontSize = 20.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontWeight = FontWeight.Bold,
            color = GoldPrimary
        )
    }
}