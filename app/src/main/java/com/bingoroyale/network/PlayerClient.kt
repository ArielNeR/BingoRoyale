package com.bingoroyale.network

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.*

data class ServerInfo(
    val name: String,
    val address: String,
    val port: Int,
    val mode: Int = 75
)

sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Scanning : ConnectionState()
    object Connecting : ConnectionState()
    data class Connected(val serverName: String) : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}

class PlayerClient {

    companion object {
        private const val TAG = "PlayerClient"
        private const val DISCOVERY_PORT = 8889
        private const val DISCOVERY_TIMEOUT = 3000
        private const val CONNECT_TIMEOUT = 5000
    }

    private var socket: Socket? = null
    private var writer: PrintWriter? = null
    private var reader: BufferedReader? = null
    @Volatile private var isConnected = false

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var listenerJob: Job? = null

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _incomingBalls = MutableSharedFlow<Int>(
        replay = 0,
        extraBufferCapacity = 100
    )
    val incomingBalls: SharedFlow<Int> = _incomingBalls.asSharedFlow()

    private val _gameEvents = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 20
    )
    val gameEvents: SharedFlow<String> = _gameEvents.asSharedFlow()

    private val _serverMode = MutableStateFlow(75)
    val serverMode: StateFlow<Int> = _serverMode.asStateFlow()

    private val _bingoNotification = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 10
    )
    val bingoNotification: SharedFlow<String> = _bingoNotification.asSharedFlow()

    fun discoverAndConnect() {
        scope.launch {
            _connectionState.value = ConnectionState.Scanning
            Log.d(TAG, "Starting discovery...")

            try {
                val server = discover()

                if (server != null) {
                    Log.d(TAG, "Found server: ${server.name} at ${server.address}")
                    connect(server)
                } else {
                    Log.d(TAG, "No server found")
                    _connectionState.value = ConnectionState.Error("No se encontró cantador")
                    delay(2000)
                    _connectionState.value = ConnectionState.Disconnected
                }
            } catch (e: Exception) {
                Log.e(TAG, "Discovery error", e)
                _connectionState.value = ConnectionState.Error("Error: ${e.message}")
                delay(2000)
                _connectionState.value = ConnectionState.Disconnected
            }
        }
    }

    private suspend fun discover(): ServerInfo? = withContext(Dispatchers.IO) {
        var udpSocket: DatagramSocket? = null
        try {
            udpSocket = DatagramSocket().apply {
                soTimeout = DISCOVERY_TIMEOUT
                broadcast = true
            }

            val message = "BINGO_DISCOVER"
            val broadcastAddr = InetAddress.getByName("255.255.255.255")
            udpSocket.send(
                DatagramPacket(message.toByteArray(), message.length, broadcastAddr, DISCOVERY_PORT)
            )

            val buffer = ByteArray(256)
            val receivePacket = DatagramPacket(buffer, buffer.size)
            udpSocket.receive(receivePacket)

            val response = String(receivePacket.data, 0, receivePacket.length)
            Log.d(TAG, "Received: $response")

            if (response.startsWith("BINGO_SERVER:")) {
                val parts = response.split(":")
                if (parts.size >= 3) {
                    ServerInfo(
                        name = parts[1],
                        address = receivePacket.address.hostAddress ?: "",
                        port = parts[2].toIntOrNull() ?: 8888,
                        mode = if (parts.size > 3) parts[3].toIntOrNull() ?: 75 else 75
                    )
                } else null
            } else null
        } catch (e: SocketTimeoutException) {
            Log.d(TAG, "Discovery timeout")
            null
        } catch (e: Exception) {
            Log.e(TAG, "Discovery error", e)
            null
        } finally {
            try { udpSocket?.close() } catch (e: Exception) { }
        }
    }

    private suspend fun connect(server: ServerInfo) = withContext(Dispatchers.IO) {
        _connectionState.value = ConnectionState.Connecting

        try {
            Log.d(TAG, "Connecting to ${server.address}:${server.port}")

            socket = Socket().apply {
                soTimeout = 0
                keepAlive = true
                tcpNoDelay = true
                connect(InetSocketAddress(server.address, server.port), CONNECT_TIMEOUT)
            }

            writer = PrintWriter(socket!!.getOutputStream(), true)
            reader = BufferedReader(InputStreamReader(socket!!.getInputStream()))

            isConnected = true
            _serverMode.value = server.mode
            _connectionState.value = ConnectionState.Connected(server.name)

            Log.d(TAG, "Connected!")
            startListening()

        } catch (e: Exception) {
            Log.e(TAG, "Connection failed", e)
            _connectionState.value = ConnectionState.Error("Error: ${e.message}")
            delay(2000)
            cleanupConnection()
        }
    }

    private fun startListening() {
        listenerJob?.cancel()
        listenerJob = scope.launch(Dispatchers.IO) {
            Log.d(TAG, "Listener started")

            try {
                while (isConnected && isActive) {
                    val line = try {
                        reader?.readLine()
                    } catch (e: Exception) {
                        if (isConnected) Log.e(TAG, "Read error", e)
                        null
                    }

                    if (line == null) {
                        Log.d(TAG, "Server closed connection")
                        break
                    }

                    try {
                        processMessage(line)
                    } catch (e: Exception) {
                        Log.e(TAG, "Process message error", e)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Listener error", e)
            } finally {
                if (isConnected) {
                    isConnected = false
                    _connectionState.value = ConnectionState.Disconnected
                    try { _gameEvents.emit("disconnected") } catch (e: Exception) { }
                    cleanupConnection()
                }
            }
        }
    }

    private suspend fun processMessage(message: String) {
        Log.d(TAG, "Processing: $message")

        try {
            val json = JSONObject(message)

            when (json.optString("type", "")) {
                "welcome" -> {
                    val mode = json.optInt("mode", 75)
                    _serverMode.value = mode
                    Log.d(TAG, "Welcome, mode: $mode")
                }
                "ball" -> {
                    val number = json.optInt("number", -1)
                    if (number > 0) {
                        Log.d(TAG, "Ball: $number")
                        _incomingBalls.emit(number)
                    }
                }
                "new_game" -> {
                    val mode = json.optInt("mode", 75)
                    _serverMode.value = mode
                    _gameEvents.emit("new_game")
                    Log.d(TAG, "New game, mode: $mode")
                }
                "server_closed" -> {
                    Log.d(TAG, "Server closed")
                    isConnected = false
                }
                "bingo_called" -> {
                    val playerName = json.optString("player", "Alguien")
                    _bingoNotification.emit(playerName)
                    Log.d(TAG, "Bingo by: $playerName")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "JSON parse error: $message", e)
        }
    }

    private fun cleanupConnection() {
        Log.d(TAG, "Cleanup")
        listenerJob?.cancel()
        listenerJob = null

        try { reader?.close() } catch (e: Exception) { }
        try { writer?.close() } catch (e: Exception) { }
        try { socket?.close() } catch (e: Exception) { }

        reader = null
        writer = null
        socket = null
        isConnected = false
    }

    fun sendBingo(playerName: String = "Jugador") {
        if (!isConnected) return

        scope.launch(Dispatchers.IO) {
            try {
                val message = JSONObject().apply {
                    put("type", "bingo")
                    put("player", playerName)
                }.toString()
                writer?.println(message)
                writer?.flush()
                Log.d(TAG, "Bingo sent")
            } catch (e: Exception) {
                Log.e(TAG, "Send bingo error", e)
            }
        }
    }

    fun disconnect() {
        Log.d(TAG, "Disconnect requested")
        isConnected = false
        cleanupConnection()
        _connectionState.value = ConnectionState.Disconnected
    }

    fun release() {
        Log.d(TAG, "Release")
        isConnected = false
        cleanupConnection()
        scope.cancel()
    }
}