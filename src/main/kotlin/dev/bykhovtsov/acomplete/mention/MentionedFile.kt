package dev.bykhovtsov.acomplete.mention

import com.intellij.openapi.vfs.VirtualFile

data class MentionedFile(
    val rawMention: String,
    val relativePath: String,
    val virtualFile: VirtualFile? = null,
    val type: MentionedFileType,
    val mimeType: String,
    val textContent: String? = null,
    val base64DataUrl: String? = null
)
