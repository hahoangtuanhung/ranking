package com.example.rankingproject

data class Game (
    var id: String = "",
    val categoryItem: String = "",
    val name: String = "",
    val type: String = "",
    val category: String = "",
    val questions: List<Question> = listOf(),
    val thumb: String = "",
    val homeThumb: String = "",
    val isFamous: Boolean = false,
)