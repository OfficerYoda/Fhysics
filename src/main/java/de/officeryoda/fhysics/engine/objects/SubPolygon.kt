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

    private val centerOffset: Vector2 = center - position
    override val position: Vector2
        get() = super.position + centerOffset

    override fun testCollision(other: FhysicsObject): CollisionInfo {
        return other.testCollision(this)
    }

    override fun findContactPoints(other: FhysicsObject, info: CollisionInfo): Array<Vector2> {
        return other.findContactPoints(this, info)
    }

    override fun clone(): FhysicsObject {
        return SubPolygon(
            position.copy(),
            calculatePolygonCenter(vertices),
            velocity.copy(),
            vertices.map { it.copy() + centerOffset }.toTypedArray(),
            angle
        )
    }

    override fun toString(): String {
        return "SubPolygon(id=$id, position=$position, velocity=$velocity, acceleration=$acceleration, mass=$mass, static=$static, color=$color, vertices=${vertices.contentToString()})"
    }
}