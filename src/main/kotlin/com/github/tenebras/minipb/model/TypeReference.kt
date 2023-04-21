package com.github.tenebras.minipb.model

data class TypeReference(
    override val name: String,
    override val packageName: String? = null
) : Type