package com.github.tenebras.minipb.model

import com.github.tenebras.minipb.TypeResolver
import java.io.File


data class ProtoFile(
    val location: File?,
    var syntax: String?,
    var packageName: String?,
    val options: Map<String, Any>,
    val services: List<Service>,
    val types: List<Type>,
    val extends: List<Extend>,
    val imports: List<Import>,
    val typeResolver: TypeResolver
) {
    inline fun <reified T> type(name: String, packageName: String? = null): T {
        return types.first { it.name == name && it is T && it.packageName == packageName } as T
    }

    inline fun <reified T> hasType(name: String, packageName: String?): Boolean {
        return types.any { it.name == name && it is T && it.packageName == packageName }
    }

    inline fun <reified T> hasType(fullName: String): Boolean {
        return types.any { it.fullName() == fullName && it is T }
    }

    fun service(name: String): Service = services.first { it.name == name }
    fun hasService(name: String): Boolean = services.any { it.name == name }

    fun allServices(): List<Service> = services + imports.map { it.file.allServices() }.flatten()
}