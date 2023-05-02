package com.github.tenebras.minipb

import com.github.tenebras.minipb.model.*
import com.github.tenebras.minipb.parsing.ProtoFileParser
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.File

internal class ProtoFileParserTest {

    @Test
    fun `should read syntax package and options`() {
        val file = ProtoFileParser.parseString(
            """
            syntax = "proto3";
            
            package com.kostynenko.sample.api;
            
            option java_multiple_files = true;
            option java_package = "com.kostynenko.sample.api";
            option go_package = "github.com/tenebras/com.kostynenko.sample.api/proto/src;proto";
            
            message Test {
                option (my_option).a = true;
                
                string package = 1;
                string syntax = 2;
                string option = 3;
            }
        """
        )

        assertEquals("\"proto3\"", file.syntax)
        assertEquals("com.kostynenko.sample.api", file.packageName)

        assertEquals(3, file.options.size)
        assertEquals("true", file.options["java_multiple_files"])
        assertEquals("\"com.kostynenko.sample.api\"", file.options["java_package"])
        assertEquals("\"github.com/tenebras/com.kostynenko.sample.api/proto/src;proto\"", file.options["go_package"])
    }

    @Test
    fun `should parse reserved`() {
        val file = ProtoFileParser.parseString(
            """
           message Test{
                reserved 8;
                reserved 13, 14;
                reserved 66 to 76;
                reserved 20 to 22, 23 to 25;
                reserved 2, 15, 9 to 11, 80 to max;
                reserved "foo", "bar";
                reserved "too";
            } 
            
            enum Test1 {
                DEFAULT = 0;
                reserved 8;
                reserved 13, 14;
                reserved 66 to 76;
                reserved 20 to 22, 23 to 25;
                reserved 2, 15, 9 to 11, 80 to max;
                reserved "foo", "bar";
                reserved "too";
            }
        """
        )

        with(file.type<MessageType>("Test").reserved) {
            assertEquals(listOf("foo", "bar", "too"), names)
            assertEquals(listOf(8, 13, 14, 2, 15), numbers)
            assertEquals(listOf(66..76, 20..22, 23..25, 9..11, 80..536_870_911), ranges)
        }

        with(file.type<EnumType>("Test1").reserved) {
            assertEquals(listOf("foo", "bar", "too"), names)
            assertEquals(listOf(8, 13, 14, 2, 15), numbers)
            assertEquals(listOf(66..76, 20..22, 23..25, 9..11, 80..536_870_911), ranges)
        }
    }

    @Test
    fun `should parse enum`() {
        val enum = ProtoFileParser.parseString(
            """
            enum Test {
                option allow_alias = true;
                DEFAULT_VALUE = 0;
                VALUE = 1;
                VALUE_1 = 1[(custom_option) = "hello world"];
            }
             
             enum InvalidEmpty{}
        """
        ).type<EnumType>("Test")

        assertEquals(3, enum.values.size)
        assertEquals("true", enum.options["allow_alias"])
        assertEquals(0, enum.value("DEFAULT_VALUE").number)
        assertEquals(1, enum.value("VALUE").number)

        with (enum.value("VALUE_1")) {
            assertEquals(1, number)
            assertEquals("\"hello world\"", options["(custom_option)"])
        }
    }

