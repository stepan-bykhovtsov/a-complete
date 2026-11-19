package dev.bykhovtsov.acomplete.header

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.Computable
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import dev.bykhovtsov.acomplete.settings.ACompleteSettings

object CHeaderResolver {

    val C_EXTENSIONS = setOf("c", "h")
    val CPP_EXTENSIONS = setOf("cpp", "hpp", "cc", "hh", "cxx", "hxx", "c++", "h++", "tpp", "inl", "ipp", "ixx", "cu", "cuh")
    val C_CPP_EXTENSIONS = C_EXTENSIONS + CPP_EXTENSIONS

    private val QUOTE_REGEX = Regex("""^#[ \t]*include[ \t]*"([^"]+)"""")
    private val ANGLE_REGEX = Regex("""^#[ \t]*include[ \t]*<([^>]+)>""")

    fun isCSourceOrHeader(fileExtension: String): Boolean {
        val ext = fileExtension.lowercase().removePrefix(".")
        return ext in C_CPP_EXTENSIONS
    }

    fun extractDirectives(text: String): List<IncludeDirective> {
        val directives = mutableListOf<IncludeDirective>()
        var inBlockComment = false

        for (rawLine in text.lines()) {
            var trimmed = rawLine.trim()

            if (inBlockComment) {
                val endCommentIdx = trimmed.indexOf("*/")
                if (endCommentIdx != -1) {
                    inBlockComment = false
                    trimmed = trimmed.substring(endCommentIdx + 2).trim()
                } else {
                    continue
                }
            }

            if (trimmed.startsWith("//")) continue

            while (trimmed.contains("/*")) {
                val startIdx = trimmed.indexOf("/*")
                val endIdx = trimmed.indexOf("*/", startIdx + 2)
                if (endIdx != -1) {
                    trimmed = (trimmed.substring(0, startIdx) + " " + trimmed.substring(endIdx + 2)).trim()
                } else {
                    inBlockComment = true
                    trimmed = trimmed.substring(0, startIdx).trim()
                    break
                }
            }

            if (trimmed.isEmpty()) continue

            val quoteMatch = QUOTE_REGEX.find(trimmed)
            if (quoteMatch != null) {
                val path = quoteMatch.groupValues[1].trim()
                if (path.isNotEmpty()) {
                    directives.add(IncludeDirective(path, IncludeType.QUOTE))
                }
                continue
            }

            val angleMatch = ANGLE_REGEX.find(trimmed)
            if (angleMatch != null) {
                val path = angleMatch.groupValues[1].trim()
                if (path.isNotEmpty()) {
                    directives.add(IncludeDirective(path, IncludeType.ANGLE))
                }
                continue
            }
        }

        return directives
    }

    fun getIncludeRoots(project: Project): List<VirtualFile> {
        val roots = LinkedHashSet<VirtualFile>()
        val projectRootManager = ProjectRootManager.getInstance(project)

        for (root in projectRootManager.contentRoots) {
            if (root.isValid && root.isDirectory) roots.add(root)
        }
        for (root in projectRootManager.contentSourceRoots) {
            if (root.isValid && root.isDirectory) roots.add(root)
        }

        project.guessProjectDir()?.let {
            if (it.isValid && it.isDirectory) roots.add(it)
        }

        val standardIncludeDirs = setOf("include", "includes", "inc", "headers", "src")
        val subRoots = mutableListOf<VirtualFile>()
        for (root in roots) {
            for (child in root.children) {
                if (child.isValid && child.isDirectory && child.name.lowercase() in standardIncludeDirs) {
                    subRoots.add(child)
                }
            }
        }
        roots.addAll(subRoots)

        return roots.toList()
    }

    fun resolveIncludeFile(
        project: Project,
        currentFile: VirtualFile?,
        directive: IncludeDirective,
        includeRoots: List<VirtualFile>
    ): VirtualFile? {
        val normalizedPath = directive.path.replace('\\', '/').trim().trimStart('/')
        if (normalizedPath.isEmpty()) return null

        return when (directive.type) {
            IncludeType.QUOTE -> {
                // 1. Quoted include searches relative to the current file's directory first
                val parent = currentFile?.parent
                if (parent != null && parent.isValid) {
                    val localFile = parent.findFileByRelativePath(normalizedPath)
                    if (localFile != null && localFile.isValid && !localFile.isDirectory) {
                        return localFile
                    }
                }

                // 2. Search in project include roots
                for (root in includeRoots) {
                    if (root.isValid) {
                        val file = root.findFileByRelativePath(normalizedPath)
                        if (file != null && file.isValid && !file.isDirectory) {
                            return file
                        }
                    }
                }

                // 3. Fallback: Search anywhere in project
                findFileInProject(project, normalizedPath)
            }

            IncludeType.ANGLE -> {
                // 1. Angle include does NOT search currentFile's directory
                // Search strictly in project include roots
                for (root in includeRoots) {
                    if (root.isValid) {
                        val file = root.findFileByRelativePath(normalizedPath)
                        if (file != null && file.isValid && !file.isDirectory) {
                            return file
                        }
                    }
                }

                // 2. If it contains directory separators (e.g. <sys/my_type.h> or <lib/api.h>),
                // search in project for path matching suffix
                if (normalizedPath.contains('/')) {
                    findFileInProject(project, normalizedPath)
                } else {
                    // Standard/system headers like <stdio.h>, <vector> won't match arbitrary project files
                    null
                }
            }
        }
    }

    fun findFileInProject(project: Project, relativePath: String): VirtualFile? {
        val normalized = relativePath.replace('\\', '/').trim().trimStart('/')
        val fileName = normalized.substringAfterLast('/')

        val roots = getIncludeRoots(project)
        for (root in roots) {
            if (root.isValid) {
                val found = root.findFileByRelativePath(normalized)
                if (found != null && found.isValid && !found.isDirectory) return found
            }
        }

        val projectRootManager = ProjectRootManager.getInstance(project)
        for (contentRoot in projectRootManager.contentRoots) {
            if (contentRoot.isValid) {
                val found = findFileRecursively(contentRoot, normalized, fileName)
                if (found != null) return found
            }
        }

        return null
    }

    private fun findFileRecursively(dir: VirtualFile, targetRelativePath: String, targetFileName: String): VirtualFile? {
        for (child in dir.children) {
            if (!child.isValid) continue
            if (!child.isDirectory) {
                if (child.name == targetFileName) {
                    if (targetRelativePath.contains('/')) {
                        val path = child.path.replace('\\', '/')
                        if (path.endsWith(targetRelativePath)) {
                            return child
                        }
                    } else {
                        return child
                    }
                }
            } else {
                val name = child.name
                if (!name.startsWith(".") && name != "build" && name != "cmake-build-debug" &&
                    name != "cmake-build-release" && name != "out" && name != "node_modules"
                ) {
                    val found = findFileRecursively(child, targetRelativePath, targetFileName)
                    if (found != null) return found
                }
            }
        }
        return null
    }

    fun loadFileContent(file: VirtualFile): String {
        val doc = FileDocumentManager.getInstance().getDocument(file)
        if (doc != null) {
            return doc.text
        }
        return try {
            VfsUtilCore.loadText(file)
        } catch (_: Exception) {
            ""
        }
    }

    fun getDisplayPath(project: Project, file: VirtualFile): String {
        val projectDir = project.guessProjectDir()
        if (projectDir != null) {
            val relPath = VfsUtilCore.getRelativePath(file, projectDir)
            if (relPath != null) return relPath
        }
        return file.name
    }

    fun resolveHeaders(
        project: Project,
        currentFile: VirtualFile?,
        fileContent: String,
        settings: ACompleteSettings.State
    ): List<HeaderFileContext> {
        val app = ApplicationManager.getApplication()
        return if (app != null) {
            app.runReadAction(Computable {
                doResolveHeaders(project, currentFile, fileContent, settings)
            })
        } else {
            doResolveHeaders(project, currentFile, fileContent, settings)
        }
    }

    internal fun doResolveHeaders(
        project: Project,
        currentFile: VirtualFile?,
        fileContent: String,
        settings: ACompleteSettings.State
    ): List<HeaderFileContext> {
        if (!settings.includeHeadersEnabled) return emptyList()

        val maxDepth = settings.getEffectiveDepth()
        if (maxDepth <= 0) return emptyList()

        val includeQuote = settings.includeQuoteHeaders
        val includeAngle = settings.includeAngleHeaders
        if (!includeQuote && !includeAngle) return emptyList()

        val includeRoots = getIncludeRoots(project)
        val visitedFiles = mutableSetOf<VirtualFile>()
        val result = mutableListOf<HeaderFileContext>()

        currentFile?.let { visitedFiles.add(it) }

        data class QueueItem(val file: VirtualFile, val depth: Int)
        val queue = ArrayDeque<QueueItem>()

        val initialDirectives = extractDirectives(fileContent)
        for (directive in initialDirectives) {
            if (directive.type == IncludeType.QUOTE && !includeQuote) continue
            if (directive.type == IncludeType.ANGLE && !includeAngle) continue

            val resolved = resolveIncludeFile(project, currentFile, directive, includeRoots)
            if (resolved != null && visitedFiles.add(resolved)) {
                val content = loadFileContent(resolved)
                val displayPath = getDisplayPath(project, resolved)
                result.add(
                    HeaderFileContext(
                        includePath = directive.path,
                        resolvedPath = displayPath,
                        content = content,
                        includeType = directive.type
                    )
                )
                if (maxDepth > 1) {
                    queue.add(QueueItem(resolved, 1))
                }
            }
        }

        while (queue.isNotEmpty()) {
            val (parentFile, depth) = queue.removeFirst()
            if (depth >= maxDepth) continue

            val parentContent = loadFileContent(parentFile)
            val directives = extractDirectives(parentContent)

            for (directive in directives) {
                if (directive.type == IncludeType.QUOTE && !includeQuote) continue
                if (directive.type == IncludeType.ANGLE && !includeAngle) continue

                val resolved = resolveIncludeFile(project, parentFile, directive, includeRoots)
                if (resolved != null && visitedFiles.add(resolved)) {
                    val content = loadFileContent(resolved)
                    val displayPath = getDisplayPath(project, resolved)
                    result.add(
                        HeaderFileContext(
                            includePath = directive.path,
                            resolvedPath = displayPath,
                            content = content,
                            includeType = directive.type
                        )
                    )
                    if (depth + 1 < maxDepth) {
                        queue.add(QueueItem(resolved, depth + 1))
                    }
                }
            }
        }

        return result
    }
}
