package de.officeryoda.fhysics.objects

import de.officeryoda.fhysics.engine.Vector2
import de.officeryoda.fhysics.engine.collision.CollisionFinder
import de.officeryoda.fhysics.engine.collision.CollisionPoints

class Circle(
    position: Vector2,
    var radius: Double
) :
    FhysicsObject(position, Math.PI * radius * radius) {

    override fun testCollision(other: Circle): CollisionPoints {
        return CollisionFinder.testCollision(this, other)
    }

    override fun testCollision(other: Box): CollisionPoints {
        return CollisionFinder.testCollision(this, other)
    }

    override fun toString(): String {
        return "Circle(id=$id, position=$position, velocity=$velocity, acceleration=$acceleration, mass=$mass, color=$color, radius=$radius)"
    }
}
