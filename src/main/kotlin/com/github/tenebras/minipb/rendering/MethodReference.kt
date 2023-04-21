package com.github.tenebras.minipb.rendering

class MethodReference(
    val methodName: String,
    val serviceName: String? = null
) {
    companion object {
        fun of(serviceAndMethod: String): MethodReference {

            if (serviceAndMethod.contains('.')) {
                return serviceAndMethod.split('.').let {
                    MethodReference(it.last(), it.first())
                }
            }

            return MethodReference(serviceAndMethod)
        }
    }
}