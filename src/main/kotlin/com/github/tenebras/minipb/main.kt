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
    val method by parser.option(ArgType.String, shortName = "m", description = "Service method. May be method name or Service.Method").multiple()
    val excludeMethod by parser.option(ArgType.String, shortName = "em", description = "Service method to exclude. May be method name or Service.Method").multiple()
    val service by parser.option(ArgType.String, shortName = "s", description = "Service to include").multiple()
    val excludeService by parser.option(ArgType.String, shortName = "es", description = "Service to exclude").multiple()
    val clearOutput by parser.option(ArgType.Boolean, shortName = "co", description = "Clear output folder").default(false)

    parser.parse(args)

    val (reducedProtoFile, dependencyTree) = FileReducer(
        ReducingOptions(
            includeMethods = method.map { MethodReference.of(it) },
            excludedMethods = excludeMethod.map { MethodReference.of(it) },
            includeServices = service,
            excludeServices = excludeService
        )
    ).reduce(ProtoFileParser.parseFile(File(input.resolveHomeDir())))

    FileWriter(File(outputFolder.resolveHomeDir()).canonicalFile, clearOutput).write(reducedProtoFile, dependencyTree)
}

fun String.resolveHomeDir(): String {
    return if (this[0] == '~') {
        replaceFirst("~", System.getProperty("user.home"))
    } else this
}
