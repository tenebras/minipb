package com.github.tenebras.minipb.util

import com.github.tenebras.minipb.model.*

fun reference(name: String, packageName: String? = null)
    = TypeReference(name, packageName)

fun message(
    name: String,
    packageName: String? = null,
    fields: List<Field> = emptyList(),
    oneOf: List<OneOf> = emptyList()
) = MessageType(name, packageName, fields = fields)

fun enum(
    name: String,
    packageName: String? = null,
    values: List<EnumType.Value> = emptyList()
) = EnumType(name, packageName, values = values)