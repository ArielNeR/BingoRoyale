package com.bingoroyale.model

data class CallerGameState(
    val drawnBalls: List<Int> = emptyList(),
    val ballsRemaining: Int = 75,
    val mode: Int = 75,
    val isNetworkActive: Boolean = false,
    val connectedPlayers: Int = 0,
    val bingoCalledBy: String? = null // Nombre del jugador que cantó BINGO
) {
    val currentBall: Int? get() = drawnBalls.lastOrNull()
    val isGameFinished: Boolean get() = ballsRemaining == 0
}

data class PlayerGameState(
    val card: BingoCard = BingoCard.generate(75),
    val markedCells: Set<Int> = setOf(12), // Centro FREE para modo 75
    val mode: Int = 75,
    val isConnected: Boolean = false,
    val lastReceivedBall: Int? = null,
    val serverName: String? = null
)

sealed class NetworkEvent {
    data class BallDrawn(val number: Int) : NetworkEvent()
    data class BingoCalled(val playerName: String) : NetworkEvent()
    object NewGame : NetworkEvent()
    object Disconnected : NetworkEvent()
}