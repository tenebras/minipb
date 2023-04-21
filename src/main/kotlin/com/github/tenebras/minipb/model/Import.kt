package com.github.tenebras.minipb.model

data class Import(
    val type: ImportType,
    val path: String,
    val file: ProtoFile
) {
    enum class ImportType {
        DEFAULT,
        PUBLIC,
        WEAK
    }
}