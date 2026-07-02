package com.github.asadsq.intellijplugin.diffjump

import com.intellij.diff.DiffContext
import com.intellij.diff.DiffExtension
import com.intellij.diff.FrameDiffTool
import com.intellij.diff.requests.DiffRequest
import com.intellij.diff.tools.fragmented.UnifiedDiffViewer
import com.intellij.diff.tools.simple.SimpleDiffViewer
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.util.Disposer

/**
 * Injects the floating [DiffJumpWidget] into text diff viewers — including the side-by-side preview
 * shown in the commit dialog (a [SimpleDiffViewer]) and the inline unified view. Non-text viewers
 * (binary, directory, etc.) are skipped.
 */
internal class DiffJumpExtension : DiffExtension() {

    override fun onViewerCreated(
        viewer: FrameDiffTool.DiffViewer,
        context: DiffContext,
        request: DiffRequest,
    ) {
        val editor: EditorEx
        val countProvider: () -> Int?
        when (viewer) {
            // Side-by-side: anchor to the right ("your changes") editor.
            is SimpleDiffViewer -> {
                editor = viewer.editor2
                countProvider = { viewer.diffChanges?.size }
            }
            is UnifiedDiffViewer -> {
                editor = viewer.editor
                countProvider = { viewer.diffChanges?.size }
            }
            else -> return
        }

        val widget = DiffJumpWidget(viewer, editor, countProvider)
        widget.attach()
        Disposer.register(viewer, widget)
    }
}
