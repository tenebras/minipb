package com.github.tenebras.minipb.model

data class Field(
    val name: String,
    val type: Type,
    val number: Int,
    val isRepeated: Boolean = false,
    val isOptional: Boolean = false,
    val options: Map<String, Any> = emptyMap()
)