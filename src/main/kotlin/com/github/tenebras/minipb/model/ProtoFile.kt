package com.github.tenebras.minipb.model

import com.github.tenebras.minipb.TypeResolver
import java.io.File


data class ProtoFile(
    val typeResolver: TypeResolver,
    val location: File? = null,
    var syntax: String? = null,
    var packageName: String? = null,
    val options: Map<String, Any> = emptyMap(),
    val services: List<Service> = emptyList(),
    val types: List<Type> = emptyList(),
    val extends: List<Extend> = emptyList(),
    val imports: List<Import> = emptyList()
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