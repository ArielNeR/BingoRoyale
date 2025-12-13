package com.bingoroyale.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.bingoroyale.model.CallerGameState
import com.bingoroyale.network.BingoServer
import kotlinx.coroutines.launch
import java.security.SecureRandom

class CallerViewModel(app: Application) : AndroidViewModel(app) {

    private val server = BingoServer(app)
    private val random = SecureRandom()

    private val _gameState = MutableLiveData(CallerGameState())
    val gameState: LiveData<CallerGameState> = _gameState

    private val _lastDrawnBall = MutableLiveData<Int?>()
    val lastDrawnBall: LiveData<Int?> = _lastDrawnBall

    private val _bingoNotification = MutableLiveData<String?>()
    val bingoNotification: LiveData<String?> = _bingoNotification

    private var availableBalls = mutableListOf<Int>()
    private var drawnBalls = mutableListOf<Int>()
    private var currentMode = 75

    init {
        initGame(75)
        observeServer()
    }

    private fun initGame(mode: Int) {
        currentMode = mode
        val maxBall = if (mode == 75) 75 else 90
        availableBalls = (1..maxBall).toMutableList()
        shuffleBalls()
        drawnBalls.clear()
        _lastDrawnBall.value = null
        updateState()
    }

    private fun shuffleBalls() {
        for (i in availableBalls.size - 1 downTo 1) {
            val j = random.nextInt(i + 1)
            val temp = availableBalls[i]
            availableBalls[i] = availableBalls[j]
            availableBalls[j] = temp
        }
    }

    private fun observeServer() {
        viewModelScope.launch {
            server.connectedClients.collect { count ->
                _gameState.value = _gameState.value?.copy(connectedPlayers = count)
            }
        }
        viewModelScope.launch {
            server.isActive.collect { active ->
                _gameState.value = _gameState.value?.copy(isNetworkActive = active)
            }
        }
        viewModelScope.launch {
            server.bingoEvents.collect { playerName ->
                _bingoNotification.value = playerName
                _gameState.value = _gameState.value?.copy(bingoCalledBy = playerName)
            }
        }
    }

    fun drawNextBall() {
        if (availableBalls.isEmpty()) return

        val ball = availableBalls.removeAt(availableBalls.size - 1)
        drawnBalls.add(ball)
        _lastDrawnBall.value = ball

        server.broadcastBall(ball)
        updateState()
    }

    fun startNewGame(mode: Int = currentMode) {
        initGame(mode)
        server.broadcastNewGame(mode)
    }

    fun setMode(mode: Int) {
        if (mode != currentMode) {
            startNewGame(mode)
        }
    }

    fun toggleNetwork() {
        if (_gameState.value?.isNetworkActive == true) {
            server.stop()
        } else {
            server.start(currentMode)
        }
    }

    fun clearBingoNotification() {
        _bingoNotification.value = null
        _gameState.value = _gameState.value?.copy(bingoCalledBy = null)
    }

    private fun updateState() {
        _gameState.value = CallerGameState(
            drawnBalls = drawnBalls.toList(),
            ballsRemaining = availableBalls.size,
            mode = currentMode,
            isNetworkActive = _gameState.value?.isNetworkActive ?: false,
            connectedPlayers = _gameState.value?.connectedPlayers ?: 0
        )
    }

    fun getServerIp(): String = server.getLocalIpAddress()

    override fun onCleared() {
        super.onCleared()
        server.release()
    }
}