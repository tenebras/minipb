package com.github.tenebras.minipb

import com.github.tenebras.minipb.model.MessageType
import com.github.tenebras.minipb.parsing.ProtoFileParser
import com.github.tenebras.minipb.rendering.ReducingOptions
import com.github.tenebras.minipb.rendering.RenderType
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.File

internal class FileReducerTest {
    @Test
    fun `should add forced type`() {
        val file = ProtoFileParser.parseString("""
          syntax = "proto3";
          package com.example;
          
          service Test {
            rpc Foo(FooRequest) returns (FooResponse);
          }
          
          message FooRequest{}
          message FooResponse{}

          message ForcedType {
            string test = 1;
          }
        """)

        val (reducedFile, tree) = FileReducer(
            ReducingOptions( forcedTypeNames = listOf("com.example.ForcedType") )
        ).reduce(file)

        assertTrue(reducedFile.hasType<MessageType>("com.example.ForcedType"))
        assertTrue(reducedFile.hasType<MessageType>("com.example.FooRequest"))
        assertTrue(reducedFile.hasType<MessageType>("com.example.FooResponse"))
        assertEquals(
            RenderType.REQUIRED,
            tree.findByTypeNameOrNull("com.example.ForcedType")!!.renderType
        )
        assertEquals(
            RenderType.REQUIRED,
            tree.findByTypeNameOrNull("com.example.FooRequest")!!.renderType
        )
        assertEquals(
            RenderType.REQUIRED,
            tree.findByTypeNameOrNull("com.example.FooResponse")!!.renderType
        )
    }

    @Test
    fun `should separate services from different files`() {
        val typeResolver = TypeResolver()
        val file = ProtoFileParser.parseFile(File("./test.proto"), typeResolver)

        val (reducedFile, _) = FileReducer(ReducingOptions()).reduce(file)

        assertTrue(reducedFile.hasService("Test"))
        assertFalse(reducedFile.hasService("Test1"))
    }
}