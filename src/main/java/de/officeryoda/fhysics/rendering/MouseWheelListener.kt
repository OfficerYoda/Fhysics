package de.officeryoda.fhysics.rendering

import java.awt.event.MouseWheelEvent
import java.awt.event.MouseWheelListener

class MouseWheelListener internal constructor(private val fhysicsPanel: FhysicsPanel) : MouseWheelListener {
    override fun mouseWheelMoved(e: MouseWheelEvent) {
        fhysicsPanel.onMouseWheel(e.wheelRotation)
    }
}
