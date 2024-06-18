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
        // Project the circle's center onto the axis and add and subtract the radius to get the projection
        val centerProjection: Float = position.dot(axis)
        return Projection(centerProjection - radius, centerProjection + radius)
    }

    override fun contains(pos: Vector2): Boolean {
        return pos.sqrDistanceTo(position) <= radius * radius
    }

    override fun calculateInertia(): Float {
        return (mass * radius * radius) / 2f
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

    override fun findContactPoints(other: FhysicsObject, info: CollisionInfo): Array<Vector2> {
        return other.findContactPoints(this, info)
    }

    override fun findContactPoints(other: Circle, info: CollisionInfo): Array<Vector2> {
        return CollisionFinder.findContactPoints(this, other, info).first
    }

    override fun findContactPoints(other: Polygon, info: CollisionInfo): Array<Vector2> {
        return CollisionFinder.findContactPoints(other, this, info).first
    }

    override fun clone(): FhysicsObject {
        return Circle(position.copy(), radius)
    }

    override fun toString(): String {
        return "Circle(id=$id, position=$position, velocity=$velocity, acceleration=$acceleration, mass=$mass, static=$static, color=$color, radius=$radius)"
    }
}
