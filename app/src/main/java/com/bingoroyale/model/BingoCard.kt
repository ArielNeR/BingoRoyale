package com.bingoroyale.model

import java.security.SecureRandom

data class BingoCard(
    val mode: Int = 75,
    val cells: Array<IntArray>
) {
    companion object {
        private val random = SecureRandom()

        fun generate(mode: Int = 75): BingoCard {
            return if (mode == 75) generate75() else generate90()
        }

        private fun generate75(): BingoCard {
            // 5x5 grid, almacenado como columns[col][row]
            val cells = Array(5) { IntArray(5) }
            val ranges = arrayOf(1..15, 16..30, 31..45, 46..60, 61..75)

            for (col in 0..4) {
                val available = ranges[col].shuffled(random).toMutableList()
                for (row in 0..4) {
                    cells[col][row] = if (col == 2 && row == 2) 0 else available.removeAt(0)
                }
            }
            return BingoCard(mode = 75, cells = cells)
        }

        private fun generate90(): BingoCard {
            // 3 filas x 9 columnas
            // Cada fila tiene 5 números y 4 espacios vacíos (-1)
            val cells = Array(3) { IntArray(9) { -1 } }

            // Rangos por columna
            val columnRanges = arrayOf(
                (1..9).toMutableList(),
                (10..19).toMutableList(),
                (20..29).toMutableList(),
                (30..39).toMutableList(),
                (40..49).toMutableList(),
                (50..59).toMutableList(),
                (60..69).toMutableList(),
                (70..79).toMutableList(),
                (80..90).toMutableList()
            )

            // Mezclar cada columna
            columnRanges.forEach { it.shuffle(random) }

            // Para cada fila, elegir 5 columnas al azar
            for (row in 0..2) {
                val selectedCols = (0..8).shuffled(random).take(5).sorted()

                for (col in selectedCols) {
                    if (columnRanges[col].isNotEmpty()) {
                        cells[row][col] = columnRanges[col].removeAt(0)
                    }
                }
            }

            return BingoCard(mode = 90, cells = cells)
        }

        fun getLetterForNumber(n: Int): String = when {
            n in 1..15 -> "B"
            n in 16..30 -> "I"
            n in 31..45 -> "N"
            n in 46..60 -> "G"
            n in 61..75 -> "O"
            else -> ""
        }
    }

    // Convierte a lista plana para el adapter
    fun toFlatList(): List<Int> {
        return if (mode == 75) {
            // 5x5: leer por filas (row-major)
            (0..4).flatMap { row ->
                (0..4).map { col -> cells[col][row] }
            }
        } else {
            // 3x9: ya está en formato row-major
            cells.flatMap { it.toList() }
        }
    }

    fun getRows(): Int = if (mode == 75) 5 else 3
    fun getCols(): Int = if (mode == 75) 5 else 9

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BingoCard) return false
        return mode == other.mode && cells.contentDeepEquals(other.cells)
    }

    override fun hashCode(): Int = 31 * mode + cells.contentDeepHashCode()
}