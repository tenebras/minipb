package com.github.tenebras.minipb.model

data class Reserved(
    val names: List<String> = emptyList(),
    val numbers: List<Int> = emptyList(),
    val ranges: List<IntRange> = emptyList()
)