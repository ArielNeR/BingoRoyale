package com.bingoroyale.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.bingoroyale.model.BingoCard
import com.bingoroyale.model.NetworkEvent
import com.bingoroyale.model.PlayerGameState
import com.bingoroyale.network.ConnectionState
import com.bingoroyale.network.PlayerClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlayerViewModel(app: Application) : AndroidViewModel(app) {

    companion object {
        private const val TAG = "PlayerViewModel"
    }

    private val client = PlayerClient()

    private val _gameState = MutableLiveData<PlayerGameState>()
    val gameState: LiveData<PlayerGameState> = _gameState

    private val _networkEvent = MutableLiveData<NetworkEvent?>()
    val networkEvent: LiveData<NetworkEvent?> = _networkEvent

    private val _showBingoAnimation = MutableLiveData(false)
    val showBingoAnimation: LiveData<Boolean> = _showBingoAnimation

    private val _bingoNotification = MutableLiveData<String?>()
    val bingoNotification: LiveData<String?> = _bingoNotification

    private var markedCells = mutableSetOf<Int>()
    private var currentMode = 75

    private var observerJob: Job? = null

    init {
        Log.d(TAG, "Initializing")
        generateNewCard(75)
        startObserving()
    }

    private fun startObserving() {
        observerJob?.cancel()
        observerJob = viewModelScope.launch {
            // Observar estado de conexión
            launch {
                client.connectionState
                    .catch { e -> Log.e(TAG, "Connection state error", e) }
                    .collect { state ->
                        try {
                            handleConnectionState(state)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error handling connection state", e)
                        }
                    }
            }

            // Observar modo del servidor
            launch {
                client.serverMode
                    .catch { e -> Log.e(TAG, "Server mode error", e) }
                    .collect { mode ->
                        try {
                            handleServerMode(mode)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error handling server mode", e)
                        }
                    }
            }

            // Observar bolas entrantes
            launch {
                client.incomingBalls
                    .catch { e -> Log.e(TAG, "Incoming balls error", e) }
                    .collect { ball ->
                        try {
                            handleIncomingBall(ball)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error handling ball", e)
                        }
                    }
            }

            // Observar eventos del juego
            launch {
                client.gameEvents
                    .catch { e -> Log.e(TAG, "Game events error", e) }
                    .collect { event ->
                        try {
                            handleGameEvent(event)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error handling game event", e)
                        }
                    }
            }

            // Observar notificaciones de bingo
            launch {
                client.bingoNotification
                    .catch { e -> Log.e(TAG, "Bingo notification error", e) }
                    .collect { playerName ->
                        try {
                            withContext(Dispatchers.Main) {
                                _bingoNotification.value = playerName
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error handling bingo notification", e)
                        }
                    }
            }
        }
    }

    private suspend fun handleConnectionState(state: ConnectionState) {
        withContext(Dispatchers.Main) {
            Log.d(TAG, "Connection state: $state")
            val wasConnected = _gameState.value?.isConnected ?: false
            val isConnected = state is ConnectionState.Connected
            val serverName = (state as? ConnectionState.Connected)?.serverName

            updateState { copy(isConnected = isConnected, serverName = serverName) }

            if (wasConnected && !isConnected) {
                _networkEvent.value = NetworkEvent.Disconnected
            }
        }
    }

    private suspend fun handleServerMode(mode: Int) {
        withContext(Dispatchers.Main) {
            Log.d(TAG, "Server mode: $mode")
            if (mode != currentMode) {
                currentMode = mode
                generateNewCard(mode)
            }
        }
    }

    private suspend fun handleIncomingBall(ball: Int) {
        withContext(Dispatchers.Main) {
            Log.d(TAG, "Ball received: $ball")
            updateState { copy(lastReceivedBall = ball) }
            _networkEvent.value = NetworkEvent.BallDrawn(ball)
        }
    }

    private suspend fun handleGameEvent(event: String) {
        withContext(Dispatchers.Main) {
            Log.d(TAG, "Game event: $event")
            when (event) {
                "new_game" -> {
                    _networkEvent.value = NetworkEvent.NewGame
                    generateNewCard(currentMode)
                }
                "disconnected" -> {
                    _networkEvent.value = NetworkEvent.Disconnected
                }
            }
        }
    }

    private inline fun updateState(update: PlayerGameState.() -> PlayerGameState) {
        val current = _gameState.value ?: return
        _gameState.value = current.update()
    }

    fun generateNewCard(mode: Int = currentMode) {
        Log.d(TAG, "Generating new card, mode: $mode")
        currentMode = mode
        val card = BingoCard.generate(mode)

        markedCells = if (mode == 75) mutableSetOf(12) else mutableSetOf()

        _gameState.value = PlayerGameState(
            card = card,
            markedCells = markedCells.toSet(),
            mode = mode,
            isConnected = _gameState.value?.isConnected ?: false,
            serverName = _gameState.value?.serverName,
            lastReceivedBall = null
        )

        Log.d(TAG, "Card generated: ${card.toFlatList().size} cells")
    }

    fun toggleCellMark(index: Int) {
        val currentState = _gameState.value ?: return
        val cellValue = currentState.card.toFlatList().getOrNull(index) ?: return

        if (cellValue <= 0) return

        if (markedCells.contains(index)) {
            markedCells.remove(index)
        } else {
            markedCells.add(index)
        }

        _gameState.value = currentState.copy(markedCells = markedCells.toSet())
    }

    fun clearAllMarks() {
        markedCells = if (currentMode == 75) mutableSetOf(12) else mutableSetOf()
        _gameState.value = _gameState.value?.copy(
            markedCells = markedCells.toSet(),
            lastReceivedBall = null
        )
    }

    fun searchAndConnect() {
        client.discoverAndConnect()
    }

    fun disconnect() {
        client.disconnect()
    }

    fun callBingo() {
        _showBingoAnimation.value = true
        client.sendBingo("Jugador")
    }

    fun dismissBingoAnimation() {
        _showBingoAnimation.value = false
    }

    fun clearNetworkEvent() {
        _networkEvent.value = null
    }

    fun clearBingoNotification() {
        _bingoNotification.value = null
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "Clearing")
        observerJob?.cancel()
        client.release()
    }
}