package com.github.tenebras.minipb.rendering

import com.github.tenebras.minipb.model.*
import com.github.tenebras.minipb.parsing.ProtoFileParser
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class RendererTest {
    @Test
    fun `should render empty file`() {
        val file = ProtoFileParser.parseString("")
        val result = Renderer().render(file, DependencyTree(emptyList()))

        assertEquals("", result)
    }

    @Test
    fun `should render minimals`() {
        val file = ProtoFile(
            typeResolver = mockk(),
            syntax = "\"proto3\"",
            packageName = "com.tenebras",
            imports = listOf(
                Import(Import.ImportType.PUBLIC, "public.proto", mockk()),
                Import(Import.ImportType.DEFAULT, "default.proto", mockk()),
            ),
            options = mapOf(
                "test" to "value"
            ),
            extends = listOf(
                Extend(
                    "google.protobuf.FileOptions",
                    fields = listOf(
                        Field("test", stringType, 1)
                    )
                )
            ),
            services = listOf(
                Service(
                    name = "Test",
                    methods = listOf(
                        Method(
                            name = "Foo",
                            request = MessageType("FooRequest", "com.tenebras"),
                            response = MessageType("FooResponse", "com.tenebras"),
                            options = mapOf(
                                "deadline" to 100
                            )
                        )
                    )
                )
            ),
            types = listOf(
                MessageType("FooRequest", "com.tenebras", fields = listOf(
                    Field("test", stringType, 1, isOptional = true)
                )),
                MessageType("FooResponse", "com.tenebras", fields = listOf(
                    Field("bar", int32Type, 1, isRepeated = true),
                    Field("mapType", MapType(stringType, stringType), 2)
                ))
            )
        )

        val tree = DependencyTree(listOf(
            Node(
                typeName = "com.tenebras.FooRequest",
                renderType = RenderType.REQUIRED,
                type = file.type("FooRequest", "com.tenebras"),
                nested = mutableListOf()
            ),
            Node(
                typeName = "com.tenebras.FooResponse",
                renderType = RenderType.REQUIRED,
                type = file.type("FooResponse", "com.tenebras"),
                nested = mutableListOf()
            ),
        ))

        val result = Renderer().render(file, tree)
        assertEquals("""syntax = "proto3";

package com.tenebras;

import public "public.proto";
import "default.proto";

option test = value;

extend google.protobuf.FileOptions {
    string test = 1;
}

service Test {
    rpc Foo (FooRequest) returns (FooResponse) {
        option deadline = 100;
    }
}

message FooRequest {
    optional string test = 1;
}

message FooResponse {
    repeated int32 bar = 1;
    map<string, string> mapType = 2;
}
""",
        result
        )
    }
}