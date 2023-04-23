package com.github.tenebras.minipb

import com.github.tenebras.minipb.model.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class TypeResolverTest {
    @Test
    fun `should handle last type properly`() {
        val resolver = TypeResolver()

        with (resolver) {
            add(MessageType("Foo", fields = listOf(Field("test", TypeReference("Bar"), 1))))
            add(MessageType("Bar", fields = listOf(Field("test", TypeReference("Baz"), 1))))
            add(MessageType("Baz"))
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
            add(MessageType("Bar", "Foo", fields = listOf(
                Field("map", MapType(TypeReference("Baz", "Foo"), TypeReference("Bar", "Foo")), 4),
                Field("map1", MapType(TypeReference("Baz", "Foo"), stringType), 5),
                Field("map2", MapType(stringType, TypeReference("Baz", "Foo")), 6),
                Field("map3", MapType(stringType, stringType), 7)
            )))
            add(MessageType("Baz", "Foo"))
            add(MessageType("Foo", fields = listOf(
                Field("bar", TypeReference("Bar", "Foo"), 1),
                Field("baz", TypeReference("Baz", "Foo"), 2),
                Field("test", TypeReference("Test"), 3)
            )))
            add(MessageType("Test"))
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

    @Test
    fun `should fail on non user defined types`() {
        val resolver = TypeResolver()

        assertThrows<IllegalStateException> {
            resolver.add(TypeReference("test"))
        }

        assertThrows<IllegalStateException> {
            resolver.add(stringType)
        }

        assertThrows<IllegalStateException> {
            resolver.add(MapType(stringType, stringType))
        }
    }
}