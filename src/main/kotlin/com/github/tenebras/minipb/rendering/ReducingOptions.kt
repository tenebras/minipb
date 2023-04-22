package com.github.tenebras.minipb.rendering

class ReducingOptions (
    val includeMethods: List<MethodReference> = emptyList(),
    val excludedMethods: List<MethodReference> = emptyList(),
    val includeServices: List<String> = emptyList(),
    val excludeServices: List<String> = emptyList()
) {
    fun isServiceAllowed(serviceName: String): Boolean {
        val isExcluded = excludeServices.isNotEmpty() && serviceName in excludeServices
        val isIncluded = includeServices.isEmpty() || serviceName in includeServices

        return !isExcluded && isIncluded
    }

    fun isMethodAllowed(serviceName: String, methodName: String): Boolean {
        if (!isServiceAllowed(serviceName)) {
            return false
        }

        val isExcluded = excludedMethods.isNotEmpty() && excludedMethods.any {
            it.methodName == methodName && (it.serviceName == serviceName || it.serviceName == null)
        }
        val isIncluded = includeMethods.isEmpty() || (includeMethods.any {
            it.methodName == methodName && (it.serviceName == serviceName || it.serviceName == null)
        })

        return !isExcluded && isIncluded
    }
}