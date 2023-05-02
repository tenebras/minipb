package com.github.tenebras.minipb.model

data class Comment(
    val type: Type,
    val placement: Placement,
    val value: String
) {
    enum class Type {
        SINGLE_LINE,
        MULTILINE
    }

    enum class Placement {
        TOP,
        BEFORE,
        AFTER
    }
}