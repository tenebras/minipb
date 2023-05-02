package com.github.tenebras.minipb.model

data class MessageType(
    override val name: String,
    override val packageName: String? = null,
    val reserved: Reserved = Reserved(),
    val fields: List<Field> = emptyList(),
    val oneOfs: List<OneOf> = emptyList(),
    val options: Map<String, Any> = emptyMap(),
    val comments:List<Comment> = emptyList()
) : Type {
    private val allFields by lazy { fields + oneOfs.map { it.fields }.flatten() }

    fun field(name: String): Field = allFields.first { it.name == name }
    fun field(number: Int): Field = allFields.first { it.number == number }

    fun hasOneOf(name: String): Boolean = oneOfs.any { it.name == name }
    fun oneOf(name: String): OneOf = oneOfs.first { it.name == name }

    fun hasTypeReferences(): Boolean = allFields
        .any {
            it.type is TypeReference ||
                (it.type is MapType && (it.type.keyType is TypeReference || it.type.valueType is TypeReference)) ||
                (it.type is MessageType && it.type.hasTypeReferences())
        }
}
