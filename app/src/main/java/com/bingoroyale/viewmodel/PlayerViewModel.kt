package com.bingoroyale.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bingoroyale.model.BingoCard
import com.bingoroyale.network.ConnectionState
import com.bingoroyale.network.GameEvent
import com.bingoroyale.network.PlayerClient
import com.bingoroyale.network.ServerInfo
import com.bingoroyale.utils.PreferencesManager
import com.bingoroyale.utils.SoundManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val client = PlayerClient()
    private val soundManager = SoundManager(application)
    private val preferences = PreferencesManager(application)

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    val connectionState: StateFlow<ConnectionState> = client.connectionState
    val discoveredServers: StateFlow<List<ServerInfo>> = client.discoveredServers

    init {
        soundManager.isSoundEnabled = preferences.isSoundEnabled
        soundManager.isVibrationEnabled = preferences.isVibrationEnabled

        generateNewCard()
        observeIncomingBalls()
        observeGameEvents()
    }

    private fun observeIncomingBalls() {
        viewModelScope.launch {
            client.incomingBalls.collect { ball ->
                val newReceivedBalls = _uiState.value.receivedBalls + ball
                _uiState.value = _uiState.value.copy(
                    receivedBalls = newReceivedBalls,
                    lastReceivedBall = ball
                )
                soundManager.playBallSound()

                // Auto-marcar si está habilitado
                if (preferences.isAutoMarkEnabled) {
                    autoMarkBall(ball)
                }

                checkAchievements()
            }
        }
    }

    private fun observeGameEvents() {
        viewModelScope.launch {
            client.gameEvents.collect { event ->
                when (event) {
                    is GameEvent.GameStarted -> {
                        setMode(event.mode)
                        generateNewCard()
                    }
                    is GameEvent.GameReset -> {
                        _uiState.value = _uiState.value.copy(
                            receivedBalls = emptyList(),
                            markedCells = if (_uiState.value.mode == 75) setOf(12) else emptySet(),
                            hasLine = false,
                            hasBingo = false
                        )
                    }
                    is GameEvent.GameEnded -> {
                        // Manejar fin del juego
                    }
                    is GameEvent.Disconnected -> {
                        // Manejar desconexión
                    }
                }
            }
        }
    }

    fun generateNewCard() {
        val mode = _uiState.value.mode
        val card = BingoCard.generate(mode)

        _uiState.value = _uiState.value.copy(
            card = card,
            markedCells = if (mode == 75) setOf(12) else emptySet(), // Centro marcado en 75
            hasLine = false,
            hasBingo = false
        )
    }

    fun setMode(mode: Int) {
        preferences.defaultMode = mode
        _uiState.value = _uiState.value.copy(mode = mode)
        generateNewCard()
    }

    fun toggleMark(index: Int) {
        val currentMarked = _uiState.value.markedCells.toMutableSet()

        // Verificar si el número está en las bolas recibidas o si estamos offline
        val cellValue = getCellValue(index)
        val canMark = cellValue != null &&
                (_uiState.value.receivedBalls.contains(cellValue) ||
                        connectionState.value !is ConnectionState.Connected)

        if (currentMarked.contains(index)) {
            currentMarked.remove(index)
        } else if (canMark) {
            currentMarked.add(index)
            soundManager.playMarkSound()
        }

        _uiState.value = _uiState.value.copy(markedCells = currentMarked)
        checkAchievements()
    }

    private fun getCellValue(index: Int): Int? {
        val mode = _uiState.value.mode
        val card = _uiState.value.card

        return if (mode == 75) {
            val row = index / 5
            val col = index % 5
            if (row == 2 && col == 2) null // FREE space
            else card.getOrNull(col)?.getOrNull(row) as? Int
        } else {
            val row = index / 9
            val col = index % 9
            card.getOrNull(row)?.getOrNull(col) as? Int
        }
    }

    private fun autoMarkBall(ball: Int) {
        val mode = _uiState.value.mode
        val card = _uiState.value.card

        if (mode == 75) {
            for (col in 0 until 5) {
                for (row in 0 until 5) {
                    if (card.getOrNull(col)?.getOrNull(row) == ball) {
                        val index = row * 5 + col
                        val currentMarked = _uiState.value.markedCells.toMutableSet()
                        currentMarked.add(index)
                        _uiState.value = _uiState.value.copy(markedCells = currentMarked)
                    }
                }
            }
        } else {
            for (row in 0 until 3) {
                for (col in 0 until 9) {
                    if (card.getOrNull(row)?.getOrNull(col) == ball) {
                        val index = row * 9 + col
                        val currentMarked = _uiState.value.markedCells.toMutableSet()
                        currentMarked.add(index)
                        _uiState.value = _uiState.value.copy(markedCells = currentMarked)
                    }
                }
            }
        }
    }

    private fun checkAchievements() {
        val mode = _uiState.value.mode
        val marked = _uiState.value.markedCells

        if (mode == 75) {
            check75Achievements(marked)
        } else {
            check90Achievements(marked)
        }
    }

    private fun check75Achievements(marked: Set<Int>) {
        val lines = listOf(
            listOf(0, 1, 2, 3, 4),     // Fila 1
            listOf(5, 6, 7, 8, 9),     // Fila 2
            listOf(10, 11, 12, 13, 14), // Fila 3
            listOf(15, 16, 17, 18, 19), // Fila 4
            listOf(20, 21, 22, 23, 24), // Fila 5
            listOf(0, 5, 10, 15, 20),   // Col 1
            listOf(1, 6, 11, 16, 21),   // Col 2
            listOf(2, 7, 12, 17, 22),   // Col 3
            listOf(3, 8, 13, 18, 23),   // Col 4
            listOf(4, 9, 14, 19, 24),   // Col 5
            listOf(0, 6, 12, 18, 24),   // Diagonal 1
            listOf(4, 8, 12, 16, 20)    // Diagonal 2
        )

        val hasLine = lines.any { line -> line.all { marked.contains(it) } }
        val hasBingo = marked.size == 25

        if (hasLine && !_uiState.value.hasLine) {
            soundManager.playLineSound()
        }
        if (hasBingo && !_uiState.value.hasBingo) {
            soundManager.playBingoSound()
        }

        _uiState.value = _uiState.value.copy(
            hasLine = hasLine,
            hasBingo = hasBingo
        )
    }

    private fun check90Achievements(marked: Set<Int>) {
        var completedLines = 0
        val card = _uiState.value.card

        for (row in 0 until 3) {
            var lineComplete = true
            for (col in 0 until 9) {
                val value = card.getOrNull(row)?.getOrNull(col)
                if (value != null && !marked.contains(row * 9 + col)) {
                    lineComplete = false
                    break
                }
            }
            if (lineComplete) completedLines++
        }

        val hasLine = completedLines >= 1
        val hasBingo = completedLines == 3

        if (hasLine && !_uiState.value.hasLine) {
            soundManager.playLineSound()
        }
        if (hasBingo && !_uiState.value.hasBingo) {
            soundManager.playBingoSound()
        }

        _uiState.value = _uiState.value.copy(
            hasLine = hasLine,
            hasBingo = hasBingo
        )
    }

    fun claimBingo() {
        soundManager.playBingoSound()
        // En una versión con verificación, enviaríamos al servidor
    }

    fun discoverServers() {
        client.discoverServers()
    }

    fun connectToServer(server: ServerInfo) {
        client.connectToServer(server)
    }

    fun connectByIp(ip: String) {
        client.connectByIp(ip)
    }

    fun disconnect() {
        client.disconnect()
    }

    override fun onCleared() {
        super.onCleared()
        client.release()
        soundManager.release()
    }
}

data class PlayerUiState(
    val mode: Int = 75,
    val card: List<List<Any?>> = emptyList(),
    val markedCells: Set<Int> = emptySet(),
    val receivedBalls: List<Int> = emptyList(),
    val lastReceivedBall: Int? = null,
    val hasLine: Boolean = false,
    val hasBingo: Boolean = false
)