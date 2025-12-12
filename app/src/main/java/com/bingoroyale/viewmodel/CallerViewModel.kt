package com.bingoroyale.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bingoroyale.network.BingoServer
import com.bingoroyale.network.ServerState
import com.bingoroyale.utils.PreferencesManager
import com.bingoroyale.utils.SoundManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.security.SecureRandom

class CallerViewModel(application: Application) : AndroidViewModel(application) {

    private val server = BingoServer(application)
    private val soundManager = SoundManager(application)
    private val preferences = PreferencesManager(application)
    private val secureRandom = SecureRandom()

    private val _uiState = MutableStateFlow(CallerUiState())
    val uiState: StateFlow<CallerUiState> = _uiState.asStateFlow()

    val serverState: StateFlow<ServerState> = server.serverState
    val connectedClients: StateFlow<Int> = server.connectedClients

    private var availableBalls = mutableListOf<Int>()

    init {
        initGame()
        soundManager.isSoundEnabled = preferences.isSoundEnabled
        soundManager.isVibrationEnabled = preferences.isVibrationEnabled
    }

    private fun initGame() {
        val mode = preferences.defaultMode
        availableBalls = (1..mode).toMutableList()
        shuffleBalls()
        _uiState.value = CallerUiState(mode = mode)
    }

    private fun shuffleBalls() {
        // Fisher-Yates shuffle con SecureRandom
        for (i in availableBalls.size - 1 downTo 1) {
            val j = secureRandom.nextInt(i + 1)
            val temp = availableBalls[i]
            availableBalls[i] = availableBalls[j]
            availableBalls[j] = temp
        }
    }

    fun startServer() {
        server.start("Bingo Royale", _uiState.value.mode)
    }

    fun stopServer() {
        server.stop()
    }

    fun setMode(mode: Int) {
        preferences.defaultMode = mode
        resetGame()
    }

    fun drawBall() {
        if (availableBalls.isEmpty()) {
            _uiState.value = _uiState.value.copy(isGameOver = true)
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAnimating = true)

            val ball = availableBalls.removeAt(availableBalls.size - 1)

            val newDrawnBalls = _uiState.value.drawnBalls + ball
            _uiState.value = _uiState.value.copy(
                currentBall = ball,
                drawnBalls = newDrawnBalls,
                isGameOver = availableBalls.isEmpty()
            )

            soundManager.playBallSound()

            // Transmitir a jugadores conectados
            server.broadcastBall(ball)

            delay(500)
            _uiState.value = _uiState.value.copy(isAnimating = false)
        }
    }

    fun resetGame() {
        val mode = preferences.defaultMode
        availableBalls = (1..mode).toMutableList()
        shuffleBalls()

        _uiState.value = CallerUiState(mode = mode)
        server.broadcastGameReset()
    }

    override fun onCleared() {
        super.onCleared()
        server.release()
        soundManager.release()
    }
}

data class CallerUiState(
    val mode: Int = 75,
    val currentBall: Int? = null,
    val drawnBalls: List<Int> = emptyList(),
    val isAnimating: Boolean = false,
    val isGameOver: Boolean = false
)