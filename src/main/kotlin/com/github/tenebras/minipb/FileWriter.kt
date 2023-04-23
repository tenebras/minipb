package com.github.tenebras.minipb

import com.github.tenebras.minipb.model.ProtoFile
import com.github.tenebras.minipb.rendering.DependencyTree
import com.github.tenebras.minipb.rendering.Renderer
import java.io.File
import java.nio.file.Files

class FileWriter(
    private val outputFolder: File,
    private val clearOutput: Boolean,
    private val renderer: Renderer = Renderer()
) {
    private var isOutputCleared = false

    fun write(protoFile: ProtoFile, dependencyTree: DependencyTree) {
        if (clearOutput && !isOutputCleared) {
            clearOutputFolder()
        }

        if (!outputFolder.exists()) {
            Files.createDirectories(outputFolder.toPath())
        }

        (protoFile.importedFiles() + protoFile).forEach {
            File(outputFolder, it.location!!.name).writeText(renderer.render(it, dependencyTree))
        }
    }

    private fun ProtoFile.importedFiles(): List<ProtoFile> {
        val imported = mutableListOf<ProtoFile>()

        imports.map {
            imported.add(it.file)
            imported.addAll(it.file.importedFiles())
        }

        return imported
    }

    private fun clearOutputFolder() {
        if (!outputFolder.exists()) {
            return
        }

        outputFolder.walkBottomUp().forEach {
            if (it.isFile || (it.isDirectory && it != outputFolder)) {
                it.delete()
            }
        }
    }
}