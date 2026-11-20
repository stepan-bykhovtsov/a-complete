package dev.bykhovtsov.acomplete.mention

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.Computable
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import dev.bykhovtsov.acomplete.util.ACompleteBundle
import java.util.Base64

object FileMentionResolver {

    // Matches @file:"path with spaces" or @file:path/to/file.ext
    private val FILE_MENTION_REGEX = Regex("""@file:(?:"([^"]+)"|([^\s\r\n]+))""")

    // Secondary match for @path/to/file.ext (must contain a dot or slash to avoid false positives)
    private val AT_MENTION_REGEX = Regex("""@(?:"([^"]+)"|([^\s\r\n]+))""")

    val TRAILING_PUNCTUATION = charArrayOf(
        ',', '.', ';', ':', '!', '?', ')', ']', '}', '>', '\'', '"'
    )

    val IMAGE_EXTENSIONS = mapOf(
        "png" to "image/png",
        "jpg" to "image/jpeg",
        "jpeg" to "image/jpeg",
        "gif" to "image/gif",
        "webp" to "image/webp",
        "svg" to "image/svg+xml",
        "bmp" to "image/bmp",
        "ico" to "image/x-icon",
        "tiff" to "image/tiff",
        "tif" to "image/tiff"
    )

    fun extractMentionPaths(text: String): Map<String, String> {
        if (text.isBlank()) return emptyMap()
        val mentionedPaths = LinkedHashMap<String, String>()

        for (match in FILE_MENTION_REGEX.findAll(text)) {
            val isQuoted = match.groupValues[1].isNotEmpty()
            if (isQuoted) {
                val path = match.groupValues[1]
                mentionedPaths[match.value] = path
            } else {
                val unquoted = match.groupValues[2]
                val trimmedPath = unquoted.trimEnd(*TRAILING_PUNCTUATION)
                if (trimmedPath.isNotEmpty()) {
                    val rawMention = "@file:$trimmedPath"
                    mentionedPaths[rawMention] = trimmedPath
                }
            }
        }

        return mentionedPaths
    }

    fun determineFileType(extension: String, isBinary: Boolean = false): MentionedFileType {
        val ext = extension.lowercase().removePrefix(".")
        return when {
            ext == "pdf" -> MentionedFileType.PDF
            ext in IMAGE_EXTENSIONS -> MentionedFileType.IMAGE
            !isBinary -> MentionedFileType.TEXT
            else -> MentionedFileType.UNSUPPORTED
        }
    }

    fun resolveMentions(text: String, project: Project?): List<MentionedFile> {
        if (project == null || text.isBlank()) return emptyList()
        val projectDir = project.guessProjectDir() ?: return emptyList()

        val mentionedPaths = LinkedHashMap<String, String>() // rawMention -> extractedPath
        mentionedPaths.putAll(extractMentionPaths(text))

        // 2. Secondary check for direct @path if it points to an existing file
        for (match in AT_MENTION_REGEX.findAll(text)) {
            val raw = match.value
            if (raw.startsWith("@file:")) continue
            val isQuoted = match.groupValues[1].isNotEmpty()
            val path = if (isQuoted) {
                match.groupValues[1]
            } else {
                match.groupValues[2].trimEnd(*TRAILING_PUNCTUATION)
            }
            if (path.isNotEmpty() && (path.contains('.') || path.contains('/'))) {
                val normalized = path.replace('\\', '/').trimStart('/')
                val vf = projectDir.findFileByRelativePath(normalized) ?: findFileInRoots(project, normalized)
                if (vf != null && !vf.isDirectory) {
                    mentionedPaths.putIfAbsent("@$path", path)
                }
            }
        }

        val results = mutableListOf<MentionedFile>()
        for ((raw, rawPath) in mentionedPaths) {
            val normalized = rawPath.replace('\\', '/').trimStart('/')
            var virtualFile = projectDir.findFileByRelativePath(normalized)
                ?: findFileInRoots(project, normalized)

            if (virtualFile == null || virtualFile.isDirectory || !virtualFile.isValid) {
                val trimmed = normalized.trimEnd(*TRAILING_PUNCTUATION)
                if (trimmed.isNotEmpty() && trimmed != normalized) {
                    virtualFile = projectDir.findFileByRelativePath(trimmed) ?: findFileInRoots(project, trimmed)
                }
            }

            if (virtualFile == null || virtualFile.isDirectory || !virtualFile.isValid) {
                if (raw.startsWith("@file:")) {
                    throw IllegalArgumentException(ACompleteBundle.message("action.error.file.not.found", rawPath))
                }
                continue
            }

            val relativePath = VfsUtilCore.getRelativePath(virtualFile, projectDir) ?: virtualFile.name
            val normalizedRelativePath = relativePath.replace('\\', '/')
            val ext = virtualFile.extension?.lowercase().orEmpty()

            when {
                ext == "pdf" -> {
                    val bytes = runReadActionSafely { virtualFile.contentsToByteArray() }
                    val base64 = Base64.getEncoder().encodeToString(bytes)
                    results.add(
                        MentionedFile(
                            rawMention = raw,
                            relativePath = normalizedRelativePath,
                            virtualFile = virtualFile,
                            type = MentionedFileType.PDF,
                            mimeType = "application/pdf",
                            base64DataUrl = "data:application/pdf;base64,$base64"
                        )
                    )
                }
                ext in IMAGE_EXTENSIONS -> {
                    val mimeType = IMAGE_EXTENSIONS[ext] ?: "image/png"
                    val bytes = runReadActionSafely { virtualFile.contentsToByteArray() }
                    val base64 = Base64.getEncoder().encodeToString(bytes)
                    results.add(
                        MentionedFile(
                            rawMention = raw,
                            relativePath = normalizedRelativePath,
                            virtualFile = virtualFile,
                            type = MentionedFileType.IMAGE,
                            mimeType = mimeType,
                            base64DataUrl = "data:$mimeType;base64,$base64"
                        )
                    )
                }
                else -> {
                    if (virtualFile.fileType.isBinary) {
                        throw IllegalArgumentException(ACompleteBundle.message("action.error.file.unsupported", virtualFile.name))
                    }
                    val textContent = loadTextContent(virtualFile)
                    results.add(
                        MentionedFile(
                            rawMention = raw,
                            relativePath = normalizedRelativePath,
                            virtualFile = virtualFile,
                            type = MentionedFileType.TEXT,
                            mimeType = "text/plain",
                            textContent = textContent
                        )
                    )
                }
            }
        }

        return results
    }

    private fun findFileInRoots(project: Project, path: String): VirtualFile? {
        val roots = ProjectRootManager.getInstance(project).contentRoots
        for (root in roots) {
            val file = root.findFileByRelativePath(path)
            if (file != null && !file.isDirectory) return file
        }
        return null
    }

    private fun loadTextContent(file: VirtualFile): String {
        return runReadActionSafely {
            val doc = FileDocumentManager.getInstance().getDocument(file)
            doc?.text ?: String(file.contentsToByteArray(), Charsets.UTF_8)
        }
    }

    fun <T> runReadActionSafely(block: () -> T): T {
        val app = ApplicationManager.getApplication()
        return if (app == null || app.isReadAccessAllowed) {
            block()
        } else {
            app.runReadAction(Computable { block() })
        }
    }
}
