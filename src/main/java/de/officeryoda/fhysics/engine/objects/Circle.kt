package de.officeryoda.fhysics.engine.objects

import de.officeryoda.fhysics.engine.Projection
import de.officeryoda.fhysics.engine.Vector2
import de.officeryoda.fhysics.engine.collision.CollisionFinder
import de.officeryoda.fhysics.engine.collision.CollisionInfo

class Circle(
    position: Vector2,
    val radius: Float,
) : FhysicsObject(position, Vector2.ZERO, (Math.PI * radius * radius).toFloat()) {

    override fun project(axis: Vector2): Projection {
        // Project the circle's center onto the axis and return the range of scalar values
        val centerProjection: Float = position.dot(axis)
        return Projection(centerProjection - radius, centerProjection + radius)
    }

    override fun contains(pos: Vector2): Boolean {
        return pos.sqrDistance(position) <= radius * radius
    }

    override fun testCollision(other: FhysicsObject): CollisionInfo {
        return other.testCollision(this) // works because FhysicsObject is abstract (aka double dispatch)
    }

    override fun testCollision(other: Circle): CollisionInfo {
        return CollisionFinder.testCollision(this, other)
    }

    override fun testCollision(other: Polygon): CollisionInfo {
        return CollisionFinder.testCollision(other, this)
    }

    override fun clone(): FhysicsObject {
        return Circle(position.copy(), radius)
    }

    override fun toString(): String {
        return "Circle(id=$id, position=$position, velocity=$velocity, acceleration=$acceleration, mass=$mass, static=$static, color=$color, radius=$radius)"
    }
}
