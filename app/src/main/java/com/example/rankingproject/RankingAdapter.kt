package com.example.rankingproject

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.rankingproject.databinding.ItemRankingGameBinding

class RankingAdapter(
    private val items: List<String>,
    private val onItemClick: (ImageView, TextView, TextView, Int) -> Unit
) : RecyclerView.Adapter<RankingAdapter.RankingViewHolder>() {
    inner class RankingViewHolder(private val binding: ItemRankingGameBinding) :
        RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n", "SuspiciousIndentation")
        fun bind(text: String, position: Int) {
            binding.tvNumOfRanking.text = text
            binding.numOfRanking.setSingleClick {
                onItemClick(binding.numOfRanking, binding.numOfRankingSelected, binding.tvNumOfRanking ,position)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RankingViewHolder {
        val binding = ItemRankingGameBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RankingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RankingViewHolder, position: Int) {
        holder.bind(items[position], position)
    }

    override fun getItemCount(): Int {
        return items.size
    }

}