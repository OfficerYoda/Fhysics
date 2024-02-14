package de.officeryoda.fhysics.objects

import de.officeryoda.fhysics.engine.FhysicsCore
import de.officeryoda.fhysics.engine.Vector2
import java.awt.Color

class Box(position: Vector2, val width: Double, val height: Double) :
    FhysicsObject(position, width * height) {
    val minX: Double
        get() = position.x

    val minY: Double
        get() = position.y

    val maxX: Double
        get() = position.x + width

    val maxY: Double
        get() = position.y + height

    init {
        color = Color.decode("#4287f5")
    }

    override fun update(dt: Double, gravity: Vector2) {
        // don't move
    }

    override fun handleCollision(other: Circle) {
        FhysicsCore.COLLISION_HANDLER.handleCollision(other, this)
    }

    override fun handleCollision(other: Box) {
        FhysicsCore.COLLISION_HANDLER.handleCollision(this, other)
    }

    override fun toString(): String {
        return "Box(position=$position, width=$width, height=$height) - ${super.toString()}"
    }
}
