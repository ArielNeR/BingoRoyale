package com.bingoroyale.model

import java.security.SecureRandom

/**
 * Representa un cartón de bingo de 5x5 (75 bolas)
 * Columna B: 1-15
 * Columna I: 16-30
 * Columna N: 31-45 (centro es FREE)
 * Columna G: 46-60
 * Columna O: 61-75
 */
data class BingoCard(
    val cells: Array<IntArray> = Array(5) { IntArray(5) }
) {
    companion object {
        private val secureRandom = SecureRandom()

        fun generate(): BingoCard {
            val card = BingoCard()
            val ranges = arrayOf(
                1..15,   // B
                16..30,  // I
                31..45,  // N
                46..60,  // G
                61..75   // O
            )

            for (col in 0 until 5) {
                val available = ranges[col].toMutableList()
                available.shuffle(secureRandom)

                for (row in 0 until 5) {
                    if (col == 2 && row == 2) {
                        // Centro es FREE (representado como 0)
                        card.cells[col][row] = 0
                    } else {
                        card.cells[col][row] = available.removeAt(0)
                    }
                }
            }

            return card
        }

        fun getLetterForColumn(col: Int): String {
            return when (col) {
                0 -> "B"
                1 -> "I"
                2 -> "N"
                3 -> "G"
                4 -> "O"
                else -> ""
            }
        }

        fun getLetterForNumber(number: Int): String {
            return when {
                number in 1..15 -> "B"
                number in 16..30 -> "I"
                number in 31..45 -> "N"
                number in 46..60 -> "G"
                number in 61..75 -> "O"
                else -> ""
            }
        }

        fun getColumnForNumber(number: Int): Int {
            return when {
                number in 1..15 -> 0
                number in 16..30 -> 1
                number in 31..45 -> 2
                number in 46..60 -> 3
                number in 61..75 -> 4
                else -> -1
            }
        }
    }

    // Para acceder como lista plana (para el RecyclerView)
    fun toFlatList(): List<Int> {
        val list = mutableListOf<Int>()
        for (row in 0 until 5) {
            for (col in 0 until 5) {
                list.add(cells[col][row])
            }
        }
        return list
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as BingoCard
        return cells.contentDeepEquals(other.cells)
    }

    override fun hashCode(): Int {
        return cells.contentDeepHashCode()
    }
}