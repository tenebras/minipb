package com.github.tenebras.minipb.model

data class Service (
    val name: String,
    val methods: List<Method>,
    val packageName: String? = null,
    val comments: List<Comment> = emptyList()
) {
    fun method(name: String): Method = methods.first { it.name == name }
    fun hasMethod(name: String): Boolean = methods.any { it.name == name }
}