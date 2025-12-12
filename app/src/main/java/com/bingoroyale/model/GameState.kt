package com.bingoroyale.model

data class CallerGameState(
    val drawnBalls: List<Int> = emptyList(),
    val remainingBalls: List<Int> = (1..75).toList().shuffled(),
    val isNetworkActive: Boolean = false,
    val connectedPlayers: Int = 0
) {
    val currentBall: Int? get() = drawnBalls.lastOrNull()
    val ballsRemaining: Int get() = remainingBalls.size
    val isGameFinished: Boolean get() = remainingBalls.isEmpty()
}

data class PlayerGameState(
    val card: BingoCard = BingoCard.generate(),
    val markedCells: Set<Int> = setOf(12), // Centro (FREE) siempre marcado
    val isConnected: Boolean = false,
    val lastReceivedBall: Int? = null,
    val serverName: String? = null
)

sealed class NetworkEvent {
    data class BallDrawn(val number: Int) : NetworkEvent()
    data class PlayerConnected(val count: Int) : NetworkEvent()
    data class ConnectionLost(val reason: String) : NetworkEvent()
    object NewGame : NetworkEvent()
}