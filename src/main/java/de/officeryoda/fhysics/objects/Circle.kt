package de.officeryoda.fhysics.objects

import de.officeryoda.fhysics.engine.FhysicsCore
import de.officeryoda.fhysics.engine.Vector2

class Circle(
    position: Vector2,
    var radius: Double
) :
    FhysicsObject(position, Math.PI * radius * radius) {

    override fun toString(): String {
        return "Circle(position=$position, radius=$radius) - ${super.toString()}"
    }

    override fun handleCollision(other: Circle) {
        FhysicsCore.COLLISION_HANDLER.handleCollision(this, other)
    }

    override fun handleCollision(other: Box) {
        FhysicsCore.COLLISION_HANDLER.handleCollision(this, other)
    }
}
