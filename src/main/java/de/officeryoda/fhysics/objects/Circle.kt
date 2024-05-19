package de.officeryoda.fhysics.objects

import de.officeryoda.fhysics.engine.Vector2
import de.officeryoda.fhysics.engine.collision.CollisionFinder
import de.officeryoda.fhysics.engine.collision.CollisionInfo
import de.officeryoda.fhysics.engine.collision.Projection

class Circle(
    position: Vector2,
    val radius: Float,
) : FhysicsObject(position, (Math.PI * radius * radius).toFloat()) {

    override fun testCollision(other: Circle): CollisionInfo {
        return CollisionFinder.testCollision(this, other)
    }

    override fun testCollision(other: Rectangle): CollisionInfo {
        return CollisionFinder.testCollision(this, other)
    }

    fun project(axis: Vector2): Projection {
        // Project the circle's center onto the axis and return the range of scalar values
        val centerProjection: Float = position.dot(axis)
        return Projection(centerProjection - radius, centerProjection + radius)
    }

    override fun toString(): String {
        return "Circle(id=$id, position=$position, velocity=$velocity, acceleration=$acceleration, mass=$mass, static=$static, color=$color, radius=$radius)"
    }
}
