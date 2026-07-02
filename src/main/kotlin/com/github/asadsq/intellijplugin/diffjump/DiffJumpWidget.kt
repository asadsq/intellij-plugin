package com.github.asadsq.intellijplugin.diffjump

import com.intellij.diff.tools.util.base.DiffViewerBase
import com.intellij.diff.tools.util.base.DiffViewerListener
import com.intellij.icons.AllIcons
import com.intellij.ide.DataManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.AlphaComposite
import java.awt.Color
import java.awt.Cursor
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.HierarchyEvent
import java.awt.event.HierarchyListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLayeredPane
import javax.swing.SwingUtilities

/**
 * A floating "next diff" control rendered as a rounded pill pinned to the bottom of a diff editor —
 * conceptually like DocuSign's "next field" tab. It lives in the host window's [JLayeredPane] so it
 * paints above the editor, and is re-anchored whenever the editor moves or resizes.
 *
 * Navigation is delegated to the platform via [DiffJumpNavigation], read from the diff viewer's data
 * context at click time, so it stays in sync with F7 / scrolling / the diff gutter markers.
 *
 * Two pieces of timing matter and are handled here:
 *  - the editor is not in a window when the viewer is created, so installation is deferred until the
 *    component is actually showing (via the hierarchy listener);
 *  - the diff is computed asynchronously, so the change count is refreshed on every [onAfterRediff].
 *
 * @param viewer         the diff viewer; used to listen for re-diff completion
 * @param editor         the editor the pill is anchored to (the "your changes" side of the diff)
 * @param countProvider  supplies the current number of changed blocks, or null if unavailable
 */
internal class DiffJumpWidget(
    private val viewer: DiffViewerBase,
    private val editor: EditorEx,
    private val countProvider: () -> Int?,
) : JComponent(), Disposable {

    private val countLabel = JBLabel().apply {
        foreground = UIUtil.getContextHelpForeground()
        border = JBUI.Borders.empty(0, 6)
    }
    private val prevButton = pillButton(AllIcons.Actions.PreviousOccurence, "Move to the previous changed block in the diff") {
        navigate(next = false)
    }
    private val nextButton = pillButton(AllIcons.Actions.NextOccurence, "Move to the next changed block in the diff") {
        navigate(next = true)
    }

    private val dataComponent: JComponent get() = viewer.component
    private val editorComponent: JComponent get() = editor.component

    private var layeredPane: JLayeredPane? = null

    private val anchorListener = object : ComponentAdapter() {
        override fun componentResized(e: ComponentEvent) = relayout()
        override fun componentMoved(e: ComponentEvent) = relayout()
    }

    private val hierarchyListener = HierarchyListener { e ->
        if (e.changeFlags and HierarchyEvent.SHOWING_CHANGED.toLong() != 0L) relayout()
    }

    private val diffListener = object : DiffViewerListener() {
        override fun onAfterRediff() {
            SwingUtilities.invokeLater { relayout() }
        }
    }

    init {
        isOpaque = false
        layout = BoxLayout(this, BoxLayout.X_AXIS)
        border = JBUI.Borders.empty(4, 8)
        add(countLabel)
        add(prevButton)
        add(nextButton)
    }

    /** Starts tracking the editor; the pill is installed/shown once the editor is on screen. */
    fun attach() {
        editorComponent.addHierarchyListener(hierarchyListener)
        editorComponent.addComponentListener(anchorListener)
        viewer.addListener(diffListener)
        relayout()
    }

    /** Installs into the layered pane if needed, then re-anchors and refreshes visibility. */
    private fun relayout() {
        val anchor = editorComponent
        val pane = SwingUtilities.getRootPane(anchor)?.layeredPane
        if (pane == null || !anchor.isShowing) {
            isVisible = false
            return
        }
        if (layeredPane !== pane) {
            layeredPane?.remove(this)
            pane.add(this, JLayeredPane.POPUP_LAYER as Any)
            layeredPane = pane
        }

        val count = refreshCount()
        if (count == 0) {
            isVisible = false
            pane.repaint()
            return
        }
        refreshEnablement()

        val size = preferredSize
        val origin = SwingUtilities.convertPoint(anchor.parent, anchor.location, pane)
        val margin = JBUI.scale(14)
        val x = origin.x + (anchor.width - size.width) / 2
        val y = origin.y + anchor.height - size.height - margin
        setBounds(x, y, size.width, size.height)
        isVisible = true
        pane.revalidate()
        pane.repaint()
    }

    /** @return the current change count, also updating the label text. */
    private fun refreshCount(): Int {
        val count = countProvider() ?: 0
        countLabel.text = if (count == 1) "1 change" else "$count changes"
        return count
    }

    private fun refreshEnablement() {
        val context = dataContext()
        prevButton.isEnabled = DiffJumpNavigation.canGoPrev(context)
        nextButton.isEnabled = DiffJumpNavigation.canGoNext(context)
    }

    private fun dataContext() = DataManager.getInstance().getDataContext(dataComponent)

    private fun navigate(next: Boolean) {
        val context = dataContext()
        if (next) DiffJumpNavigation.goNext(context) else DiffJumpNavigation.goPrev(context)
        refreshEnablement()
    }

    private fun pillButton(icon: javax.swing.Icon, tooltip: String, onClick: () -> Unit): JButton =
        JButton(icon).apply {
            toolTipText = tooltip
            isOpaque = false
            isContentAreaFilled = false
            isBorderPainted = false
            isFocusPainted = false
            isRolloverEnabled = true
            border = JBUI.Borders.empty(4)
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            addActionListener { onClick() }
            // Light hover affordance.
            addMouseListener(object : MouseAdapter() {
                override fun mouseEntered(e: MouseEvent) {
                    if (isEnabled) isContentAreaFilled = true
                }

                override fun mouseExited(e: MouseEvent) {
                    isContentAreaFilled = false
                }
            })
        }

    override fun paintComponent(g: Graphics) {
        val g2 = g.create() as Graphics2D
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            val arc = JBUI.scale(18)
            val bodyW = width - JBUI.scale(2)
            val bodyH = height - JBUI.scale(2)
            // Soft drop shadow.
            g2.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.18f)
            g2.color = JBColor.BLACK
            g2.fillRoundRect(JBUI.scale(1), JBUI.scale(2), bodyW, bodyH, arc, arc)
            // Pill body.
            g2.composite = AlphaComposite.SrcOver
            g2.color = pillBackground()
            g2.fillRoundRect(0, 0, bodyW, bodyH, arc, arc)
            g2.color = JBColor.border()
            g2.drawRoundRect(0, 0, bodyW, bodyH - 1, arc, arc)
        } finally {
            g2.dispose()
        }
        super.paintComponent(g)
    }

    private fun pillBackground(): Color =
        JBColor.namedColor("Editor.Toolbar.background", UIUtil.getPanelBackground())

    override fun dispose() {
        editorComponent.removeHierarchyListener(hierarchyListener)
        editorComponent.removeComponentListener(anchorListener)
        viewer.removeListener(diffListener)
        layeredPane?.let {
            it.remove(this)
            it.revalidate()
            it.repaint()
        }
        layeredPane = null
    }

    override fun getPreferredSize(): Dimension {
        val base = super.getPreferredSize()
        // Reserve room for the shadow we paint outside the pill body.
        return Dimension(base.width + JBUI.scale(2), base.height + JBUI.scale(2))
    }
}
