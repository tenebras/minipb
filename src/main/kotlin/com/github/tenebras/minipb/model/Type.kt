package com.github.tenebras.minipb.model

interface Type {
    val name: String
    val packageName: String?

    fun fullName(): String = packageName?.let { "$packageName." }.orEmpty() + name
}
