package com.bingoroyale.network

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.*
import java.util.concurrent.CopyOnWriteArrayList

class BingoServer(private val context: Context) {

    companion object {
        private const val TAG = "BingoServer"
        const val PORT = 8888
        const val DISCOVERY_PORT = 8889
    }

    private var serverSocket: ServerSocket? = null
    private var discoverySocket: DatagramSocket? = null
    @Volatile private var isRunning = false

    private val clients = CopyOnWriteArrayList<ClientHandler>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _connectedClients = MutableStateFlow(0)
    val connectedClients: StateFlow<Int> = _connectedClients.asStateFlow()

    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

    private val _bingoEvents = MutableSharedFlow<String>(extraBufferCapacity = 10)
    val bingoEvents: SharedFlow<String> = _bingoEvents.asSharedFlow()

    private var currentMode = 75

    fun start(mode: Int = 75) {
        if (isRunning) return
        currentMode = mode
        isRunning = true
        _isActive.value = true

        // Servidor principal
        scope.launch {
            try {
                serverSocket = ServerSocket(PORT).apply {
                    reuseAddress = true
                }
                Log.d(TAG, "Server started on port $PORT")

                while (isRunning) {
                    try {
                        val socket = serverSocket?.accept()
                        if (socket != null && isRunning) {
                            Log.d(TAG, "Client connecting from ${socket.inetAddress}")
                            val handler = ClientHandler(socket)
                            clients.add(handler)
                            updateClientCount()
                            handler.start()
                        }
                    } catch (e: Exception) {
                        if (isRunning) Log.e(TAG, "Accept error", e)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Server error", e)
            }
        }

        // Discovery UDP
        scope.launch {
            try {
                discoverySocket = DatagramSocket(null).apply {
                    reuseAddress = true
                    bind(InetSocketAddress(DISCOVERY_PORT))
                    broadcast = true
                }

                val buffer = ByteArray(256)
                Log.d(TAG, "Discovery listening on port $DISCOVERY_PORT")

                while (isRunning) {
                    try {
                        val packet = DatagramPacket(buffer, buffer.size)
                        discoverySocket?.receive(packet)
                        val message = String(packet.data, 0, packet.length)

                        if (message == "BINGO_DISCOVER") {
                            val response = "BINGO_SERVER:BingoRoyale:$PORT:$currentMode"
                            val data = response.toByteArray()
                            val responsePacket = DatagramPacket(
                                data, data.size,
                                packet.address, packet.port
                            )
                            discoverySocket?.send(responsePacket)
                            Log.d(TAG, "Responded to discovery from ${packet.address}")
                        }
                    } catch (e: Exception) {
                        if (isRunning) Log.e(TAG, "Discovery error", e)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Discovery socket error", e)
            }
        }
    }

    fun broadcastBall(number: Int) {
        val message = JSONObject().apply {
            put("type", "ball")
            put("number", number)
        }.toString()

        Log.d(TAG, "Broadcasting ball: $number to ${clients.size} clients")
        broadcast(message)
    }

    fun broadcastNewGame(mode: Int) {
        currentMode = mode
        val message = JSONObject().apply {
            put("type", "new_game")
            put("mode", mode)
        }.toString()
        broadcast(message)
    }

    private fun broadcast(message: String) {
        // Crear copia para evitar ConcurrentModification
        val clientsCopy = ArrayList(clients)

        scope.launch {
            clientsCopy.forEach { client ->
                try {
                    client.send(message)
                } catch (e: Exception) {
                    Log.e(TAG, "Error sending to client", e)
                    removeClient(client)
                }
            }
        }
    }

    private fun removeClient(client: ClientHandler) {
        if (clients.remove(client)) {
            client.close()
            updateClientCount()
            Log.d(TAG, "Client removed. Remaining: ${clients.size}")
        }
    }

    private fun updateClientCount() {
        _connectedClients.value = clients.size
    }

    fun stop() {
        Log.d(TAG, "Stopping server...")
        isRunning = false
        _isActive.value = false

        // Notificar a clientes
        val closeMessage = JSONObject().apply {
            put("type", "server_closed")
        }.toString()

        clients.forEach { client ->
            try {
                client.send(closeMessage)
            } catch (e: Exception) { }
            client.close()
        }
        clients.clear()

        try { discoverySocket?.close() } catch (e: Exception) { }
        try { serverSocket?.close() } catch (e: Exception) { }

        discoverySocket = null
        serverSocket = null

        updateClientCount()
        Log.d(TAG, "Server stopped")
    }

    fun release() {
        stop()
        scope.cancel()
    }

    fun getLocalIpAddress(): String {
        return try {
            val wifiManager = context.applicationContext
                .getSystemService(Context.WIFI_SERVICE) as WifiManager
            val ip = wifiManager.connectionInfo.ipAddress
            if (ip == 0) {
                "No WiFi"
            } else {
                "${ip and 0xff}.${ip shr 8 and 0xff}.${ip shr 16 and 0xff}.${ip shr 24 and 0xff}"
            }
        } catch (e: Exception) {
            "Error"
        }
    }

    inner class ClientHandler(private val socket: Socket) {
        private var writer: PrintWriter? = null
        private var reader: BufferedReader? = null
        @Volatile private var running = true
        private var listenerJob: Job? = null

        fun start() {
            listenerJob = scope.launch {
                try {
                    socket.soTimeout = 0
                    socket.keepAlive = true

                    writer = PrintWriter(socket.getOutputStream(), true)
                    reader = BufferedReader(InputStreamReader(socket.getInputStream()))

                    // Enviar bienvenida
                    val welcome = JSONObject().apply {
                        put("type", "welcome")
                        put("mode", currentMode)
                    }.toString()
                    writer?.println(welcome)

                    Log.d(TAG, "Client connected and welcomed")

                    // Escuchar mensajes
                    while (running && isRunning && isActive) {
                        try {
                            val line = reader?.readLine()
                            if (line == null) {
                                Log.d(TAG, "Client disconnected (null)")
                                break
                            }
                            processMessage(line)
                        } catch (e: Exception) {
                            if (running && isRunning) {
                                Log.e(TAG, "Read error", e)
                            }
                            break
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Client handler error", e)
                } finally {
                    removeClient(this@ClientHandler)
                }
            }
        }

        private suspend fun processMessage(message: String) {
            try {
                val json = JSONObject(message)
                when (json.optString("type")) {
                    "bingo" -> {
                        val playerName = json.optString("player", "Jugador")
                        Log.d(TAG, "BINGO called by $playerName")
                        _bingoEvents.emit(playerName)

                        // Notificar a todos
                        val notification = JSONObject().apply {
                            put("type", "bingo_called")
                            put("player", playerName)
                        }.toString()
                        broadcast(notification)
                    }
                    "ping" -> {
                        send(JSONObject().apply { put("type", "pong") }.toString())
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Parse error: $message", e)
            }
        }

        @Synchronized
        fun send(message: String) {
            if (!running) return
            try {
                writer?.println(message)
                writer?.flush()
            } catch (e: Exception) {
                running = false
                throw e
            }
        }

        fun close() {
            running = false
            listenerJob?.cancel()
            try { reader?.close() } catch (e: Exception) { }
            try { writer?.close() } catch (e: Exception) { }
            try { socket.close() } catch (e: Exception) { }
        }
    }
}