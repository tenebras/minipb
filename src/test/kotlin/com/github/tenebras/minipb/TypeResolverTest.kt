package com.github.tenebras.minipb

import com.github.tenebras.minipb.model.*
import com.github.tenebras.minipb.util.message
import com.github.tenebras.minipb.util.reference
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class TypeResolverTest {
    @Test
    fun `should handle last type properly`() {
        val resolver = TypeResolver()

        with (resolver) {
            add(message("Foo", fields = listOf(Field("test", reference("Bar"), 1))))
            add(message("Bar", fields = listOf(Field("test", reference("Baz"), 1))))
            add(message("Baz"))
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
            add(message("Bar", "Foo"))
            add(message("Baz", "Foo"))
            add(message("Foo", fields = listOf(
                Field("bar", reference("Bar", "Foo"), 1),
                Field("baz", reference("Baz", "Foo"), 2),
                Field("test", reference("Test"), 3)
            )))
            add(message("Test"))
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