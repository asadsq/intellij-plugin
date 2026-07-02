package com.github.asadsq.intellijplugin.folderhighlight

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ProjectViewNode
import com.intellij.ide.projectView.ProjectViewNodeDecorator
import com.intellij.openapi.components.service
import com.intellij.ui.JBColor
import java.awt.Color

/**
 * Tints every Project-view node that belongs to the currently highlighted folder,
 * so the folder's full contents read as one contiguous band regardless of how
 * deep or misleading the indentation gets.
 */
class FolderHighlightNodeDecorator : ProjectViewNodeDecorator {

    override fun decorate(node: ProjectViewNode<*>, data: PresentationData) {
        val file = node.virtualFile ?: return
        if (node.project?.service<FolderHighlightService>()?.isInHighlightedFolder(file) == true) {
            data.background = HIGHLIGHT_BACKGROUND
        }
    }

    companion object {
        /** Subtle, theme-aware tint: a soft blue in light themes, a muted blue-gray in dark ones. */
        private val HIGHLIGHT_BACKGROUND: Color = JBColor(Color(0xEA, 0xF1, 0xFB), Color(0x2A, 0x3A, 0x4D))
    }
}
