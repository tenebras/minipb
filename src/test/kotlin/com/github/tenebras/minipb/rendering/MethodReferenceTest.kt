package com.github.tenebras.minipb.rendering

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

internal class MethodReferenceTest {
    @Test
    fun `should parse input`() {
        with(MethodReference.of("Service.Method")) {
            assertEquals("Service", serviceName)
            assertEquals("Method", methodName)
        }

        with(MethodReference.of("Method")) {
            assertNull(serviceName)
            assertEquals("Method", methodName)
        }
    }
}