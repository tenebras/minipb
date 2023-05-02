package com.github.tenebras.minipb.model

data class Method (
    val name: String,
    val request: Type,
    val response: Type,
    val isRequestStreamed: Boolean = false,
    val isResponseStreamed: Boolean = false,
    val options: Map<String, Any> = emptyMap(),
    val comments: List<Comment> = emptyList()
)