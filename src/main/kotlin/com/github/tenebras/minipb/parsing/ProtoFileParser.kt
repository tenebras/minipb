package com.github.tenebras.minipb.parsing

import com.github.tenebras.minipb.TypeResolver
import com.github.tenebras.minipb.model.*
import org.intellij.lang.annotations.Language
import java.io.BufferedReader
import java.io.File
import java.nio.charset.StandardCharsets

class ProtoFileParser(
    private val location: File? = null,
    private val typeResolver: TypeResolver = TypeResolver()
) {
    private var idx = 0

    private var syntax: String? = null
    private var packageName: String? = null
    private val options = mutableMapOf<String, Any>()
    private val services = mutableListOf<Service>()
    private val types = mutableListOf<Type>()
    private val extends = mutableListOf<Extend>()
    private val imports = mutableListOf<Import>()

    companion object {
        fun parseString(@Language("protobuf") content: String, typeResolver: TypeResolver = TypeResolver()): ProtoFile {
            return ProtoFileParser(null, typeResolver).parse(content)
        }

        fun parseFile(file: File, typeResolver: TypeResolver = TypeResolver()): ProtoFile {
            return ProtoFileParser(file, typeResolver).parse(file.readText(StandardCharsets.UTF_8))
        }
    }

    fun parse(content: String): ProtoFile {
        val tokens = tokenizeProto(content)

        while (idx < tokens.size) {
            when (tokens[idx]) {
                "syntax" -> {
                    syntax = tokens[idx + 2]
                    idx += 3
                }
                "package" -> {
                    packageName = tokens[idx + 1]
                    idx += 2
                }
                "option" -> tokens.readOption().let { options[it.first] = it.second }
                "extend" -> extends.add(tokens.readExtend())
                "import" -> imports.add(tokens.readImport())
                "service" -> services.add(tokens.readService())
                "enum" -> tokens.readEnum(packageName).let {
                    types.add(it)
                    typeResolver.add(it)
                }
                "message" -> tokens.readMessage(packageName).let {
                    types.add(it)
                    typeResolver.add(it)
                }
                else -> {
                    throw IllegalStateException("Can't handle token '${tokens[idx]}' in this context. file=${location}, idx=$idx")
                }
            }

            idx++
        }

        return ProtoFile(
            location = location,
            syntax = syntax,
            packageName = packageName,
            options = options,
            types = types.map { typeResolver.find(it.name, it.packageName) },
            services = services.map { s ->
                s.copy(
                    methods = s.methods.map { m ->
                        m.copy(
                            request = typeResolver.find(m.request.name, m.request.packageName),
                            response = typeResolver.find(m.response.name, m.response.packageName)
                        )
                    }
                )
            },
            extends = extends.map {
                it.copy(
                    fields = it.fields.map { field ->
                        field.copy( type = typeResolver.find(field.type.name, field.type.packageName) )
                    }
                )
            },
            imports = imports,
            typeResolver = typeResolver
        )
    }

    private fun List<String>.readField(packageName: String?): Field {
        val isRepeated = this[idx] == "repeated"
        val isOptional = this[idx] == "optional"

        if (isRepeated || isOptional) {
            idx++
        }

        val type = if (this[idx] == "map") {
            MapType(
                TypeReference(this[idx + 2], packageName),
                TypeReference(this[idx + 4], packageName)
            ).also {
                idx += 5
            }
        } else {
            TypeReference(this[idx], packageName)
        }

        val fieldName = this[idx + 1]
        val options = mutableMapOf<String, Any>()
        val number = this[idx + 3].toInt()

        idx += 4

        if (this[idx] == "[") {
            idx++
            while (this[idx] != ";") {
                options[this[idx]] = this[idx + 2]
                idx += 3

                if (this[idx] == "," || this[idx] == "]") {
                    idx++
                }
            }
        }

        return Field(
            name = fieldName,
            type = type,
            number = number,
            isRepeated = isRepeated,
            isOptional = isOptional,
            options = options
        )
    }

    private fun List<String>.readMessage(rootPackageName: String?): MessageType {
        val messageName = this[idx + 1]
        val fields = mutableListOf<Field>()
        val packageName = rootPackageName?.let { "${it}.$messageName" } ?: messageName
        val oneOfs = mutableListOf<OneOf>()
        val options = mutableMapOf<String, Any>()
        val reservedNames = mutableListOf<String>()
        val reservedNumbers = mutableListOf<Int>()
        val reservedRanges = mutableListOf<IntRange>()
        idx += 3

        while (this[idx] != "}") {
            when (this[idx]) {
                "reserved" -> readReserved().let {
                    reservedNames.addAll(it.names)
                    reservedNumbers.addAll(it.numbers)
                    reservedRanges.addAll(it.ranges)
                }
                "repeated", "optional" -> fields.add(readField(packageName))
                "oneof" -> oneOfs.add(readOneOf(packageName))
                "option" -> readOption().let {
                    options[it.first] = it.second
                }
                "message" -> readMessage(packageName).let {
                    types.add(it)
                    typeResolver.add(it)
                }
                "enum" -> readEnum(packageName).let {
                    types.add(it)
                    typeResolver.add(it)
                }
                else -> fields.add(readField(packageName))
            }

            idx++
        }

        // };
        if (getOrNull(idx + 1) == ";") idx++

        return MessageType(
            name = messageName,
            packageName = rootPackageName,
            reserved = Reserved(
                names = reservedNames,
                numbers = reservedNumbers,
                ranges = reservedRanges
            ),
            fields = fields,
            oneOfs = oneOfs,
            options = options
        )
    }

    private fun List<String>.readOneOf(packageName: String?): OneOf {
        val name = this[idx + 1]
        val fields = mutableListOf<Field>()
        val options = mutableMapOf<String, Any>()
        idx += 3

        while (this[idx] != "}") {
            if (this[idx] == "option") {
                readOption().let { options[it.first] = it.second }
            } else {
                fields.add(readField(packageName))
            }

            idx++
        }

        // };
        if (getOrNull(idx + 1) == ";") idx++

        return OneOf(
            name = name,
            fields = fields,
            options = options
        )
    }

    private fun List<String>.readEnum(packageName: String?): EnumType {
        val name = this[idx + 1]
        val options = mutableMapOf<String, Any>()
        val values = mutableListOf<EnumType.Value>()
        val reservedNames = mutableListOf<String>()
        val reservedNumbers = mutableListOf<Int>()
        val reservedRanges = mutableListOf<IntRange>()

        idx += 3

        while (this[idx] != "}") {
            when (this[idx]) {
                "option" -> readOption().let { options[it.first] = it.second }
                "reserved" -> readReserved().let {
                    reservedNames.addAll(it.names)
                    reservedNumbers.addAll(it.numbers)
                    reservedRanges.addAll(it.ranges)
                }
                else -> {
                    val label = this[idx]

                    val number = this[idx + 2].toInt()
                    val fieldOptions = if (this[idx + 3] == "[") {
                        var offset = 3
                        while (this[idx + (++offset)] != "]");

                        subList(idx + 4, idx + offset).joinToString("").split(',').associate { expression ->
                            expression.split('=').let { it.first() to it.last() }
                        }.also {
                            idx += offset + 1
                        }
                    } else {
                        idx += 3
                        emptyMap()
                    }

                    values.add(
                        EnumType.Value(
                            label = label,
                            number = number,
                            options = fieldOptions
                        )
                    )
                }
            }

            if (this[idx] == ";") idx++
        }

        // };
        if (getOrNull(idx + 1) == ";") idx++

        return EnumType(
            name = name,
            packageName = packageName,
            values = values,
            options = options,
            reserved = Reserved(reservedNames, reservedNumbers, reservedRanges)
        )
    }

    private fun List<String>.readOption(): Pair<String, Any> {
        return if (this[idx + 1] == "(") {
            if (this[idx + 4] == "=") {
                (subList(idx + 1, idx + 4).joinToString("") to this[idx + 5]).also {
                    idx += 6
                }
            } else {
                (subList(idx + 1, idx + 5).joinToString("") to this[idx + 6]).also {
                    idx += 7
                }
            }
        } else {
            (this[idx + 1] to this[idx + 3]).also {
                idx += 4
            }
        }
    }

    private fun List<String>.readReserved(): Reserved {
        val names = mutableListOf<String>()
        val ranges = mutableListOf<IntRange>()
        val numbers = mutableListOf<Int>()

        idx++
        while (this[idx] != ";") {
            if (this[idx + 1] == "," || this[idx + 1] == ";") {
                if (this[idx][0] == '"') {
                    names.add(this[idx].trim('"'))
                } else {
                    numbers.add(this[idx].toInt())
                }
            } else if (this[idx + 1] == "to") {
                val end = if (this[idx + 2] == "max") 536_870_911 else this[idx + 2].toInt()

                ranges.add(this[idx].toInt()..end)

                idx += 2
            }

            idx++

            if (this[idx] == ",") {
                idx++
            }
        }

        return Reserved(names, numbers, ranges)
    }

    private fun List<String>.readService(): Service {
        val serviceName = this[idx + 1]
        val methods = mutableListOf<Method>()

        idx += 3

        var name: String
        var request: Type?
        var response: Type?
        var isRequestStreamed: Boolean
        var isResponseStreamed: Boolean

        while (this[idx] != "}") {
            name = this[++idx]

            if (this[idx + 2] == "stream") {
                isRequestStreamed = true
                request = TypeReference(this[idx + 3], packageName)
                idx += 7
            } else {
                isRequestStreamed = false
                request = TypeReference(this[idx + 2], packageName)
                idx += 6
            }

            if (this[idx] == "stream") {
                isResponseStreamed = true
                response = TypeReference(this[idx + 1], packageName)
                idx++
            } else {
                isResponseStreamed = false
                response = TypeReference(this[idx], packageName)
            }

            val methodOptions = mutableMapOf<String, Any>()

            // rpc Method(Request) returns (Response);
            // rpc Method(Request) returns (Response) {};
            // rpc Method(Request) returns (Response) {option test = 1;}
            // rpc Method(Request) returns (Response) {}

            when {
                this[idx + 2] == ";" -> idx += 3
                this[idx + 2] == "{" && this[idx + 4] == ";" -> idx += 5
                this[idx + 2] == "{" && this[idx + 3] == "option" -> {
                    idx += 3

                    while (this[idx] != "}") {
                        readOption().let {
                            methodOptions[it.first] = it.second
                        }

                        idx++
                    }

                    if (this[idx +1] == ";") idx += 2 else idx++
                }
                else -> idx += 4
            }

            methods.add(
                Method(
                    name = name,
                    request = request,
                    response = response,
                    isRequestStreamed = isRequestStreamed,
                    isResponseStreamed = isResponseStreamed,
                    options = methodOptions
                )
            )
        }

        return Service(serviceName, methods, packageName)
    }

    private fun List<String>.readImport(): Import {
        val importType = when (this[idx + 1]) {
            "public" -> Import.ImportType.PUBLIC
            "weak" -> Import.ImportType.WEAK
            else -> Import.ImportType.DEFAULT
        }

        if (importType != Import.ImportType.DEFAULT) {
            idx++
        }

        val path = this[idx + 1].trim('"')

        idx += 2

        val file = File(location?.parent?.let { "$it/$path" } ?: "./$path")
        val parsedFile = when {
            file.exists() -> parseFile(File(path), typeResolver)
            path.startsWith("google/protobuf/") -> {
                val content = ProtoFileParser::class.java.classLoader.getResourceAsStream(path)
                    ?.bufferedReader()
                    ?.use(BufferedReader::readText) ?: error("Imported file [$path] not found")

                parseString(content, typeResolver)
            }
            else -> throw IllegalArgumentException("Imported file [$path] not found")
        }

        return Import(
            type = importType,
            path = path,
            file = parsedFile
        )
    }

    private fun List<String>.readExtend(): Extend {
        val type = this[idx + 1]
        val extendFields = mutableListOf<Field>()
        idx += 3

        while (this[idx] != "}") {
            extendFields.add(this.readField(packageName))
            idx++
        }

        return Extend(
            typeName = type,
            fields = extendFields
        )
    }
}
