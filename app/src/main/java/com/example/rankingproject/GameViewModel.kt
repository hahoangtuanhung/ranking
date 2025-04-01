package com.example.rankingproject

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.io.File

class GameViewModel constructor() : ViewModel() {
    val tempFile = MutableLiveData<File?>().apply { value = null }
    val videoURL = MutableLiveData<String?>().apply { value = null }
    val fileSaved = MutableLiveData<File?>().apply { value = null }
    var game = MutableLiveData<Game>()
    val ranking1Game = Game(
        id = "ranking1",
        categoryItem = "Female Actress",
        name = "Ranking",
        type = "Ranking",
        category = "Ranking",
        questions = listOf(
            Question(
                question = "A1,A2,A3,A4,A5,A6,A7,A8,A9,A10",
                trueAnswer = "",
                falseAnswer = ""
            )
        ),
        thumb = "ic_female_ac_category",
        isFamous = false
    )

    val actionStartGame = SingleLiveData<Unit?>()
    val actionStopGame = SingleLiveData<Unit?>()

    private val _startRankingGame = SingleLiveData<Unit>()
    val startRankingGame: LiveData<Unit> get() = _startRankingGame

    fun setStartRankingGame() {
        _startRankingGame.postValue(Unit)
    }

    fun setGame(){
        game.value = ranking1Game
    }
}