    @Test
    fun `should read services`() {
        val file = ProtoFileParser.parseString(
            """
            syntax = "proto3";
            
            package com.kostynenko.sample.api;
            
            service EmptyService{}
            
            service TestService {
                rpc Method1(Request1) returns (Response1) {}
                rpc Method2(stream Request2) returns (Response2) {};
                rpc Method3(Request3) returns (stream Response3);
                rpc Method4(stream Request4) returns (stream Response4) {}
            }
        """
        )

        assertEquals(2, file.services.size)
        assertTrue(file.hasService("EmptyService"))
        assertTrue(file.hasService("TestService"))

        assertEquals(0, file.service("EmptyService").methods.size)
        assertEquals(4, file.service("TestService").methods.size)

        val testService = file.service("TestService")

        with(testService.method("Method1")) {
            assertEquals("Request1", request.name)
            assertEquals("com.kostynenko.sample.api", request.packageName)
            assertFalse(isRequestStreamed)

            assertEquals("Response1", response.name)
            assertEquals("com.kostynenko.sample.api", response.packageName)
            assertFalse(isResponseStreamed)
        }

        with(testService.method("Method2")) {
            assertEquals("Request2", request.name)
            assertEquals("com.kostynenko.sample.api", request.packageName)
            assertTrue(isRequestStreamed)

            assertEquals("Response2", response.name)
            assertEquals("com.kostynenko.sample.api", response.packageName)
            assertFalse(isResponseStreamed)
        }

        with(testService.method("Method3")) {
            assertEquals("Request3", request.name)
            assertEquals("com.kostynenko.sample.api", request.packageName)
            assertFalse(isRequestStreamed)

            assertEquals("Response3", response.name)
            assertEquals("com.kostynenko.sample.api", response.packageName)
            assertTrue(isResponseStreamed)
        }

        with(testService.method("Method4")) {
            assertEquals("Request4", request.name)
            assertEquals("com.kostynenko.sample.api", request.packageName)
            assertTrue(isRequestStreamed)

            assertEquals("Response4", response.name)
            assertEquals("com.kostynenko.sample.api", response.packageName)
            assertTrue(isResponseStreamed)
        }
    }

    @Test
    fun `should parse complex message with basic fields`() {
        val file = ProtoFileParser.parseString(
            """
            syntax = "proto3";
            
            package com.kostynenko.api;
            
            message Types {
                repeated double doubleValue = 1[packed=false, deprecated = true];
                repeated float floatValue = 2 [packed=false,deprecated = true];
                repeated int32 int32Value = 3;
                optional int64 int64Value = 4;
                uint32 uint32Value = 5;
                uint64 uint64Value = 6;
                sint32 sint32Value = 7;
                sint64 sint64Value = 8;
                fixed32 fixed32Value = 9;
                fixed64 fixed64Value = 10;
                sfixed32 sfixed32Value = 11;
                sfixed64 sfixed64Value = 12;
                bool boolValue = 13;
                string stringValue = 14;
                bytes bytesValue = 15;
                ENUM enumValue = 16;
                SubType subType = 17;
                map<string, int32> mapValue = 18;
            
                message SubType { int32 a = 1; }
                enum ENUM { ZERO = 0; ONE = 1; TWO = 2; THREE = 3;}
            }
        """
        )

        val message = file.type<MessageType>("Types", "com.kostynenko.api")
        assertEquals("Types", message.name)
        assertEquals("com.kostynenko.api", message.packageName)

        with(message.field("doubleValue")) {
            assertEquals(true, isRepeated)
            assertEquals(false, isOptional)
            assertEquals("doubleValue", name)
            assertEquals(1, number)
            assertEquals("false", options["packed"])
            assertEquals("true", options["deprecated"])
            assertEquals(doubleType, type)
        }
    }

    @Test
    fun `should parse embedded message and enum`() {
        val typeResolver = TypeResolver()
        val file = ProtoFileParser.parseString(
            """
            syntax = "proto3"; 
            package com.kostynenko.api;
            
            message Test {
                message One {
                    message Two {
                        message Three {
                          uint32 test_field = 1 [deprecated=true];
                        }
                    }
                }
            
                message Dummy {
                  One.Two.Three message = 1;
                  TestEnum en = 2;
                }
                
                enum TestEnum {
                  DEF = 0;
                }
            }
            
            message One {
              message Two {
                message Three {
                  string test_string_field = 1;
                };
              };
            }
            
            enum RootEnum {
                DEFAULT_VALUE = 0;
                VALUE = 1;
            };
        """,
            typeResolver
        )

        assertTrue(file.hasType<EnumType>("TestEnum", "com.kostynenko.api.Test"))
        assertTrue(file.hasType<MessageType>("Test", "com.kostynenko.api"))
        assertTrue(file.hasType<MessageType>("One", "com.kostynenko.api.Test"))
        assertTrue(file.hasType<MessageType>("Two", "com.kostynenko.api.Test.One"))
        assertTrue(file.hasType<MessageType>("Three", "com.kostynenko.api.Test.One.Two"))
        assertTrue(file.hasType<MessageType>("Dummy", "com.kostynenko.api.Test"))
        assertTrue(file.hasType<EnumType>("TestEnum", "com.kostynenko.api.Test"))
        assertTrue(file.hasType<MessageType>("One", "com.kostynenko.api"))
        assertTrue(file.hasType<MessageType>("Two", "com.kostynenko.api.One"))
        assertTrue(file.hasType<MessageType>("Three", "com.kostynenko.api.One.Two"))
        assertTrue(file.hasType<EnumType>("RootEnum", "com.kostynenko.api"))

        assertEquals(
            file.type<MessageType>("Three", "com.kostynenko.api.Test.One.Two"),
            file.type<MessageType>("Dummy", "com.kostynenko.api.Test").field("message").type
        )
        assertEquals(
            file.type<EnumType>("TestEnum", "com.kostynenko.api.Test"),
            file.type<MessageType>("Dummy", "com.kostynenko.api.Test").field("en").type
        )
    }

