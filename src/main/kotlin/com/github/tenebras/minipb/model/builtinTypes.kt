package com.github.tenebras.minipb.model

interface BuiltInType: Type

val doubleType = object : BuiltInType {
    override val name: String = "double"
    override val packageName: String? = null
}

val floatType = object : BuiltInType {
    override val name: String = "float"
    override val packageName: String? = null
}

val int32Type = object : BuiltInType {
    override val name: String = "int32"
    override val packageName: String? = null
}

val int64Type = object : BuiltInType {
    override val name: String = "int64"
    override val packageName: String? = null
}

val uint32Type = object : BuiltInType {
    override val name: String = "uint32"
    override val packageName: String? = null
}

val uint64Type = object : BuiltInType {
    override val name: String = "uint64"
    override val packageName: String? = null
}

val sint32Type = object : BuiltInType {
    override val name: String = "sint32"
    override val packageName: String? = null
}

val sint64Type = object : BuiltInType {
    override val name: String = "sint64"
    override val packageName: String? = null
}

val fixed32Type = object : BuiltInType {
    override val name: String = "fixed32"
    override val packageName: String? = null
}

val fixed64Type = object : BuiltInType {
    override val name: String = "fixed64"
    override val packageName: String? = null
}

val sfixed32Type = object : BuiltInType {
    override val name: String = "sfixed32"
    override val packageName: String? = null
}

val sfixed64Type = object : BuiltInType {
    override val name: String = "sfixed64"
    override val packageName: String? = null
}

val boolType = object : BuiltInType {
    override val name: String = "bool"
    override val packageName: String? = null
}

val stringType = object : BuiltInType {
    override val name: String = "string"
    override val packageName: String? = null
}

val bytesType = object : BuiltInType {
    override val name: String = "bytes"
    override val packageName: String? = null
}