package com.github.tenebras.minipb.rendering

import com.github.tenebras.minipb.model.Type

data  class Node(
    val typeName: String,
    val renderType: RenderType,
    val type: Type?,
    val nested: MutableList<Node> = mutableListOf(),
) {
    fun addToParent(parentTypeName: String?, node: Node) {
        val parent = parentTypeName?.let { findNestedByType(it) }

        if (parent != null) {
            parent.addNested(node)
        } else {
            addNested(node)
        }
    }

    fun addNested(node: Node) {
        val idx = nested.indexOfFirst { it.typeName == node.typeName }

        if (idx == -1) {
            nested.add(node)
        } else if (nested[idx].renderType.ordinal < node.renderType.ordinal) {
            nested[idx] = node.also { n ->
                nested[idx].nested.forEach {
                    n.addNested(it)
                }
            }
        } else {
            node.nested.forEach {
                nested[idx].addNested(it)
            }
        }
    }

    fun findNestedByType(name: String): Node? {
        if (typeName == name) {
            return this
        }

        return nested.mapNotNull {
            if (it.typeName == name) {
                it
            } else if (name.startsWith(it.typeName)) {
                it.findNestedByType(name)
            } else {
                null
            }
        }.firstOrNull()
    }
}