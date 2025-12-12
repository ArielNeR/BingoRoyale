package com.bingoroyale.network

import android.content.Context
import android.net.wifi.WifiManager
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject
import java.io.PrintWriter
import java.net.*
import java.util.concurrent.CopyOnWriteArrayList

class BingoServer(private val context: Context) {

    companion object {
        private const val TAG = "BingoServer"
        const val DEFAULT_PORT = 8888
        private const val DISCOVERY_PORT = 8889
        private const val BUFFER_SIZE = 1024
    }

    private var serverSocket: ServerSocket? = null
    private var discoverySocket: DatagramSocket? = null
    private var isRunning = false
    private val clients = CopyOnWriteArrayList<ClientHandler>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var serverName = "Bingo Royale"
    private var gameMode = 75

    private val _serverState = MutableStateFlow<ServerState>(ServerState.Stopped)
    val serverState: StateFlow<ServerState> = _serverState

    private val _connectedClients = MutableStateFlow(0)
    val connectedClients: StateFlow<Int> = _connectedClients

    fun start(name: String = "Bingo Royale", mode: Int = 75) {
        if (isRunning) return

        serverName = name
        gameMode = mode

        scope.launch {
            try {
                serverSocket = ServerSocket(DEFAULT_PORT)
                isRunning = true

                val ipAddress = getLocalIpAddress()
                _serverState.value = ServerState.Running(ipAddress, DEFAULT_PORT)

                Log.d(TAG, "Server started on $ipAddress:$DEFAULT_PORT")

                // Iniciar servicio de descubrimiento
                startDiscoveryService()

                // Aceptar conexiones
                while (isRunning) {
                    try {
                        val clientSocket = serverSocket?.accept()
                        clientSocket?.let {
                            val handler = ClientHandler(it)
                            clients.add(handler)
                            _connectedClients.value = clients.size
                            handler.start()
                            Log.d(TAG, "Client connected: ${it.inetAddress.hostAddress}")
                        }
                    } catch (e: SocketException) {
                        if (isRunning) {
                            Log.e(TAG, "Socket error: ${e.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Server error: ${e.message}")
                _serverState.value = ServerState.Error("Error: ${e.message}")
            }
        }
    }

    private fun startDiscoveryService() {
        scope.launch {
            try {
                discoverySocket = DatagramSocket(DISCOVERY_PORT)
                discoverySocket?.broadcast = true

                val buffer = ByteArray(BUFFER_SIZE)

                while (isRunning) {
                    try {
                        val packet = DatagramPacket(buffer, buffer.size)
                        discoverySocket?.receive(packet)

                        val message = String(packet.data, 0, packet.length)
                        if (message == "BINGO_DISCOVER") {
                            // Responder con información del servidor
                            val response = "BINGO_SERVER:$serverName:$DEFAULT_PORT:$gameMode"
                            val responsePacket = DatagramPacket(
                                response.toByteArray(),
                                response.length,
                                packet.address,
                                packet.port
                            )
                            discoverySocket?.send(responsePacket)
                            Log.d(TAG, "Responded to discovery from ${packet.address.hostAddress}")
                        }
                    } catch (e: Exception) {
                        if (isRunning) {
                            Log.e(TAG, "Discovery error: ${e.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Discovery service error: ${e.message}")
            }
        }
    }

    fun broadcastBall(number: Int) {
        scope.launch {
            val message = JSONObject().apply {
                put("type", "ball")
                put("number", number)
                put("timestamp", System.currentTimeMillis())
            }
            broadcast(message.toString())
        }
    }

    fun broadcastGameStart() {
        scope.launch {
            val message = JSONObject().apply {
                put("type", "game_start")
                put("mode", gameMode)
            }
            broadcast(message.toString())
        }
    }

    fun broadcastGameReset() {
        scope.launch {
            val message = JSONObject().apply {
                put("type", "game_reset")
            }
            broadcast(message.toString())
        }
    }

    fun broadcastGameEnd() {
        scope.launch {
            val message = JSONObject().apply {
                put("type", "game_end")
            }
            broadcast(message.toString())
        }
    }

    private fun broadcast(message: String) {
        val deadClients = mutableListOf<ClientHandler>()

        clients.forEach { client ->
            try {
                client.send(message)
            } catch (e: Exception) {
                Log.e(TAG, "Error sending to client: ${e.message}")
                deadClients.add(client)
            }
        }

        deadClients.forEach { client ->
            clients.remove(client)
            client.close()
        }

        _connectedClients.value = clients.size
    }

    fun stop() {
        isRunning = false

        clients.forEach { it.close() }
        clients.clear()

        try {
            discoverySocket?.close()
            serverSocket?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping server: ${e.message}")
        }

        _serverState.value = ServerState.Stopped
        _connectedClients.value = 0
    }

    fun release() {
        stop()
        scope.cancel()
    }

    private fun getLocalIpAddress(): String {
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val wifiInfo = wifiManager.connectionInfo
            val ipInt = wifiInfo.ipAddress

            if (ipInt != 0) {
                return String.format(
                    "%d.%d.%d.%d",
                    ipInt and 0xff,
                    ipInt shr 8 and 0xff,
                    ipInt shr 16 and 0xff,
                    ipInt shr 24 and 0xff
                )
            }

            // Alternativa: buscar en interfaces de red
            NetworkInterface.getNetworkInterfaces()?.toList()?.forEach { networkInterface ->
                networkInterface.inetAddresses?.toList()?.forEach { address ->
                    if (!address.isLoopbackAddress && address is Inet4Address) {
                        return address.hostAddress ?: "0.0.0.0"
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting IP: ${e.message}")
        }
        return "0.0.0.0"
    }

    private inner class ClientHandler(private val socket: Socket) {
        private var writer: PrintWriter? = null
        private var isActive = true

        fun start() {
            scope.launch {
                try {
                    writer = PrintWriter(socket.getOutputStream(), true)

                    // Enviar mensaje de bienvenida
                    val welcome = JSONObject().apply {
                        put("type", "welcome")
                        put("server", serverName)
                        put("mode", gameMode)
                    }
                    send(welcome.toString())

                } catch (e: Exception) {
                    Log.e(TAG, "Client handler error: ${e.message}")
                    close()
                }
            }
        }

        fun send(message: String) {
            if (isActive) {
                writer?.println(message)
                writer?.flush()
            }
        }

        fun close() {
            isActive = false
            try {
                writer?.close()
                socket.close()
            } catch (e: Exception) {
                Log.e(TAG, "Error closing client: ${e.message}")
            }
        }
    }
}

sealed class ServerState {
    object Stopped : ServerState()
    data class Running(val ip: String, val port: Int) : ServerState()
    data class Error(val message: String) : ServerState()
}