package de.officeryoda.fhysics.rendering

import de.officeryoda.fhysics.engine.Vector2
import de.officeryoda.fhysics.engine.Vector2Int
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import java.awt.event.MouseWheelListener

class MouseListener internal constructor(private val fhysicsPanel: FhysicsPanel) : MouseAdapter(), MouseWheelListener {

    override fun mouseWheelMoved(e: MouseWheelEvent) {
        fhysicsPanel.onMouseWheel(e.wheelRotation)
    }

    override fun mouseClicked(e: MouseEvent?) {
        super.mouseClicked(e)
        println(e)
        fhysicsPanel.onMouseClick(Vector2Int(e!!.x, e.y))
    }
}
