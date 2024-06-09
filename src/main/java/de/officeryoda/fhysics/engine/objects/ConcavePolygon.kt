package de.officeryoda.fhysics.engine.objects

import de.officeryoda.fhysics.engine.Vector2
import de.officeryoda.fhysics.engine.collision.CollisionInfo

class ConcavePolygon(
    vertices: Array<Vector2>,
    subPolygonIndices: Array<Array<Int>>, // the indices of the vertices that form the convex sub-polygons
    rotation: Float = 0f,
) : Polygon(vertices, rotation) {

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
            val center = calculatePolygonCenter(subVertices)
            subPolygons.add(SubPolygon(position, center, velocity, subVertices, rotation))
        }
    }

    override fun testCollision(other: FhysicsObject): CollisionInfo {
        return other.testCollision(this)
    }

    override fun clone(): FhysicsObject {
        TODO("Not yet implemented")
    }
}

