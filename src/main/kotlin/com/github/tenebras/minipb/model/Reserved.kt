package com.github.tenebras.minipb.model

// todo should be immutable
data class Reserved(
    val names: MutableList<String> = mutableListOf(),
    val numbers: MutableList<Int> = mutableListOf(),
    val ranges: MutableList<IntRange> = mutableListOf()
) {
    fun add(name: String) {
        names.add(name)
    }

    fun add(number: Int) {
        numbers.add(number)
    }

    fun add(range: IntRange) {
        ranges.add(range)
    }

    fun add(reserved: Reserved) {
        names.addAll(reserved.names)
        numbers.addAll(reserved.numbers)
        ranges.addAll(reserved.ranges)
    }
}