package com.bingoroyale.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bingoroyale.R
import com.bingoroyale.databinding.ItemBingoCellBinding
import com.bingoroyale.model.BingoCard

class BingoCardAdapter(
    private val onCellClick: (Int) -> Unit
) : RecyclerView.Adapter<BingoCardAdapter.CellViewHolder>() {

    private var numbers: List<Int> = emptyList()
    private var markedCells: Set<Int> = emptySet()

    fun submitCard(card: BingoCard, marked: Set<Int>) {
        numbers = card.toFlatList()
        markedCells = marked
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CellViewHolder {
        val binding = ItemBingoCellBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        // Hacer las celdas cuadradas
        val width = parent.measuredWidth / 5
        binding.root.layoutParams = ViewGroup.LayoutParams(width, width)
        binding.cellContainer.layoutParams.height = width - 4 // Menos padding

        return CellViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CellViewHolder, position: Int) {
        holder.bind(position, numbers[position], markedCells.contains(position))
    }

    override fun getItemCount(): Int = numbers.size

    inner class CellViewHolder(
        private val binding: ItemBingoCellBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(position: Int, number: Int, isMarked: Boolean) {
            val isFreeSpace = number == 0

            if (isFreeSpace) {
                binding.tvNumber.text = "⭐"
                binding.cellContainer.setBackgroundResource(R.drawable.bg_bingo_cell_free)
                binding.tvCheck.visibility = View.GONE
            } else {
                binding.tvNumber.text = number.toString()
                binding.cellContainer.isSelected = isMarked
                binding.tvNumber.visibility = if (isMarked) View.GONE else View.VISIBLE
                binding.tvCheck.visibility = if (isMarked) View.VISIBLE else View.GONE
            }

            binding.cellContainer.setOnClickListener {
                if (!isFreeSpace) {
                    onCellClick(position)
                }
            }
        }
    }
}