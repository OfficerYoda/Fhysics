package de.officeryoda.fhysics.engine.objects

import de.officeryoda.fhysics.engine.collision.CollisionInfo
import de.officeryoda.fhysics.engine.math.Vector2

class SubPolygon(
    position: Vector2,
    center: Vector2,
    velocity: Vector2,
    vertices: Array<Vector2>,
    angularVelocity: Float,
    val parent: ConcavePolygon,
) : Polygon(position, velocity, vertices, parent.angle, angularVelocity) {

    override val type = FhysicsObjectType.SUB_POLYGON

    private val centerOffset: Vector2 = center - position

    override val position: Vector2
        get() = super.position + centerOffset.rotated(parent.angle)

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