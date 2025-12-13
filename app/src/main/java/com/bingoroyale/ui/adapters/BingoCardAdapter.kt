package com.bingoroyale.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bingoroyale.R
import com.bingoroyale.databinding.ItemBingoCellBinding
import com.bingoroyale.model.BingoCard

class BingoCardAdapter(
    private val onCellClick: (Int) -> Unit
) : RecyclerView.Adapter<BingoCardAdapter.CellViewHolder>() {

    private var cells: List<Int> = emptyList()
    private var markedCells: Set<Int> = emptySet()
    private var mode: Int = 75

    fun submitCard(card: BingoCard, marked: Set<Int>) {
        val newCells = card.toFlatList()
        val newMode = card.mode

        // Solo actualizar si hay cambios reales
        if (cells != newCells || markedCells != marked || mode != newMode) {
            cells = newCells
            markedCells = marked
            mode = newMode
            notifyDataSetChanged()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CellViewHolder {
        val binding = ItemBingoCellBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CellViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CellViewHolder, position: Int) {
        if (position < cells.size) {
            holder.bind(position, cells[position], markedCells.contains(position), mode)
        }
    }

    override fun getItemCount() = cells.size

    class CellViewHolder(
        private val binding: ItemBingoCellBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private var currentClickListener: (() -> Unit)? = null

        init {
            binding.cellContainer.setOnClickListener {
                currentClickListener?.invoke()
            }
        }

        fun bind(index: Int, number: Int, isMarked: Boolean, mode: Int, onClick: (() -> Unit)? = null) {
            val context = binding.root.context

            // Limpiar listener anterior
            currentClickListener = null

            when {
                // Espacio vacío (modo 90)
                number == -1 -> {
                    binding.tvNumber.visibility = View.INVISIBLE
                    binding.tvCheck.visibility = View.GONE
                    binding.cellContainer.setBackgroundColor(
                        ContextCompat.getColor(context, R.color.bg_secondary)
                    )
                    binding.cellContainer.alpha = 0.3f
                    binding.cellContainer.isClickable = false
                }
                // FREE space (modo 75)
                number == 0 -> {
                    binding.tvNumber.text = "⭐"
                    binding.tvNumber.visibility = View.VISIBLE
                    binding.tvCheck.visibility = View.GONE
                    binding.cellContainer.setBackgroundResource(R.drawable.bg_cell_free)
                    binding.cellContainer.alpha = 1f
                    binding.cellContainer.isClickable = false
                }
                // Celda marcada
                isMarked -> {
                    binding.tvNumber.visibility = View.GONE
                    binding.tvCheck.visibility = View.VISIBLE
                    binding.cellContainer.setBackgroundResource(R.drawable.bg_cell_marked)
                    binding.cellContainer.alpha = 1f
                    binding.cellContainer.isClickable = true
                }
                // Celda normal
                else -> {
                    binding.tvNumber.text = number.toString()
                    binding.tvNumber.visibility = View.VISIBLE
                    binding.tvCheck.visibility = View.GONE
                    binding.cellContainer.setBackgroundResource(R.drawable.bg_bingo_cell)
                    binding.cellContainer.alpha = 1f
                    binding.cellContainer.isClickable = true
                }
            }
        }

        fun bind(index: Int, number: Int, isMarked: Boolean, mode: Int) {
            bind(index, number, isMarked, mode, null)
        }

        fun setClickListener(listener: () -> Unit) {
            currentClickListener = listener
        }
    }

    override fun onBindViewHolder(holder: CellViewHolder, position: Int, payloads: MutableList<Any>) {
        if (position < cells.size) {
            val number = cells[position]
            val isMarked = markedCells.contains(position)

            holder.bind(position, number, isMarked, mode)

            // Solo permitir click en celdas válidas
            if (number > 0) {
                holder.setClickListener { onCellClick(position) }
            }
        }
    }
}