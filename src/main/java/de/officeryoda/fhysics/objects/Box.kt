package de.officeryoda.fhysics.objects

import de.officeryoda.fhysics.engine.FhysicsCore
import de.officeryoda.fhysics.engine.Vector2

class Box(position: Vector2, val width: Double, val height: Double) :
    FhysicsObject(position, width * height) {

    override fun toString(): String {
        return "Box(position=$position, width=$width, height=$height) - ${super.toString()}"
    }

    override fun handleCollision(other: Circle) {
        FhysicsCore.COLLISION_HANDLER.handleCollision(other, this)
    }

    override fun handleCollision(other: Box) {
        FhysicsCore.COLLISION_HANDLER.handleCollision(this, other)
    }
}
