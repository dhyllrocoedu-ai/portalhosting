package com.portalhost.desktop.window

import java.awt.AWTEvent
import java.awt.Cursor
import java.awt.Frame
import java.awt.Toolkit
import java.awt.event.MouseEvent

class NativeTitleBarDragHandler(
    private val window: Frame,
    private val titleBarHeightPx: Int
) {
    private var isPressed = false
    private var isDragging = false
    private var pressScreenX = 0
    private var pressScreenY = 0
    private var offsetFromWindowTopLeft = Pair(0, 0)

    private val awtListener = object : java.awt.event.AWTEventListener {
        override fun eventDispatched(event: AWTEvent) {
            if (event !is MouseEvent) return
            if (event.source !is java.awt.Component) return

            val component = event.source as java.awt.Component
            if (!isDescendantOf(component, window)) return

            when (event.id) {
                MouseEvent.MOUSE_PRESSED -> {
                    if (event.button != MouseEvent.BUTTON1) return
                    try {
                        val winLoc = window.locationOnScreen
                        val clickY = event.yOnScreen
                        if (clickY in winLoc.y until (winLoc.y + titleBarHeightPx)) {
                            isPressed = true
                            isDragging = false
                            pressScreenX = event.xOnScreen
                            pressScreenY = event.yOnScreen
                        }
                    } catch (_: Exception) {}
                }
                MouseEvent.MOUSE_DRAGGED -> {
                    if (!isPressed) return
                    val dx = event.xOnScreen - pressScreenX
                    val dy = event.yOnScreen - pressScreenY
                    if (!isDragging) {
                        if (dx * dx + dy * dy > DRAG_THRESHOLD_PX * DRAG_THRESHOLD_PX) {
                            isDragging = true
                            try {
                                val winLoc = window.locationOnScreen
                                offsetFromWindowTopLeft = Pair(
                                    event.xOnScreen - winLoc.x,
                                    event.yOnScreen - winLoc.y
                                )
                                window.cursor = Cursor(Cursor.MOVE_CURSOR)
                            } catch (_: Exception) {
                                isDragging = false
                                isPressed = false
                            }
                        }
                    }
                    if (isDragging) {
                        window.setLocation(
                            event.xOnScreen - offsetFromWindowTopLeft.first,
                            event.yOnScreen - offsetFromWindowTopLeft.second
                        )
                    }
                }
                MouseEvent.MOUSE_RELEASED -> {
                    if (isDragging) {
                        window.cursor = Cursor.getDefaultCursor()
                    }
                    isPressed = false
                    isDragging = false
                }
            }
        }
    }

    fun install() {
        Toolkit.getDefaultToolkit().addAWTEventListener(
            awtListener,
            AWTEvent.MOUSE_EVENT_MASK or AWTEvent.MOUSE_MOTION_EVENT_MASK
        )
    }

    fun uninstall() {
        Toolkit.getDefaultToolkit().removeAWTEventListener(awtListener)
    }

    private fun isDescendantOf(component: java.awt.Component, parent: Frame): Boolean {
        var current: java.awt.Component? = component
        while (current != null) {
            if (current === parent) return true
            current = current.parent
        }
        return false
    }

    companion object {
        private const val DRAG_THRESHOLD_PX = 4
    }
}
