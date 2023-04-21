package com.github.tenebras.minipb.model

data class Method (
    val name: String,
    val request: Type,
    val response: Type,
    val isRequestStreamed: Boolean,
    val isResponseStreamed: Boolean
)