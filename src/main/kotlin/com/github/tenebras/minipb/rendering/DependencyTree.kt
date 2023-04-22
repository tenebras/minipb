package com.github.tenebras.minipb.rendering

import com.github.tenebras.minipb.TypeResolver
import com.github.tenebras.minipb.model.*

class DependencyTree(val nodes: List<Node>) {

    fun hasType(typeName: String): Boolean = nodes.any { n -> n.findNestedByType(typeName) != null }
    fun findByTypeNameOrNull(typeName: String): Node? = nodes.firstOrNull { it.findNestedByType(typeName) != null }

    companion object {
        fun of(typeResolver: TypeResolver, types: List<Type>): DependencyTree {
            return DependencyTree(dependencyTree(typeResolver, *types.toTypedArray()))
        }

        private fun dependencyTree(typeResolver: TypeResolver, vararg types: Type): List<Node> {
            val tree = Node("root", RenderType.IGNORE, null)

            types.forEach { type ->
                if (type is TypeReference) {
                    error("TypeReference should be resolved for ${type.fullName()} before building dependency tree")
                }

                var parent: Type? = type
                val parents = mutableListOf<Type>()

                while (parent?.packageName != null) {
                    parent = typeResolver.findOrNull(parent.packageName!!)?.also {
                        parents.add(it)
                    }
                }

                parents.apply { sortBy { it.fullName() } }.forEach {
                    tree.addToParent(
                        it.packageName,
                        Node(
                            typeName = it.fullName(),
                            type = it,
                            renderType = RenderType.NAMESPACE
                        )
                    )
                }

                tree.addToParent(
                    type.packageName,
                    Node(
                        typeName = type.fullName(),
                        type = type,
                        renderType = RenderType.REQUIRED
                    )
                )

                if (type is MessageType) {
                    val fieldTypes = type.fields.map {
                        if (it.type is MapType) listOf(it.type.keyType, it.type.valueType)
                        else listOf(it.type)
                    }.flatten()

                    val oneOfTypes = type.oneOfs.map { o -> o.fields.map { it.type } }.flatten()

                    (fieldTypes + oneOfTypes)
                        .filter { it is MessageType || it is EnumType || it is TypeReference }
                        .distinct()
                        .forEach { f ->
                            dependencyTree(typeResolver, f).forEach {
                                tree.addNested(it)
                            }
                        }
                }
            }

            return tree.nested
        }
    }
}