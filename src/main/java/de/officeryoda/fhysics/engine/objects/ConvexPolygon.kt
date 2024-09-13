package de.officeryoda.fhysics.engine.objects

import de.officeryoda.fhysics.engine.collision.CollisionInfo
import de.officeryoda.fhysics.engine.math.Vector2

class ConvexPolygon(
    vertices: Array<Vector2>, // must be CCW and in global space
    angle: Float = 0f,
) : Polygon(vertices, angle) {

    override fun testCollision(other: FhysicsObject): CollisionInfo {
        return other.testCollision(this)
    }

    override fun findContactPoints(other: FhysicsObject, info: CollisionInfo): Array<Vector2> {
        return other.findContactPoints(this, info)
    }

    override fun clone(): FhysicsObject {
        return ConvexPolygon(vertices.map { position + it.copy() }.toTypedArray(), angle)
    }

    override fun toString(): String {
        return "ConvexPolygon(id=$id, position=$position, velocity=$velocity, mass=$mass, angle=$angle, angularVelocity=$angularVelocity, inertia=$inertia, static=$static, color=$color, vertices=${vertices.contentToString()})"
    }
}