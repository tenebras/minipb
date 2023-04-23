package com.github.tenebras.minipb.model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class MessageTypeTest {
    @Test
    fun `should find field by name or number`() {
        val message = MessageType(
            name = "Foo",
            fields = listOf(
                Field("foo", stringType, 1),
                Field("bar", stringType, 2)
            ),
            oneOfs = listOf(
                OneOf(
                    name = "ONE",
                    fields = listOf(
                        Field("baz", int32Type, 3)
                    )
                )
            )
        )

        assertEquals(1, message.field("foo").number)
        assertEquals(2, message.field("bar").number)
        assertEquals(3, message.field("baz").number)

        assertEquals("foo", message.field(1).name)
        assertEquals("bar", message.field(2).name)
        assertEquals("baz", message.field(3).name)

        assertTrue(message.hasOneOf("ONE"))
        assertFalse(message.hasOneOf("test"))

        assertThrows<NoSuchElementException> { message.field("any") }
        assertThrows<NoSuchElementException> { message.field(4) }
    }
}