    @Test
    fun `should parse oneOf`() {
        val message = ProtoFileParser.parseString(
            """
            syntax = "proto3";
            
            message GetRequest {
                oneof oneof_test{
                    option (my_option.a) = 54321;
                    string id = 1[deprecated=true, packed=false];
                    uint32 number = 2;
                };
            };
        """
        ).type<MessageType>("GetRequest")

        assertTrue(message.hasOneOf("oneof_test"))

        assertEquals("54321", message.oneOf("oneof_test").options["(my_option.a)"])

        with(message.oneOf("oneof_test").field("id")) {
            assertEquals(stringType, type)
            assertEquals(1, number)
            assertEquals("true", options["deprecated"])
            assertEquals("false", options["packed"])
        }

        with(message.oneOf("oneof_test").field("number")) {
            assertEquals(uint32Type, type)
            assertEquals(2, number)
            assertTrue(options.isEmpty())
        }
    }

    @Test
    fun `should parse extend`() {
        val file = ProtoFileParser.parseString("""
            extend google.protobuf.FileOptions {
                optional int32 source_retention_option = 1234
                  [retention = RETENTION_SOURCE];
            }
        """)

        assertEquals(1, file.extends.size)

        with (file.extends.first()) {
            assertEquals("google.protobuf.FileOptions", typeName)
            assertEquals(1, fields.size)

            assertEquals(int32Type, fields.first().type)
            assertEquals("source_retention_option", fields.first().name)
            assertEquals(1234, fields.first().number)

            assertEquals("RETENTION_SOURCE", fields.first().options["retention"])
        }
    }

    @Test
    fun `should process imports`() {
        val typeResolver = TypeResolver()
        val file = ProtoFileParser.parseFile(File("./test.proto"), typeResolver)

        with(file.imports) {
            assertEquals(Import.ImportType.DEFAULT, get(0).type)
            assertEquals("import_test.proto", get(0).path)

            assertEquals(Import.ImportType.WEAK, get(1).type)
            assertEquals("import_test.proto", get(1).path)

            assertEquals(Import.ImportType.PUBLIC, get(2).type)
            assertEquals("second_service_import.proto", get(2).path)
        }
    }

    @Test
    fun `should handle rpc method options`() {
        val file = ProtoFileParser.parseString("""
            syntax = "proto3";
            
            extend google.protobuf.MethodOptions {
                Test msg = 50056;
                int64 deadline = 18;
            }
            
            service Foo {
                rpc Baz(Test) returns (Test) {
                    option (google.api.http) = {
                      post: "/api/bookings/vehicle/{vehicle_id}"
                      body: "*"
                      test: {
                        asd: "asd"
                      }
                    };
                    option deadline = x;
                    option (msg).test = "test";
                }
                rpc Bar(Test) returns (Test){
                    option test = x;
                };
                rpc Empty(Test) returns (Test);
            }
            
            message Test {
                string test = 1 [(validator.field) = {string_not_empty: true, length_lt: 255},deprecated=true];
            }
        """)

        val baz = file.service("Foo").method("Baz")
        val bar = file.service("Foo").method("Bar")
        val empty = file.service("Foo").method("Empty")
        val test = file.type<MessageType>("Test")
        assertEquals("x", baz.options["deadline"])
        assertEquals("\"test\"", baz.options["(msg).test"])
        assertEquals(
            "{\npost:\"/api/bookings/vehicle/{vehicle_id}\"\nbody:\"*\"\ntest:{\nasd:\"asd\"\n}\n}",
            baz.options["(google.api.http)"]
        )
        assertEquals("x", bar.options["test"])
        assertTrue(empty.options.isEmpty())

        assertEquals("{string_not_empty:true,length_lt:255}", test.field("test").options["(validator.field)"])
        assertEquals("true", test.field("test").options["deprecated"])
    }

