package com.github.tenebras.minipb.rendering

import com.github.tenebras.minipb.TypeResolver
import com.github.tenebras.minipb.model.*
import com.github.tenebras.minipb.util.reference
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class DependencyTreeTest {
    @Test
    fun `should create dummy parent classes to preserve namespacing`() {
        val typeResolver = TypeResolver()

        with(typeResolver) {
            add(MessageType("Foo", fields = listOf(
                Field("fooString", stringType, 1),
                Field("fooInt", int32Type, 2)
            )))
            add(MessageType("Bar", "Foo"))
            add(MessageType("Baz", "Foo.Bar", fields = listOf(
                Field("string", stringType, 1),
                Field("int", int32Type, 2)
            )))
        }

        val (foo, bar, baz) = DependencyTree.of(typeResolver, listOf(typeResolver.findOrNull("Foo.Bar.Baz")!!)).let {
            assertTrue(it.hasType("Foo.Bar.Baz"))

            listOf(it.nodes[0], it.nodes[0].nested[0], it.nodes[0].nested[0].nested[0])
        }

        assertEquals(RenderType.NAMESPACE, foo.renderType)
        assertEquals("Foo", foo.typeName)
        assertEquals(typeResolver.findOrNull("Foo"), foo.type)

        assertEquals(RenderType.NAMESPACE, bar.renderType)
        assertEquals("Foo.Bar", bar.typeName)
        assertEquals(typeResolver.findOrNull("Foo.Bar"), bar.type)

        assertEquals(RenderType.REQUIRED, baz.renderType)
        assertEquals("Foo.Bar.Baz", baz.typeName)
        assertEquals(typeResolver.findOrNull("Foo.Bar.Baz"), baz.type)
    }

    @Test
    fun `should fail when TypeReference used to build tree`() {
        assertThrows<IllegalStateException> {
            DependencyTree.of(TypeResolver(), listOf(reference("Test")))
        }

        assertThrows<IllegalStateException> {
            val typeResolver = TypeResolver().apply {
                add(MessageType("Foo", fields = listOf(
                    Field("fooString", stringType, 1),
                    Field("fooInt", int32Type, 2),
                    Field("reference", TypeReference("Bar", "Foo"), 3)
                )))
            }

            DependencyTree.of(typeResolver, listOf(typeResolver.findOrNull("Foo")!!))
        }
    }

    @Test
    fun `should respect oneof and map fields`() {
        val typeResolver = TypeResolver()

        with(typeResolver) {
            val testEnum = EnumType("TestEnum")
            val fooType = MessageType(
                name = "Foo",
                fields = listOf(
                    Field("fooString", stringType, 1),
                    Field("fooInt", int32Type, 2)
                ),
                oneOfs = listOf(
                    OneOf(
                        "Test",
                        fields = listOf(
                            Field("testEnum", testEnum, 1),
                        )
                    )
                )
            )

            add(fooType)
            add(MessageType("Bar", "Foo"))
            add(MessageType("Baz", "Foo.Bar", fields = listOf(
                Field("string", stringType, 1),
                Field("int", int32Type, 2),
                Field("testEnum", testEnum, 3),
                Field("testMap", MapType(stringType, fooType), 4)
            )))
            add(EnumType("TestEnum"))
            add(MessageType("UselessMessage"))
        }

        val tree = DependencyTree.of(typeResolver, listOf(typeResolver.findOrNull("Foo.Bar.Baz")!!))
        assertTrue(tree.hasType("Foo.Bar.Baz"))
        assertFalse(tree.hasType("UselessMessage"))

        val (foo, bar, baz, testEnum) = tree.let {
            listOf(it.nodes[0], it.nodes[0].nested[0], it.nodes[0].nested[0].nested[0], it.nodes[1])
        }

        assertEquals(RenderType.REQUIRED, foo.renderType)
        assertEquals("Foo", foo.typeName)
        assertEquals(typeResolver.findOrNull("Foo"), foo.type)

        assertEquals(RenderType.NAMESPACE, bar.renderType)
        assertEquals("Foo.Bar", bar.typeName)
        assertEquals(typeResolver.findOrNull("Foo.Bar"), bar.type)

        assertEquals(RenderType.REQUIRED, baz.renderType)
        assertEquals("Foo.Bar.Baz", baz.typeName)
        assertEquals(typeResolver.findOrNull("Foo.Bar.Baz"), baz.type)

        assertEquals(RenderType.REQUIRED, testEnum.renderType)
        assertEquals("TestEnum", testEnum.typeName)
        assertEquals(typeResolver.findOrNull("TestEnum"), testEnum.type)
    }
}