package com.bingoroyale.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bingoroyale.R
import com.bingoroyale.databinding.ItemBallHistoryBinding
import com.bingoroyale.model.BingoCard

class BallHistoryAdapter : ListAdapter<Int, BallHistoryAdapter.BallViewHolder>(BallDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BallViewHolder {
        val binding = ItemBallHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BallViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BallViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class BallViewHolder(
        private val binding: ItemBallHistoryBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(number: Int) {
            val letter = BingoCard.getLetterForNumber(number)
            binding.tvLetter.text = letter
            binding.tvNumber.text = number.toString()

            val bgRes = when (letter) {
                "B" -> R.drawable.bg_ball_b
                "I" -> R.drawable.bg_ball_i
                "N" -> R.drawable.bg_ball_n
                "G" -> R.drawable.bg_ball_g
                "O" -> R.drawable.bg_ball_o
                else -> R.drawable.bg_ball_empty
            }
            binding.ballBackground.setBackgroundResource(bgRes)
        }
    }

    class BallDiffCallback : DiffUtil.ItemCallback<Int>() {
        override fun areItemsTheSame(oldItem: Int, newItem: Int) = oldItem == newItem
        override fun areContentsTheSame(oldItem: Int, newItem: Int) = oldItem == newItem
    }
}