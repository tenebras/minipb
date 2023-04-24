package com.github.tenebras.minipb.model

data class EnumType(
    override val name: String,
    override val packageName: String? = null,
    val values: List<Value> = emptyList(),
    val options: Map<String, Any> = emptyMap(),
    val reserved: Reserved = Reserved()
) : Type {
    fun value(label: String): Value = values.first { it.label == label }
    fun value(number: Int): Value = values.first { it.number == number }

    data class Value (
        val label: String,
        val number: Int,
        val options: Map<String, Any> = emptyMap()
    )
}