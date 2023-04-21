package com.github.tenebras.minipb

import com.github.tenebras.minipb.model.Field
import com.github.tenebras.minipb.model.MessageType
import com.github.tenebras.minipb.model.TypeReference
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class TypeResolverTest {
    @Test
    fun `should handle last type properly`() {
        val resolver = TypeResolver()

        with (resolver) {
            add(mt("Foo", fields = listOf(Field("test", tr("Bar"), 1))))
            add(mt("Bar", fields = listOf(Field("test", tr("Baz"), 1))))
            add(mt("Baz"))
        }

        assertEquals(
            resolver.findOrNull("Bar"),
            (resolver.findOrNull("Foo") as MessageType).fields.first().type
        )
    }

    @Test
    fun `should resolve previously added types`() {
        val resolver = TypeResolver()

        with(resolver) {
            add(mt("Bar", "Foo"))
            add(mt("Baz", "Foo"))
            add(mt("Foo", fields = listOf(
                Field("bar", tr("Bar", "Foo"), 1),
                Field("baz", tr("Baz", "Foo"), 2),
                Field("test", tr("Test"), 3)
            )))
            add(mt("Test"))
        }

        val foo = resolver.findOrNull("Foo") as MessageType

        assertEquals(
            resolver.findOrNull("Foo.Bar"),
            foo.field(1).type
        )

        assertEquals(
            resolver.findOrNull("Foo.Baz"),
            foo.field(2).type
        )
    }

    private fun tr(name: String, packageName: String? = null)
        = TypeReference(name, packageName)
    private fun mt(name: String, packageName: String? = null, fields: List<Field> = emptyList())
        = MessageType(name, packageName, fields = fields)
}