package com.example.rankingproject

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.rankingproject.databinding.FragmentGameRankingBinding

class GameRankingFragment : Fragment() {
    private var _binding: FragmentGameRankingBinding? = null
    private val binding get() = _binding!!

    private var listRanking = mutableListOf<String>()
    private var imgRanking: String = ""
    private var isStartRanking = true
    private var indexRanking = 0
    private var startTime: Long = 0
    private var isRandomRanking = false
    private var canReloadRanking = false
    private var lastPositionRanking = -1
    private val processedPositions = mutableSetOf<Int>()
    private val handler by lazy { Handler(Looper.getMainLooper()) }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGameRankingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Example list
        listRanking = mutableListOf("a1", "a2", "a3", "a4", "a5", "a6", "a7", "a8", "a9", "a10")
        setupRcvRanking()
        handleRanking(listRanking)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupRcvRanking() {
        val listItem = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "10")
        val adapter = context?.let { context ->
            RankingAdapter(listItem) { item, numberColum1, tvNumber2, position ->
                if (!canReloadRanking || lastPositionRanking == position) return@RankingAdapter
                lastPositionRanking = position
                handleAnswerRanking()
                if (!isRandomRanking && listRanking.size > 0) {
                    if (processedPositions.contains(position)) return@RankingAdapter
                    processedPositions.add(position)
                    loadImageFromAsset(context, "ranking/$imgRanking.webp", item)
                    numberColum1.text = (position + 1).toString()
                    tvNumber2.hide()
                    indexRanking += 1
                    listRanking.remove(imgRanking)
                }
            }
        }
        binding.layoutRankingGame.rcvRanking.layoutManager = LinearLayoutManager(
            requireContext(), LinearLayoutManager.VERTICAL, false
        )
        binding.layoutRankingGame.rcvRanking.adapter = adapter
    }

    private fun handleRanking(data: List<String>) {
        if (data.isEmpty()) return
        context?.let { context ->
            imgRanking = data.random()
            loadImageFromAsset(
                context, "ranking/$imgRanking.webp", binding.layoutRankingGame.imgRanking
            )
        }
    }

    private fun handleAnswerRanking() {
        if (isStartRanking) return
        canReloadRanking = false
        isRandomRanking = System.currentTimeMillis() - startTime <= 2000
        if (!isRandomRanking) {
            startTime = System.currentTimeMillis()
            val updateInterval = 20L
            val maxDuration = 2000L

            val rankingRunnable = object : Runnable {
                override fun run() {
                    val elapsedTime = System.currentTimeMillis() - startTime
                    if (elapsedTime < maxDuration) {
                        handleRanking(listRanking)
                        handler.postDelayed(this, updateInterval)
                    } else {
                        canReloadRanking = true
                        handler.removeCallbacks(this)
                    }
                }
            }
            handler.removeCallbacks(rankingRunnable)
            handler.post(rankingRunnable)
        }
    }

    private fun startRanking() {
        if (!isStartRanking) return
        isRandomRanking = System.currentTimeMillis() - startTime <= 2000
        isStartRanking = false

        if (!isRandomRanking) {
            startTime = System.currentTimeMillis()
            val updateInterval = 20L
            val maxDuration = 2000L

            val rankingRunnable = object : Runnable {
                override fun run() {
                    val elapsedTime = System.currentTimeMillis() - startTime
                    if (elapsedTime < maxDuration) {
                        handleRanking(listRanking)
                        handler.postDelayed(this, updateInterval)
                    } else {
                        canReloadRanking = true
                        handler.removeCallbacks(this)
                    }
                }
            }
            handler.removeCallbacks(rankingRunnable)
            handler.post(rankingRunnable)
        }
    }
}