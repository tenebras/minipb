package com.github.tenebras.minipb.model

data class OneOf(
    val name: String,
    val fields: List<Field>,
    val options: Map<String, Any> = emptyMap(),
    val comments: List<Comment> = emptyList()
) {
    fun field(name: String): Field = fields.first { it.name == name }
}