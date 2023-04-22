package com.github.tenebras.minipb

import com.github.tenebras.minipb.parsing.tokenizeProto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class TokenizeProtoKtTest {

    @Test
    fun `should ignore comments`() {
        val tokens = tokenizeProto(
            """
            // single line comment
            syntax = "proto3"; // endline comment
            // yet another single line
            /*
            Multiline comment
            */
            package com.kostynenko.sample.api;
            
            option java_multiple_files = true; 
            """
        )

        assertEquals(
            listOf(
                "syntax", "=", "\"proto3\"", ";", "package", "com.kostynenko.sample.api", ";", "option",
                "java_multiple_files", "=", "true", ";"
            ),
            tokens
        )
    }
}