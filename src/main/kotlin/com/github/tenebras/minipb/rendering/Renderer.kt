package com.github.tenebras.minipb.rendering

import com.github.tenebras.minipb.model.*

class Renderer {
    fun render(file: ProtoFile, dependencyTree: DependencyTree): String {
        val builder = StringBuilder()

        file.syntax?.let { builder.append("syntax = $it;\n") }
        file.packageName?.let { builder.append("\npackage $it;\n") }

        if (file.imports.isNotEmpty()) {
            builder.append("\n")
        }

        file.imports.forEach { i ->
            if (i.type != Import.ImportType.DEFAULT) {
                builder.append("import ${i.type.name.lowercase()} \"${i.path}\";\n")
            } else {
                builder.append("import \"${i.path}\";\n")
            }
        }

        if (file.options.isNotEmpty()) {
            builder.append("\n")
        }

        file.options.forEach { (name, value) -> builder.append("option $name = $value;\n") }

        file.extends.forEach {
            val renderedFields = it.fields.joinToString("\n") { f -> "    " + f.render(null, file.packageName) }

            builder.append("\nextend ${it.typeName} {\n${renderedFields}\n}\n")
        }

        file.services.forEach { s ->
            val renderedMethods = "\n" + s.methods.joinToString("\n") { m ->
                val filePackageName = file.packageName?.let { "$it." } ?: ""
                val requestTypeName = m.request.fullName().replace(filePackageName, "")
                val responseTypeName = m.response.fullName().replace(filePackageName, "")

                if (m.options.isEmpty()) {
                    "    rpc ${m.name} (${requestTypeName}) returns (${responseTypeName});"
                } else {
                    "    rpc ${m.name} (${requestTypeName}) returns (${responseTypeName}) {${m.options.renderOptions("        ")}    }"
                }
            }

            builder.append("\nservice ${s.name} {${renderedMethods}\n}\n")
        }

        dependencyTree.nodes
            .filter { file.hasType<Type>(it.typeName) }
            .forEach { n ->
                builder.append(n.render(filePackageName = file.packageName) + "\n")
            }

        return builder.toString()
    }

    private fun Node.render(indent: String = "", filePackageName: String? = null): String {
        return when (type) {
            is MessageType -> {

                val renderedFields = if(renderType == RenderType.REQUIRED)
                    type.fields.render("$indent    ", type, filePackageName)
                else ""

                val renderedOneOf = if(renderType == RenderType.REQUIRED && type.oneOfs.isNotEmpty()) {
                    type.oneOfs.joinToString("\n") {
                        val renderedOptions = type.options.renderOptions("$indent        ")
                        val fields = it.fields.render("$indent        ", type, filePackageName)

                        "\n\n${indent}    oneof ${it.name}{${renderedOptions}${fields}${indent}    }"
                    } + "\n"
                } else ""

                val nestedTypes = if (nested.isNotEmpty()) {
                    "\n" + nested.joinToString("\n") { it.render("$indent    ") }
                } else ""

                val renderedOptions = type.options.renderOptions("$indent    ")

                "\n${indent}message ${type.name} {$renderedOptions${renderedFields}${renderedOneOf}${nestedTypes}\n$indent}"
            }
            is EnumType -> {
                val renderedOptions = type.options.renderOptions("$indent    ")

                val renderedValues = if (type.values.isNotEmpty()) {
                    "\n$indent    " + type.values.joinToString("\n$indent    ") {
                        val valueOptions = it.options
                            .map { (name, value) -> "$name = $value" }
                            .joinToString(", ")
                            .let { ro -> if (ro.isEmpty()) "" else "[$ro]" }

                        "${it.label} = ${it.number}$valueOptions;"
                    }
                } else ""

                "\n${indent}enum ${type.name} {${renderedOptions}${renderedValues}\n$indent}"
            }
            else -> throw IllegalStateException("Type ${type?.fullName()} should not be rendered")
        }
    }

    private fun Field.render(context: Type?, filePackageName: String? = null): String {
        val prefix = when {
            isOptional -> "optional "
            isRepeated -> "repeated "
            else -> ""
        }

        val renderedOptions = if (options.isNotEmpty()) {
            " [${options.map { (name, value) -> "$name = $value" }.joinToString(", ")}]"
        } else ""

        val typeName = when {
            type is BuiltInType || context == null -> type.fullName()
            // One level nested or same level
            context.fullName() == type.packageName || context.packageName == type.packageName -> type.name
            // branch from parent or nested multiple levels
            context.packageName != null && type.packageName?.startsWith(context.packageName!!) == true ->
                if (type.packageName?.startsWith(context.fullName()) == true) {
                    type.fullName().replace("${context.fullName()}.", "")
                } else {
                    type.fullName().replace("${context.packageName!!}.", "")
                }
            // package name from root
            else -> filePackageName?.let { type.fullName().replace("$it.", "") } ?: type.fullName()
        }

        return "$prefix${typeName} $name = $number$renderedOptions;"
    }

    private fun Map<String, Any>.renderOptions(indent: String): String
        = if (isNotEmpty()) {
            "\n" + this.map { "${indent}option ${it.key} = ${it.value};" }.joinToString("\n") + "\n"
        } else ""

    private fun List<Field>.render(indent: String, type: Type?, filePackageName: String?): String {
        return if (isNotEmpty()) {
            "\n" + joinToString("\n") { indent + it.render(type, filePackageName) }
        } else ""
    }
}
