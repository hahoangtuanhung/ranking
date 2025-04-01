package com.example.rankingproject

class TriangleData {
    private val triangleIndices = listOf(
        listOf(9, 67, 297),
        listOf(9, 67, 221),
        listOf(9, 441, 297),
        listOf(441, 284, 297),
        listOf(441, 284, 359),
        listOf(221, 67, 54),
        listOf(221, 130, 54),
        listOf(9, 221, 168),
        listOf(441, 221, 168),
        listOf(130, 221, 232),
        listOf(168, 221, 232),
        listOf(168, 195, 232),
        listOf(168, 195, 452),
        listOf(168, 441, 452),
        listOf(359, 441, 452),
        listOf(195, 294, 452),
        listOf(195, 294, 64),
        listOf(195, 232, 64),
        listOf(164, 294, 64),
        listOf(54, 130, 93),
        listOf(284, 359, 366),
        listOf(164, 57, 64),
        listOf(164, 287, 294),
        listOf(93, 136, 57),
        listOf(18, 136, 57),
        listOf(18, 287, 365),
        listOf(366, 287, 365),
        listOf(136, 152, 365),
    )

    fun getTriangleIndices(): List<List<Int>> {
        return triangleIndices
    }
}