package com.github.tenebras.minipb.model

data class MapType(
    val keyType: Type,
    val valueType: Type
) : Type {
    override val name: String = "map<${keyType.name}, ${valueType.fullName()}>"
    override val packageName: String? = null

    fun hasTypeReferences(): Boolean = keyType is TypeReference || valueType is TypeReference
}