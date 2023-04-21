package com.github.tenebras.minipb.model

data class MessageType(
    override val name: String,
    override val packageName: String? = null,
    val reserved: Reserved = Reserved(),
    val fields: List<Field> = emptyList(),
    val oneOfs: List<OneOf> = emptyList(),
    val options: Map<String, Any> = emptyMap()
) : Type {
    fun hasField(name: String): Boolean = fields.any { it.name == name }
    fun hasField(number: Int): Boolean = fields.any { it.number == number }
    fun field(name: String): Field = fields.first { it.name == name }
    fun field(number: Int): Field = fields.first { it.number == number }

    fun hasOneOf(name: String): Boolean = oneOfs.any { it.name == name }
    fun oneOf(name: String): OneOf = oneOfs.first { it.name == name }

    fun hasTypeReferences(): Boolean = (fields + oneOfs.map { it.fields }.flatten())
        .any {
            it.type is TypeReference ||
            (it.type is MapType && (it.type.keyType is TypeReference || it.type.valueType is TypeReference)) ||
            (it.type is MessageType && it.type.hasTypeReferences())
        }
}
