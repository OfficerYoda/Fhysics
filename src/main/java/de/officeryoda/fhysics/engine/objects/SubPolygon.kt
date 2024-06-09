package de.officeryoda.fhysics.engine.objects

import de.officeryoda.fhysics.engine.Vector2
import de.officeryoda.fhysics.engine.collision.CollisionInfo

class SubPolygon(
    position: Vector2,
    center: Vector2,
    velocity: Vector2,
    vertices: Array<Vector2>,
    rotation: Float = 0f,
) : Polygon(position, velocity, vertices, rotation) {

    private val centerOffset = center - position
    override val center
        get() = position + centerOffset


    override fun testCollision(other: FhysicsObject): CollisionInfo {
        return other.testCollision(this)
    }

    override fun clone(): FhysicsObject {
        return SubPolygon(
            position.copy(),
            center.copy(),
            velocity.copy(),
            vertices.map { it.copy() + center }.toTypedArray(),
            rotation
        )
    }

    override fun toString(): String {
        return "SubPolygon(id=$id, position=$position, velocity=$velocity, acceleration=$acceleration, mass=$mass, static=$static, color=$color, vertices=${vertices.contentToString()})"
    }
}