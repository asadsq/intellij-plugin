package com.github.asadsq.intellijplugin.diffjump

import com.intellij.diff.tools.util.DiffDataKeys
import com.intellij.diff.tools.util.PrevNextDifferenceIterable
import com.intellij.openapi.actionSystem.DataContext

/**
 * Thin wrapper around the platform's [PrevNextDifferenceIterable], which is the same mechanism the
 * built-in "Next/Previous Difference" actions (F7 / Shift+F7) use. Reading it from a [DataContext]
 * at invocation time means we always act on whatever diff viewer currently has focus, whether that
 * is the commit dialog preview, an editor-tab diff, or a popup diff window.
 */
internal object DiffJumpNavigation {

    fun iterable(context: DataContext): PrevNextDifferenceIterable? =
        DiffDataKeys.PREV_NEXT_DIFFERENCE_ITERABLE.getData(context)

    fun goNext(context: DataContext): Boolean {
        val iterable = iterable(context) ?: return false
        if (!iterable.canGoNext()) return false
        iterable.goNext()
        return true
    }

    fun goPrev(context: DataContext): Boolean {
        val iterable = iterable(context) ?: return false
        if (!iterable.canGoPrev()) return false
        iterable.goPrev()
        return true
    }
}
