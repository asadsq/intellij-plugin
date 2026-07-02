package com.github.asadsq.intellijplugin.diffjump

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUiKind
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.ex.ActionUtil

/**
 * Delegates diff navigation to the platform's built-in "NextDiff"/"PreviousDiff" actions — the same
 * ones behind F7 / Shift+F7 — via their public action ids. Evaluating them against a [DataContext]
 * at invocation time means we always act on whatever diff viewer currently has focus, whether that
 * is the commit dialog preview, an editor-tab diff, or a popup diff window.
 *
 * Going through [ActionManager]/[ActionUtil] keeps us on stable public API; the underlying
 * PrevNextDifferenceIterable machinery is marked @ApiStatus.Internal and is not touched directly.
 */
internal object DiffJumpNavigation {

    private const val NEXT_ACTION_ID = "NextDiff"
    private const val PREV_ACTION_ID = "PreviousDiff"
    private const val PLACE = "DiffJumpWidget"

    fun canGoNext(context: DataContext): Boolean = canPerform(NEXT_ACTION_ID, context)

    fun canGoPrev(context: DataContext): Boolean = canPerform(PREV_ACTION_ID, context)

    fun goNext(context: DataContext) = perform(NEXT_ACTION_ID, context)

    fun goPrev(context: DataContext) = perform(PREV_ACTION_ID, context)

    private fun action(id: String): AnAction? = ActionManager.getInstance().getAction(id)

    private fun eventFor(action: AnAction, context: DataContext): AnActionEvent =
        AnActionEvent.createEvent(action, context, null, PLACE, ActionUiKind.NONE, null)

    private fun canPerform(id: String, context: DataContext): Boolean {
        val action = action(id) ?: return false
        val event = eventFor(action, context)
        ActionUtil.performDumbAwareUpdate(action, event, false)
        return event.presentation.isEnabled
    }

    private fun perform(id: String, context: DataContext) {
        val action = action(id) ?: return
        val event = eventFor(action, context)
        ActionUtil.performDumbAwareUpdate(action, event, false)
        if (event.presentation.isEnabled) {
            ActionUtil.performActionDumbAwareWithCallbacks(action, event)
        }
    }
}
