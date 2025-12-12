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
        private const val PORT = 0 // Puerto aleatorio
    }

    private var serverSocket: ServerSocket? = null
    private var nsdManager: NsdManager? = null
    private var serviceName: String? = null
    private var isRunning = false

    private val clients = mutableListOf<PrintWriter>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun start() {
        scope.launch {
            try {
                // Crear servidor
                serverSocket = ServerSocket(PORT)
                val port = serverSocket?.localPort ?: return@launch

                Log.d(TAG, "Server started on port $port")
                isRunning = true

                // Registrar servicio NSD
                registerService(port)

                // Aceptar conexiones
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

        nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
        nsdManager?.registerService(
            serviceInfo,
            NsdManager.PROTOCOL_DNS_SD,
            object : NsdManager.RegistrationListener {
                override fun onServiceRegistered(info: NsdServiceInfo) {
                    serviceName = info.serviceName
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
        )
    }

    private fun handleClient(socket: Socket) {
        scope.launch {
            try {
                val writer = PrintWriter(
                    OutputStreamWriter(socket.getOutputStream()),
                    true
                )

                synchronized(clients) {
                    clients.add(writer)
                    withContext(Dispatchers.Main) {
                        onPlayerCountChanged(clients.size)
                    }
                }

                Log.d(TAG, "Client connected. Total: ${clients.size}")

                // Mantener conexión activa
                while (isRunning && !socket.isClosed) {
                    delay(1000)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Client error", e)
            } finally {
                synchronized(clients) {
                    val writer = clients.find { true } // Debería guardar referencia
                    clients.removeIf { socket.isClosed }
                    scope.launch(Dispatchers.Main) {
                        onPlayerCountChanged(clients.size)
                    }
                }
            }
        }
    }

    fun broadcastBall(number: Int) {
        scope.launch {
            synchronized(clients) {
                clients.forEach { writer ->
                    try {
                        writer.println("BALL:$number")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error sending to client", e)
                    }
                }
            }
        }
    }

    fun broadcastNewGame() {
        scope.launch {
            synchronized(clients) {
                clients.forEach { writer ->
                    try {
                        writer.println("NEW_GAME")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error sending new game", e)
                    }
                }
            }
        }
    }

    fun stop() {
        isRunning = false

        try {
            nsdManager?.unregisterService(object : NsdManager.RegistrationListener {
                override fun onServiceRegistered(info: NsdServiceInfo) {}
                override fun onRegistrationFailed(info: NsdServiceInfo, errorCode: Int) {}
                override fun onServiceUnregistered(info: NsdServiceInfo) {}
                override fun onUnregistrationFailed(info: NsdServiceInfo, errorCode: Int) {}
            })
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering service", e)
        }

        clients.clear()
        serverSocket?.close()
        scope.cancel()

        Log.d(TAG, "Server stopped")
    }
}