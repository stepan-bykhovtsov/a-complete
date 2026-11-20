package dev.bykhovtsov.acomplete.mention

import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileVisitor

data class ProjectFileInfo(
    val virtualFile: VirtualFile,
    val relativePath: String
)

object ProjectFileUtils {

    val IGNORED_DIRECTORY_NAMES = setOf(
        ".git", ".svn", ".hg",
        ".idea", ".run",
        ".gradle",
        "build", "out", "target", "dist",
        "node_modules",
        ".cache",
        "bin", "obj",
        ".venv", "venv", "__pycache__"
    )

    fun listProjectFiles(project: Project, maxFiles: Int = 5000): List<ProjectFileInfo> {
        val projectDir = project.guessProjectDir() ?: return emptyList()
        val fileIndex = ProjectFileIndex.getInstance(project)
        val result = mutableListOf<ProjectFileInfo>()

        VfsUtilCore.visitChildrenRecursively(projectDir, object : VirtualFileVisitor<Void>() {
            override fun visitFile(file: VirtualFile): Boolean {
                ProgressManager.checkCanceled()

                if (result.size >= maxFiles) {
                    return false
                }

                val name = file.name

                if (file.isDirectory) {
                    if (name.startsWith(".") && name != ".") {
                        return false
                    }
                    if (name in IGNORED_DIRECTORY_NAMES) {
                        return false
                    }
                    if (fileIndex.isExcluded(file)) {
                        return false
                    }
                    return true
                }

                if (!file.isValid) return true
                if (fileIndex.isExcluded(file)) return true

                val relativePath = VfsUtilCore.getRelativePath(file, projectDir) ?: name
                val normalizedPath = relativePath.replace('\\', '/')
                result.add(ProjectFileInfo(file, normalizedPath))

                return true
            }
        })

        result.sortWith(
            compareBy<ProjectFileInfo> { it.relativePath.count { ch -> ch == '/' } }
                .thenBy { it.relativePath.lowercase() }
        )

        return result
    }
}
