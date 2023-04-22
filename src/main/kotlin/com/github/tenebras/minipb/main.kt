package com.github.tenebras.minipb

import com.github.tenebras.minipb.parsing.ProtoFileParser
import com.github.tenebras.minipb.rendering.MethodReference
import com.github.tenebras.minipb.rendering.ReducingOptions
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.multiple
import java.io.File

fun main(args: Array<String>) {
    val parser = ArgParser("minipb")
    val input by parser.argument(ArgType.String, description = "Input file")
    val outputFolder by parser.argument(ArgType.String, description = "Output folder")
    val methods by parser.option(
        ArgType.String,
        fullName = "method",
        shortName = "m",
        description = "Service method. May be method name or Service.Method"
    ).multiple()

    val excludedMethods by parser.option(
        ArgType.String,
        fullName = "exclude-method",
        shortName = "em",
        description = "Service method to exclude. May be method name or Service.Method"
    ).multiple()

    val includeServices by parser.option(
        ArgType.String,
        fullName = "service",
        shortName = "s",
        description = "Service to include"
    ).multiple()

    val excludedServices by parser.option(
        ArgType.String,
        fullName = "exclude-service",
        shortName = "es",
        description = "Service to exclude"
    ).multiple()

    val isClearOutput by parser.option(
        ArgType.Boolean,
        fullName = "clear-output",
        shortName = "co",
        description = "Clear output folder"
    ).default(false)

    val forcedTypeNames by parser.option(
        ArgType.String,
        fullName = "type",
        shortName = "t",
        description = "Type name to force include. Should be FQN including package name"
    ).multiple()

    val ignoreServices by parser.option(
        ArgType.Boolean,
        fullName = "ignore-services",
        shortName = "is",
        description = "Ignore all services would be included only forced types"
    ).default(false)

    parser.parse(args)

    val (reducedProtoFile, dependencyTree) = FileReducer(
        ReducingOptions(
            includeMethods = methods.map { MethodReference.of(it) },
            excludedMethods = excludedMethods.map { MethodReference.of(it) },
            includeServices = includeServices,
            excludeServices = excludedServices,
            forcedTypeNames = forcedTypeNames,
            ignoreServices = ignoreServices
        )
    ).reduce(ProtoFileParser.parseFile(File(input.resolveHomeDir())))

    FileWriter(File(outputFolder.resolveHomeDir()).canonicalFile, isClearOutput).write(reducedProtoFile, dependencyTree)
}

fun String.resolveHomeDir(): String {
    return if (this[0] == '~') {
        replaceFirst("~", System.getProperty("user.home"))
    } else this
}
