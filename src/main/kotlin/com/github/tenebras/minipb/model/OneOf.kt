package com.github.tenebras.minipb.model

data class OneOf(
    val name: String,
    val fields: List<Field>,
    val options: Map<String, Any>
) {
    fun hasField(name: String): Boolean = fields.any { it.name == name }
    fun hasField(number: Int): Boolean = fields.any { it.number == number }
    fun field(name: String): Field = fields.first { it.name == name }
    fun field(number: Int): Field = fields.first { it.number == number }
}