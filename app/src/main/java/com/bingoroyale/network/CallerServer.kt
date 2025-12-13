package com.bingoroyale.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import kotlinx.coroutines.*
import java.io.OutputStreamWriter
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket

class CallerServer(
    private val context: Context,
    private val onPlayerCountChanged: (Int) -> Unit
) {
    companion object {
        private const val TAG = "CallerServer"
        private const val SERVICE_TYPE = "_bingo._tcp."
        private const val SERVICE_NAME = "BingoRoyale"
    }

    private var serverSocket: ServerSocket? = null
    private var nsdManager: NsdManager? = null
    private var registrationListener: NsdManager.RegistrationListener? = null
    private var isRunning = false

    private val clients = mutableListOf<ClientConnection>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Clase para manejar cada cliente
    private data class ClientConnection(
        val socket: Socket,
        val writer: PrintWriter
    )

    fun start() {
        if (isRunning) return

        scope.launch {
            try {
                serverSocket = ServerSocket(0) // Puerto aleatorio
                val port = serverSocket?.localPort ?: return@launch

                Log.d(TAG, "Server started on port $port")
                isRunning = true

                registerService(port)

                while (isRunning) {
                    try {
                        val clientSocket = serverSocket?.accept() ?: break
                        handleClient(clientSocket)
                    } catch (e: Exception) {
                        if (isRunning) {
                            Log.e(TAG, "Error accepting client", e)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Server error", e)
            }
        }
    }

    private fun registerService(port: Int) {
        val serviceInfo = NsdServiceInfo().apply {
            serviceName = SERVICE_NAME
            serviceType = SERVICE_TYPE
            setPort(port)
        }

        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(info: NsdServiceInfo) {
                Log.d(TAG, "Service registered: ${info.serviceName}")
            }

            override fun onRegistrationFailed(info: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "Service registration failed: $errorCode")
            }

            override fun onServiceUnregistered(info: NsdServiceInfo) {
                Log.d(TAG, "Service unregistered")
            }

            override fun onUnregistrationFailed(info: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "Service unregistration failed: $errorCode")
            }
        }

        nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
        nsdManager?.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
    }

    private fun handleClient(socket: Socket) {
        scope.launch {
            var clientConnection: ClientConnection? = null

            try {
                val writer = PrintWriter(
                    OutputStreamWriter(socket.getOutputStream()),
                    true
                )

                clientConnection = ClientConnection(socket, writer)

                // Agregar cliente y notificar FUERA del synchronized
                val newCount: Int
                synchronized(clients) {
                    clients.add(clientConnection)
                    newCount = clients.size
                }

                // Notificar en Main thread FUERA del synchronized
                withContext(Dispatchers.Main) {
                    onPlayerCountChanged(newCount)
                }

                Log.d(TAG, "Client connected. Total: $newCount")

                // Enviar mensaje de bienvenida
                writer.println("CONNECTED")

                // Mantener conexión activa
                while (isRunning && !socket.isClosed) {
                    delay(1000)

                    // Verificar si el socket sigue activo
                    if (socket.isClosed || !socket.isConnected) {
                        break
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Client error", e)
            } finally {
                // Remover cliente y notificar FUERA del synchronized
                val remainingCount: Int
                synchronized(clients) {
                    clientConnection?.let { clients.remove(it) }
                    remainingCount = clients.size
                }

                withContext(Dispatchers.Main) {
                    onPlayerCountChanged(remainingCount)
                }

                try {
                    socket.close()
                } catch (e: Exception) {
                    // Ignorar
                }

                Log.d(TAG, "Client disconnected. Remaining: $remainingCount")
            }
        }
    }

    fun broadcastBall(number: Int) {
        scope.launch {
            val clientsCopy: List<ClientConnection>
            synchronized(clients) {
                clientsCopy = clients.toList()
            }

            val deadClients = mutableListOf<ClientConnection>()

            clientsCopy.forEach { client ->
                try {
                    client.writer.println("BALL:$number")
                    if (client.writer.checkError()) {
                        deadClients.add(client)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error sending to client", e)
                    deadClients.add(client)
                }
            }

            // Limpiar clientes muertos
            if (deadClients.isNotEmpty()) {
                val remainingCount: Int
                synchronized(clients) {
                    clients.removeAll(deadClients)
                    remainingCount = clients.size
                }
                withContext(Dispatchers.Main) {
                    onPlayerCountChanged(remainingCount)
                }
            }
        }
    }

    fun broadcastNewGame() {
        scope.launch {
            val clientsCopy: List<ClientConnection>
            synchronized(clients) {
                clientsCopy = clients.toList()
            }

            clientsCopy.forEach { client ->
                try {
                    client.writer.println("NEW_GAME")
                } catch (e: Exception) {
                    Log.e(TAG, "Error sending new game", e)
                }
            }
        }
    }

    fun getConnectedCount(): Int {
        synchronized(clients) {
            return clients.size
        }
    }

    fun stop() {
        isRunning = false

        // Desregistrar servicio NSD
        try {
            registrationListener?.let { listener ->
                nsdManager?.unregisterService(listener)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering service", e)
        }

        // Cerrar todas las conexiones
        synchronized(clients) {
            clients.forEach { client ->
                try {
                    client.writer.println("SERVER_CLOSED")
                    client.socket.close()
                } catch (e: Exception) {
                    // Ignorar
                }
            }
            clients.clear()
        }

        // Cerrar servidor
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            // Ignorar
        }

        scope.cancel()
        Log.d(TAG, "Server stopped")
    }
}