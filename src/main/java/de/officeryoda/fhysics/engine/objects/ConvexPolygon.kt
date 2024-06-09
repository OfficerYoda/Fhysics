package de.officeryoda.fhysics.engine.objects

import de.officeryoda.fhysics.engine.Vector2
import de.officeryoda.fhysics.engine.collision.CollisionInfo

class ConvexPolygon : Polygon {

    // must be CCW and in global space
    constructor(position: Vector2, velocity: Vector2, vertices: Array<Vector2>, rotation: Float = 0f) : super(
        position,
        velocity,
        vertices,
        rotation
    )

    constructor(vertices: Array<Vector2>, rotation: Float = 0f) : this(
        calculatePolygonCenter(vertices),
        Vector2.ZERO,
        vertices,
        rotation
    )

    override fun testCollision(other: FhysicsObject): CollisionInfo {
        return other.testCollision(this)
    }

    override fun clone(): FhysicsObject {
        return ConvexPolygon(vertices.map { position + it.copy() }.toTypedArray(), rotation)
    }

    override fun toString(): String {
        return "Polygon(id=$id, position=$position, velocity=$velocity, acceleration=$acceleration, mass=$mass, static=$static, color=$color, vertices=${vertices.contentToString()})"
    }
}