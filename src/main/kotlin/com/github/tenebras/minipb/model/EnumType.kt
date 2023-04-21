package com.github.tenebras.minipb.model

data class EnumType(
    override val name: String,
    override val packageName: String?,
    val values: List<EnumValue>,
    val options: Map<String, Any>,
    val reserved: Reserved
) : Type {
    fun value(label: String): EnumValue = values.first { it.label == label }
    fun value(number: Int): EnumValue = values.first { it.number == number }

    data class EnumValue (
        val label: String,
        val number: Int,
        val options: Map<String, Any>
    )
}