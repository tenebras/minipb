package com.github.tenebras.minipb

import com.github.tenebras.minipb.model.*

class TypeResolver {
    private val types = mutableMapOf<String, Type>()
    private val references = mutableMapOf<String, MutableList<TypeReference>>()

    init {
        types[doubleType.name] = doubleType
        types[floatType.name] = floatType
        types[int32Type.name] = int32Type
        types[int64Type.name] = int64Type
        types[uint32Type.name] = uint32Type
        types[uint64Type.name] = uint64Type
        types[sint32Type.name] = sint32Type
        types[sint64Type.name] = sint64Type
        types[fixed32Type.name] = fixed32Type
        types[fixed64Type.name] = fixed64Type
        types[sfixed32Type.name] = sfixed32Type
        types[sfixed64Type.name] = sfixed64Type
        types[boolType.name] = boolType
        types[stringType.name] = stringType
        types[bytesType.name] = bytesType
    }

    fun add(type: Type) {
        if (type is TypeReference || type is BuiltInType || type is MapType) {
            error(
                "Only MessageType or EnumType could be added to TypeResolver. " +
                "'${type.fullName()}' has type ${type::class.simpleName}"
            )
        }

        types[type.fullName()] = if ((type as? MessageType)?.hasTypeReferences() == true) {
            // Because of lazy type resolving even builtin types are references when type added.
            // This step is required to resolve builtin and already added types
            type.copyWithResolvedReferences()
        } else type

        if ((types[type.fullName()] as? MessageType)?.hasTypeReferences() == true) {
            computeReferences(types[type.fullName()] as MessageType)
        } else {
            updateReferences(types[type.fullName()]!!)
        }
    }

    private fun computeReferences(currentType: MessageType) {
        references.compute(currentType.fullName()) { _, list ->
            (list ?: mutableListOf()).apply {
                (currentType.fields + currentType.oneOfs.map { it.fields }.flatten())
                    .map {
                        if (it.type is TypeReference) {
                            listOf(it.type)
                        } else if (it.type is MapType && it.type.hasTypeReferences()) {
                            listOfNotNull(
                                it.type.keyType.takeIf { t -> t is TypeReference } as? TypeReference,
                                it.type.valueType.takeIf { t -> t is TypeReference } as? TypeReference
                            )
                        } else {
                            emptyList()
                        }
                    }
                    .flatten()
                    .distinct()
                    .forEach {
                        if (!contains(it)) {
                            add(it)
                        }
                    }
            }
        }
    }

    private fun updateReferences(type: Type) {
        val resolved = mutableListOf<String>()

        references.filterValues { r -> r.any { it.name == type.name } }.keys.forEach { typeName ->
            types[typeName] = (types[typeName] as MessageType).copyWithResolvedReferences().also {
                if (!it.hasTypeReferences()) {
                    resolved.add(typeName)
                }
            }
        }

        resolved.forEach {
            references.remove(it)
            updateReferences(types[it]!!)
        }
    }

    private fun MessageType.copyWithResolvedReferences(): MessageType {
        return copy(
            fields = fields.map { field -> field.copyWithResolvedReferences(fullName()) },
            oneOfs = oneOfs.map { oneOf ->
                oneOf.copy(
                    fields = oneOf.fields.map { field -> field.copyWithResolvedReferences(fullName()) }
                )
            }
        )
    }

    private fun Field.copyWithResolvedReferences(parentTypeName: String): Field {
        return if (type is TypeReference) {
            val newType = findOrNull(type.name, type.packageName)

            if (newType == null || (newType is MessageType && newType.hasTypeReferences())) {
                this
            } else {
                references.computeIfPresent(parentTypeName) { _, list ->
                    list.filter { it != type }.toMutableList()
                }

                copy(type = newType)
            }
        } else if (type is MapType && type.hasTypeReferences()) {
            val keyType = find(type.keyType.name, type.keyType.packageName)
            val valueType = find(type.valueType.name, type.valueType.packageName)

            if (keyType !is TypeReference && valueType !is TypeReference) {
                references.computeIfPresent(parentTypeName) { _, list ->
                    list
                        .filter { it != type.keyType && it != type.valueType }
                        .toMutableList()
                }
            }

            copy(type = MapType(keyType, valueType))
        } else {
            this
        }
    }

    fun find(name: String, context: String?): Type = findOrNull(name, context) ?: TypeReference(name, context)

    fun findOrNull(fullName: String): Type? = types[fullName]

    private fun findOrNull(name: String, context: String?): Type? {
        var type: Type? = types[fullTypeName(name, context)] ?: types[name]

        if (type == null) {
            var typeName = name
            val packageName = context?.split('.') ?: emptyList()
            val subPackage = mutableListOf<String>()

            if (name.contains('.')) {
                val (normalizedName, subPkg) = name.split('.').let {
                    it.last() to it.dropLast(1)
                }

                typeName = normalizedName
                subPackage.addAll(subPkg)
            }

            for (size in (0..packageName.size)) {
                types[
                    fullTypeName(typeName, (packageName.dropLast(size) + subPackage).joinToString("."))
                ]?.let { type = it }

                if (type != null) {
                    break
                }
            }
        }

        return type
    }

    private fun fullTypeName(name: String, context: String?): String
        = context?.let { "$context." }.orEmpty() + name
}