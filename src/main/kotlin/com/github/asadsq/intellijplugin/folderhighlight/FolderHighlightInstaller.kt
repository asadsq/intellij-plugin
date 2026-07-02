package com.github.asadsq.intellijplugin.folderhighlight

import com.intellij.ide.projectView.ProjectView
import com.intellij.ide.projectView.ProjectViewNode
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.util.ui.tree.TreeUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * On project open, attaches a selection listener to the Project-view tree so that
 * picking a folder drives the highlight. The pane/tree may not exist the instant the
 * project opens, so we retry a few times until it is available.
 */
class FolderHighlightInstaller : ProjectActivity {

    override suspend fun execute(project: Project) {
        repeat(MAX_ATTEMPTS) {
            val installed = withContext(Dispatchers.EDT) { tryInstall(project) }
            if (installed) return
            delay(RETRY_DELAY_MS)
        }
    }

    private fun tryInstall(project: Project): Boolean {
        val service = project.service<FolderHighlightService>()
        if (service.listenerInstalled) return true

        val pane = ProjectView.getInstance(project).currentProjectViewPane ?: return false
        val tree = pane.tree ?: return false

        tree.addTreeSelectionListener {
            val selectedFolder = selectedFolder(tree.selectionPath?.lastPathComponent)
            if (selectedFolder != service.highlighted) {
                service.highlighted = selectedFolder
                pane.updateFromRoot(true)
            }
        }
        service.listenerInstalled = true
        return true
    }

    /** Returns the selected node's [com.intellij.openapi.vfs.VirtualFile] only when it is a folder, else null. */
    private fun selectedFolder(component: Any?) =
        (TreeUtil.getUserObject(component) as? ProjectViewNode<*>)
            ?.virtualFile
            ?.takeIf { it.isDirectory }

    companion object {
        private const val MAX_ATTEMPTS = 20
        private const val RETRY_DELAY_MS = 500L
    }
}
