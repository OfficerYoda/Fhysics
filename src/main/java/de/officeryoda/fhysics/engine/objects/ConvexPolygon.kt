package de.officeryoda.fhysics.engine.objects

import de.officeryoda.fhysics.engine.Vector2
import de.officeryoda.fhysics.engine.collision.CollisionInfo

class ConvexPolygon(
    vertices: Array<Vector2>, // must be CCW and in global space
    rotation: Float = 0f,
) : Polygon(vertices, rotation) {

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