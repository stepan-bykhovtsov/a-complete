package dev.bykhovtsov.acomplete.header

data class HeaderFileContext(
    val includePath: String,
    val resolvedPath: String,
    val content: String,
    val includeType: IncludeType
)
