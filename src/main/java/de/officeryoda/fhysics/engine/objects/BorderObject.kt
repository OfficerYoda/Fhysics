package de.officeryoda.fhysics.engine.objects

import de.officeryoda.fhysics.engine.Vector2
import de.officeryoda.fhysics.engine.collision.CollisionInfo

class BorderObject(
    private val axis: Vector2,
    vertices: Array<Vector2>,
) : Polygon(vertices, 0f) {

    init {
        static = true
    }

    override fun getAxes(): Set<Vector2> {
        return setOf(axis)
    }

    override fun testCollision(other: FhysicsObject): CollisionInfo {
        return other.testCollision(this)
    }

    override fun findContactPoints(other: FhysicsObject, info: CollisionInfo): Array<Vector2> {
        return other.findContactPoints(this, info)
    }

    override fun clone(): FhysicsObject {
        return BorderObject(axis, getTransformedVertices())
    }
}