package com.github.tenebras.minipb

import com.github.tenebras.minipb.model.ProtoFile
import com.github.tenebras.minipb.model.Service
import com.github.tenebras.minipb.rendering.DependencyTree
import com.github.tenebras.minipb.rendering.ReducingOptions

class FileReducer(private val reducingOptions: ReducingOptions) {
    fun reduce(file: ProtoFile): Pair<ProtoFile, DependencyTree> {
        val allServices = if (reducingOptions.ignoreServices) {
            emptyList()
        } else {
            file.allServices().mapNotNull { s ->
                if (reducingOptions.isServiceAllowed(s.name))
                    s.copy(
                        methods = s.methods.filter { m -> reducingOptions.isMethodAllowed(s.name, m.name) }
                    )
                else null
            }.filter { it.methods.isNotEmpty() }
        }

        val serviceTypes = allServices
            .map { s -> s.methods.map { m -> listOf(m.request, m.response) }.flatten() }
            .flatten()
            .distinct()

        val forcedTypes = reducingOptions.forcedTypeNames.map {
            file.typeResolver.findOrNull(it) ?: throw IllegalArgumentException("Can't resolve forced type $it")
        }

        val dependencyTree = DependencyTree.of(file.typeResolver, (serviceTypes + forcedTypes).distinct())

        return reduce(file, dependencyTree, allServices) to dependencyTree
    }

    private fun reduce(file: ProtoFile, dependencyTree: DependencyTree, services: List<Service>): ProtoFile {
        return file.copy(
            imports = file.imports.map { i -> i.copy(file = reduce(i.file, dependencyTree, services)) },
            services = services.filter { s ->
                file.services.any { s.name == it.name && s.packageName == it.packageName }
            },
            types = file.types.filter { t -> dependencyTree.hasType(t.fullName()) }
        )
    }
}