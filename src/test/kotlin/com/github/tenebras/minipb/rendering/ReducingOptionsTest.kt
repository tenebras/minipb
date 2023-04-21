package com.github.tenebras.minipb.rendering

import com.github.tenebras.minipb.rendering.MethodReference
import com.github.tenebras.minipb.rendering.ReducingOptions
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class ReducingOptionsTest {

    @Test
    fun `it should allow any by default`() {
        val options = ReducingOptions()

        assertTrue(options.isServiceAllowed("Test"))
        assertTrue(options.isMethodAllowed("Test", "Get"))
    }

    @Test
    fun `it should allow only included`() {
        val options = ReducingOptions(
            includeServices = listOf("Test"),
            includeMethods = listOf(
                MethodReference("Get", null),
                MethodReference.of("Test.Set"),
                MethodReference.of("Other.Get"), // misconfiguration case
            )
        )

        assertTrue(options.isServiceAllowed("Test"))
        assertFalse(options.isServiceAllowed("Other"))

        assertTrue(options.isMethodAllowed("Test", "Get"))
        assertTrue(options.isMethodAllowed("Test", "Set"))
        assertTrue(options.isMethodAllowed("Other", "Get"))
        assertTrue(options.isMethodAllowed("Foo", "Get"))
        assertFalse(options.isMethodAllowed("Foo", "Set"))
    }
}