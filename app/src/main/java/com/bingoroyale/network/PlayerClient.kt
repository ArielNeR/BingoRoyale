package com.bingoroyale.network

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.Socket
import java.io.BufferedReader
import java.io.InputStreamReader

class PlayerClient {

    companion object {
        private const val TAG = "PlayerClient"
        private const val DISCOVERY_PORT = 8889
        private const val DISCOVERY_TIMEOUT = 5000
        private const val BUFFER_SIZE = 1024
    }

    private var socket: Socket? = null
    private var isConnected = false
    private var listenerJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState

    private val _incomingBalls = MutableSharedFlow<Int>(replay = 0)
    val incomingBalls: SharedFlow<Int> = _incomingBalls

    private val _gameEvents = MutableSharedFlow<GameEvent>(replay = 0)
    val gameEvents: SharedFlow<GameEvent> = _gameEvents

    private val _discoveredServers = MutableStateFlow<List<ServerInfo>>(emptyList())
    val discoveredServers: StateFlow<List<ServerInfo>> = _discoveredServers

    // Descubrir servidores en la red local
    fun discoverServers() {
        scope.launch {
            _connectionState.value = ConnectionState.Scanning
            val servers = mutableListOf<ServerInfo>()

            try {
                val socket = DatagramSocket()
                socket.soTimeout = DISCOVERY_TIMEOUT
                socket.broadcast = true

                // Enviar broadcast de descubrimiento
                val message = "BINGO_DISCOVER"
                val broadcastAddress = InetAddress.getByName("255.255.255.255")
                val packet = DatagramPacket(
                    message.toByteArray(),
                    message.length,
                    broadcastAddress,
                    DISCOVERY_PORT
                )

                socket.send(packet)
                Log.d(TAG, "Discovery broadcast sent")

                // Escuchar respuestas
                val buffer = ByteArray(BUFFER_SIZE)
                val startTime = System.currentTimeMillis()

                while (System.currentTimeMillis() - startTime < DISCOVERY_TIMEOUT) {
                    try {
                        val responsePacket = DatagramPacket(buffer, buffer.size)
                        socket.receive(responsePacket)

                        val response = String(responsePacket.data, 0, responsePacket.length)
                        if (response.startsWith("BINGO_SERVER:")) {
                            val parts = response.split(":")
                            if (parts.size >= 3) {
                                val serverInfo = ServerInfo(
                                    name = parts[1],
                                    address = responsePacket.address.hostAddress ?: "",
                                    port = parts[2].toIntOrNull() ?: BingoServer.DEFAULT_PORT,
                                    mode = if (parts.size > 3) parts[3].toIntOrNull() ?: 75 else 75
                                )
                                if (!servers.any { it.address == serverInfo.address }) {
                                    servers.add(serverInfo)
                                    _discoveredServers.value = servers.toList()
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // Timeout o error, continuar
                    }
                }

                socket.close()

            } catch (e: Exception) {
                Log.e(TAG, "Discovery error: ${e.message}")
            }

            _discoveredServers.value = servers
            _connectionState.value = ConnectionState.Disconnected
        }
    }

    // Conectar a un servidor específico
    fun connectToServer(serverInfo: ServerInfo) {
        scope.launch {
            _connectionState.value = ConnectionState.Connecting

            try {
                socket = Socket(serverInfo.address, serverInfo.port)
                socket?.soTimeout = 0 // Sin timeout para escucha continua
                isConnected = true

                _connectionState.value = ConnectionState.Connected(serverInfo)
                Log.d(TAG, "Connected to ${serverInfo.name} at ${serverInfo.address}")

                // Iniciar escucha de mensajes
                startListening()

            } catch (e: Exception) {
                Log.e(TAG, "Connection error: ${e.message}")
                _connectionState.value = ConnectionState.Error("No se pudo conectar: ${e.message}")
                disconnect()
            }
        }
    }

    // Conectar por IP directa
    fun connectByIp(ip: String, port: Int = BingoServer.DEFAULT_PORT) {
        val serverInfo = ServerInfo(
            name = "Servidor",
            address = ip,
            port = port,
            mode = 75
        )
        connectToServer(serverInfo)
    }

    private fun startListening() {
        listenerJob = scope.launch {
            try {
                val reader = BufferedReader(InputStreamReader(socket?.getInputStream()))

                while (isConnected && isActive) {
                    val line = reader.readLine()
                    if (line != null) {
                        processMessage(line)
                    } else {
                        // Conexión cerrada
                        break
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Listening error: ${e.message}")
            }

            if (isConnected) {
                isConnected = false
                _connectionState.value = ConnectionState.Disconnected
                _gameEvents.emit(GameEvent.Disconnected)
            }
        }
    }

    private suspend fun processMessage(message: String) {
        try {
            val json = JSONObject(message)
            val type = json.optString("type", "")

            when (type) {
                "ball" -> {
                    val number = json.getInt("number")
                    _incomingBalls.emit(number)
                    Log.d(TAG, "Received ball: $number")
                }
                "game_start" -> {
                    val mode = json.optInt("mode", 75)
                    _gameEvents.emit(GameEvent.GameStarted(mode))
                }
                "game_reset" -> {
                    _gameEvents.emit(GameEvent.GameReset)
                }
                "game_end" -> {
                    _gameEvents.emit(GameEvent.GameEnded)
                }
                "ping" -> {
                    // Responder al ping para mantener conexión
                    sendPong()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing message: ${e.message}")
        }
    }

    private fun sendPong() {
        scope.launch {
            try {
                val pong = JSONObject().apply {
                    put("type", "pong")
                }
                socket?.getOutputStream()?.write("${pong}\n".toByteArray())
                socket?.getOutputStream()?.flush()
            } catch (e: Exception) {
                Log.e(TAG, "Error sending pong: ${e.message}")
            }
        }
    }

    fun disconnect() {
        isConnected = false
        listenerJob?.cancel()
        listenerJob = null

        try {
            socket?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing socket: ${e.message}")
        }

        socket = null
        _connectionState.value = ConnectionState.Disconnected
    }

    fun release() {
        disconnect()
        scope.cancel()
    }
}

// Estados de conexión
sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Scanning : ConnectionState()
    object Connecting : ConnectionState()
    data class Connected(val server: ServerInfo) : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

// Eventos del juego
sealed class GameEvent {
    data class GameStarted(val mode: Int) : GameEvent()
    object GameReset : GameEvent()
    object GameEnded : GameEvent()
    object Disconnected : GameEvent()
}

// Información del servidor
data class ServerInfo(
    val name: String,
    val address: String,
    val port: Int,
    val mode: Int = 75
)