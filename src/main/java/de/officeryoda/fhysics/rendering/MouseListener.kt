package de.officeryoda.fhysics.rendering

import de.officeryoda.fhysics.engine.Vector2Int
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import java.awt.event.MouseWheelListener

class MouseListener internal constructor(private val fhysicsPanel: FhysicsPanel) : MouseAdapter(), MouseWheelListener {

    override fun mouseWheelMoved(e: MouseWheelEvent) {
        fhysicsPanel.onMouseWheel(e.wheelRotation)
    }

    override fun mousePressed(e: MouseEvent) {
        super.mouseClicked(e)
        fhysicsPanel.onMousePressed(Vector2Int(e.x, e.y))
    }
}
