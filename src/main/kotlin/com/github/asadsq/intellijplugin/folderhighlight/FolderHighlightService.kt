package com.github.asadsq.intellijplugin.folderhighlight

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile

/**
 * Holds the folder whose contents are currently highlighted in the Project view.
 *
 * The decorator reads [highlighted] for every node it renders; the selection
 * listener writes it whenever the user picks a different folder.
 */
@Service(Service.Level.PROJECT)
class FolderHighlightService(@Suppress("unused") private val project: Project) {

    /** The folder currently being highlighted, or `null` when nothing is highlighted. */
    @Volatile
    var highlighted: VirtualFile? = null

    /** Guards against attaching the selection listener to the tree more than once. */
    @Volatile
    var listenerInstalled: Boolean = false

    /** True when [file] is the highlighted folder itself or lives anywhere inside it. */
    fun isInHighlightedFolder(file: VirtualFile): Boolean {
        val root = highlighted ?: return false
        return VfsUtilCore.isAncestor(root, file, false)
    }
}
