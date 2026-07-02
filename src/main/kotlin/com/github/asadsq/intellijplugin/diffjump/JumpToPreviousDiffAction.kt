package com.github.asadsq.intellijplugin.diffjump

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction

/** Hops the focused diff viewer to the previous changed block. */
internal class JumpToPreviousDiffAction : DumbAwareAction(
    "Jump to Previous Difference",
    "Move to the previous changed block in the diff",
    AllIcons.Actions.PreviousOccurence,
) {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = DiffJumpNavigation.canGoPrev(e.dataContext)
    }

    override fun actionPerformed(e: AnActionEvent) {
        DiffJumpNavigation.goPrev(e.dataContext)
    }
}
