package com.portalhost.native

expect class NativeFilePicker {
    suspend fun pickFile(config: PickConfig): Result<List<Uri>>
    suspend fun pickDirectory(config: PickConfig): Result<Uri?>
    suspend fun saveFile(config: SaveConfig): Result<Uri?>
}

data class PickConfig(
    val title: String = "Select File",
    val filters: List<FileFilter> = emptyList(),
    val multiSelect: Boolean = false,
    val startDir: String? = null
)

data class SaveConfig(
    val title: String = "Save File",
    val filters: List<FileFilter> = emptyList(),
    val defaultName: String? = null,
    val startDir: String? = null
)

data class FileFilter(
    val name: String,
    val extensions: List<String>
)

data class Uri(val path: String) {
    override fun toString(): String = path
}