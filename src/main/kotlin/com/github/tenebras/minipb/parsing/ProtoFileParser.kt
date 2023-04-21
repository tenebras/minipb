package com.github.tenebras.minipb.parsing

import com.github.tenebras.minipb.TypeResolver
import com.github.tenebras.minipb.model.*
import org.intellij.lang.annotations.Language
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

                "extend" -> {
                    val type = tokens[idx + 1]
                    val extendFields = mutableListOf<Field>()
                    idx += 3

                    while (tokens[idx] != "}") {
                        extendFields.add(tokens.readField(packageName))
                        idx++
                    }

                    extends.add(
                        Extend(
                            typeName = type,
                            fields = extendFields
                        )
                    )
                }

                "import" -> {
                    val importType = when (tokens[idx + 1]) {
                        "public" -> Import.ImportType.PUBLIC
                        "weak" -> Import.ImportType.WEAK
                        else -> Import.ImportType.DEFAULT
                    }

                    if (importType != Import.ImportType.DEFAULT) {
                        idx++
                    }

                    val path = tokens[idx + 1].trim('"')

                    imports.add(
                        Import(
                            type = importType,
                            path = path,
                            file = parseFile(
                                File(location?.parent?.let { "$it/$path" } ?: "./$path"),
                                typeResolver
                            )
                        )
                    )

                    idx += 2
                }

                "service" -> {
                    val serviceName = tokens[idx + 1]
                    val methods = mutableListOf<Method>()

                    idx += 3

                    var name: String
                    var request: Type?
                    var response: Type?
                    var isRequestStreamed: Boolean
                    var isResponseStreamed: Boolean

                    while (tokens[idx] != "}") {
                        name = tokens[++idx]

                        if (tokens[idx + 2] == "stream") {
                            isRequestStreamed = true
                            request = TypeReference(tokens[idx + 3], packageName)
                            idx += 7
                        } else {
                            isRequestStreamed = false
                            request = TypeReference(tokens[idx + 2], packageName)
                            idx += 6
                        }

                        if (tokens[idx] == "stream") {
                            isResponseStreamed = true
                            response = TypeReference(tokens[idx + 1], packageName)
                            idx++
                        } else {
                            isResponseStreamed = false
                            response = TypeReference(tokens[idx], packageName)
                        }

                        methods.add(
                            Method(
                                name = name,
                                request = request,
                                response = response,
                                isRequestStreamed = isRequestStreamed,
                                isResponseStreamed = isResponseStreamed
                            )
                        )

                        // rpc Method(Request) returns (Response);
                        // rpc Method(Request) returns (Response) {};
                        // rpc Method(Request) returns (Response) {}

                        idx += when {
                            tokens[idx + 2] == ";" -> 3
                            tokens[idx + 2] == "{" && tokens[idx + 4] == ";" -> 5
                            else -> 4
                        }
                    }

                    services.add(
                        Service(serviceName, methods, packageName)
                    )
                }

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
        val reserved = Reserved()
        val fields = mutableListOf<Field>()
        val packageName = rootPackageName?.let { "${it}.$messageName" } ?: messageName
        val oneOfs = mutableListOf<OneOf>()
        val options = mutableMapOf<String, Any>()
        idx += 3

        while (this[idx] != "}") {
            when (this[idx]) {
                "reserved" -> readReserved().let { reserved.add(it) }

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

        return MessageType(
            name = messageName,
            packageName = rootPackageName,
            reserved = reserved,
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

        return OneOf(
            name = name,
            fields = fields,
            options = options
        )
    }

    private fun List<String>.readEnum(packageName: String?): EnumType {
        val name = this[idx + 1]
        val options = mutableMapOf<String, Any>()
        val values = mutableListOf<EnumType.EnumValue>()
        val reserved = Reserved()

        idx += 3

        while (this[idx] != "}") {
            when (this[idx]) {
                "option" -> readOption().let { options[it.first] = it.second }
                "reserved" -> readReserved().let { reserved.add(it) }
                else -> {
                    val label = this[idx]

                    if (this[idx + 2] == ";") {
                        println()
                    }

                    val number = this[idx + 2].toInt()
                    val fieldOptions = if (this[idx + 3] == "[") {
                        var offset = 3
                        while (this[idx + (++offset)] != "]");

                        subList(idx + 4, idx + offset).joinToString("").split(',').map { expression ->
                            expression.split('=').let { it.first() to it.last() }
                        }.toMap().also {
                            idx += offset + 1
                        }
                    } else {
                        idx += 3
                        emptyMap()
                    }

                    values.add(
                        EnumType.EnumValue(
                            label = label,
                            number = number,
                            options = fieldOptions
                        )
                    )
                }
            }

            if (this[idx] == ";") {
                idx++
            }
        }

        return EnumType(
            name = name,
            packageName = packageName,
            values = values,
            options = options,
            reserved = reserved
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
        val reserved = Reserved()

        idx++
        while (this[idx] != ";") {
            if (this[idx + 1] == "," || this[idx + 1] == ";") {
                if (this[idx][0] == '"') {
                    reserved.add(this[idx].trim('"'))
                } else {
                    reserved.add(this[idx].toInt())
                }
            } else if (this[idx + 1] == "to") {
                val end = if (this[idx + 2] == "max") 536_870_911 else this[idx + 2].toInt()

                reserved.add(this[idx].toInt()..end)

                idx += 2
            }

            idx++

            if (this[idx] == ",") {
                idx++
            }
        }

        return reserved
    }
}