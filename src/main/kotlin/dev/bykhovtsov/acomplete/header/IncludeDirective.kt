package dev.bykhovtsov.acomplete.header

data class IncludeDirective(
    val path: String,
    val type: IncludeType
)
