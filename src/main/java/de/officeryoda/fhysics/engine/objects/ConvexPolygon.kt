package de.officeryoda.fhysics.engine.objects

import de.officeryoda.fhysics.engine.collision.CollisionInfo
import de.officeryoda.fhysics.engine.math.Vector2

/**
 * A ConvexPolygon is a polygon where all interior angles are less than 180 degrees.
 */
class ConvexPolygon(
    vertices: Array<Vector2>, // must be CCW and in global space
    angle: Float = 0f,
) : Polygon(vertices, angle) {

    override val type = FhysicsObjectType.CONVEX_POLYGON

    override fun testCollision(other: FhysicsObject): CollisionInfo {
        return other.testCollision(this)
    }

    override fun findContactPoints(other: FhysicsObject, info: CollisionInfo): Array<Vector2> {
        return other.findContactPoints(this, info)
    }

    override fun toString(): String {
        return "ConvexPolygon(id=$id, position=$position, velocity=$velocity, mass=$mass, angle=$angle, angularVelocity=$angularVelocity, inertia=$inertia, static=$static, color=$color, vertices=${vertices.contentToString()})"
    }
}