package de.officeryoda.fhysics.engine.objects

import de.officeryoda.fhysics.engine.Vector2
import de.officeryoda.fhysics.engine.collision.CollisionInfo

class ConcavePolygon(
    vertices: Array<Vector2>,
    subPolygonIndices: Array<Array<Int>>, // the indices of the vertices that form the convex sub-polygons
    angle: Float = 0f,
) : Polygon(vertices, angle) {

    var subPolygons: MutableList<SubPolygon> = mutableListOf()
    override var static: Boolean
        get() = super.static
        set(value) {
            super.static = value
            subPolygons.forEach { it.static = value }
        }

    init {
        subPolygonIndices.forEach { indices ->
            val subVertices: Array<Vector2> = indices.map { vertices[it] + position }.toTypedArray()
            val center: Vector2 = calculatePolygonCenter(subVertices)
            subPolygons.add(SubPolygon(position, center, velocity, subVertices, angularVelocity, this))
        }
    }

    override fun testCollision(other: FhysicsObject): CollisionInfo {
        return other.testCollision(this)
    }

    override fun findContactPoints(other: FhysicsObject, info: CollisionInfo): Array<Vector2> {
        return other.findContactPoints(this, info)
    }

    override fun clone(): FhysicsObject {
        TODO("Not yet implemented")
    }
}

