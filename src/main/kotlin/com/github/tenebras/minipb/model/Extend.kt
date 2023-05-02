package com.github.tenebras.minipb.model

data class Extend (
    val typeName: String,
    val fields: List<Field>,
    val comments: List<Comment> = emptyList()
)