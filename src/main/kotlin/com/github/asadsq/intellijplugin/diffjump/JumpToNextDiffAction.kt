package com.github.asadsq.intellijplugin.diffjump

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction

/** Hops the focused diff viewer to the next changed block. */
internal class JumpToNextDiffAction : DumbAwareAction(
    "Jump to Next Difference",
    "Move to the next changed block in the diff",
    AllIcons.Actions.NextOccurence,
) {
    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = DiffJumpNavigation.iterable(e.dataContext)?.canGoNext() == true
    }

    override fun actionPerformed(e: AnActionEvent) {
        DiffJumpNavigation.goNext(e.dataContext)
    }
}