    @Test
    fun `should fail on invalid token`() {
        assertThrows<IllegalStateException> {
            ProtoFileParser.parseString("""
               syntax = "proto3";
                
               int32 fieldName = 1;
            """)
        }
    }

    @Test
    fun `should parse comments`() {
        // todo enum comments
        val file = ProtoFileParser.parseString("""
            // Protocol Buffers - Google's data interchange format
            // Copyright 2008 Google Inc.  All rights reserved.            
            syntax = "proto3";
            
            // extend comment
            extend google.protobuf.MethodOptions {
                Test msg = 50056;
                int64 deadline = 18;
            }
                        
            /*
              Multiline comment
            */
            // Service definition
              // Offset comment
            service Foo {
                rpc Baz(Test) returns (Test){} // Right comment
                /* left comment*/ rpc Bar(Test) returns (Test){}; /*right multiline*/ // Right comment
                
                // top comment
                rpc Empty(Test) returns (Test); // Right comment
            }
            
            /*
              Multiline message comment
            */
            // Message comment
            message Test {
                // Field top comment
                string test = 1; // Field right comment
                int64 testInt64 = 2; /* Multiline field right comment */
                /* multiline left comment */ int64 testInt32 = 3;
                
                // oneof comment
                oneof oneof_test{
                    string id = 4[deprecated=true, packed=false];// Field right comment
                    uint32 number = 5;
                }
            }
            """.trimIndent(),
            includeComments = true
        )

        mapOf<Comment, Triple<String, Comment.Placement, Comment.Type>>(
            file.headerComments[0] to Triple(
                "// Protocol Buffers - Google's data interchange format",
                Comment.Placement.TOP,
                Comment.Type.SINGLE_LINE
            ),
            file.headerComments[1] to Triple(
                "// Copyright 2008 Google Inc.  All rights reserved.            ",
                Comment.Placement.TOP,
                Comment.Type.SINGLE_LINE
            ),
            file.extends[0].comments[0] to Triple(
                "// extend comment",
                Comment.Placement.TOP,
                Comment.Type.SINGLE_LINE
            ),
            file.services[0].comments[0] to Triple(
                "/*\n  Multiline comment\n*/",
                Comment.Placement.TOP,
                Comment.Type.MULTILINE
            ),
            file.services[0].comments[1] to Triple(
                "// Service definition",
                Comment.Placement.TOP,
                Comment.Type.SINGLE_LINE
            ),
            file.services[0].comments[2] to Triple(
                "// Offset comment",
                Comment.Placement.TOP,
                Comment.Type.SINGLE_LINE
            ),
            file.services[0].method("Baz").comments[0] to Triple(
                "// Field top comment",
                Comment.Placement.TOP,
                Comment.Type.SINGLE_LINE
            ),
            file.services[0].method("Bar").comments[0] to Triple(
                "/* left comment*/",
                Comment.Placement.BEFORE,
                Comment.Type.MULTILINE
            ),
            file.services[0].method("Bar").comments[1] to Triple(
                "/*right multiline*/",
                Comment.Placement.AFTER,
                Comment.Type.MULTILINE
            ),
            file.services[0].method("Bar").comments[2] to Triple(
                "// Right comment",
                Comment.Placement.AFTER,
                Comment.Type.SINGLE_LINE
            ),

            file.services[0].method("Empty").comments[0] to Triple(
                "// top comment",
                Comment.Placement.TOP,
                Comment.Type.SINGLE_LINE
            ),
            file.services[0].method("Empty").comments[1] to Triple(
                "// Right comment",
                Comment.Placement.AFTER,
                Comment.Type.SINGLE_LINE
            ),

            // todo message comments
        ).forEach { (comment, assertions) ->
            assertEquals(assertions.first, comment.value)
            assertEquals(assertions.second, comment.placement)
            assertEquals(assertions.third, comment.type)
        }
    }
}