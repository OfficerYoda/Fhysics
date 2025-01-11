package de.officeryoda.fhysics.engine.objects

import de.officeryoda.fhysics.engine.collision.CollisionInfo
import de.officeryoda.fhysics.engine.math.Vector2

/**
 * A SubPolygon is a convex polygon that is part of a [ConcavePolygon][parent].
 */
class SubPolygon(
    vertices: Array<Vector2>,
    val parent: ConcavePolygon,
    /** The relative position of the sub-polygon to the parent polygon */
    private val relativePosition: Vector2,
) : Polygon(
    // Velocity and angularVelocity are always retrieved from the parent polygon
    parent.position, parent.velocity,
    vertices, parent.angle, 0f
) {

    override val type = FhysicsObjectType.SUB_POLYGON

    /** The position of the sub-polygon in world space */
    override val position: Vector2
        get() = parent.position + relativePosition.rotated(parent.angle)

    override var angle: Float
        get() = parent.angle
        set(_) = throw Exception("Cannot set angle of SubPolygon, use parent.angle instead")

    override var static: Boolean
        get() = parent.static
        set(value) {
            parent.static = value
        }

    override fun testCollision(other: FhysicsObject): CollisionInfo {
        return other.testCollision(this)
    }

    override fun findContactPoints(other: FhysicsObject, info: CollisionInfo): Array<Vector2> {
        return other.findContactPoints(this, info)
    }

    override fun toString(): String {
        return "SubPolygon(id=$id, position=$position, velocity=$velocity, acceleration=$acceleration, mass=$mass, angle=$angle, angularVelocity=$angularVelocity, inertia=$inertia, static=$static, color=$color, vertices=${vertices.contentToString()}, parent=$parent)"
    }
}