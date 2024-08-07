package de.officeryoda.fhysics.engine.objects

import de.officeryoda.fhysics.engine.Vector2
import de.officeryoda.fhysics.engine.collision.CollisionInfo

class ConcavePolygon(
    vertices: Array<Vector2>,
    subPolygonIndices: Array<Array<Int>>, // the indices of the vertices that form the convex sub-polygons
    angle: Float = 0f,
) : Polygon(vertices, angle) {

    var subPolygons: MutableList<SubPolygon> = mutableListOf()

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
        val clone: ConcavePolygon =
            PolygonCreator.createPolygon(vertices.map { it + position }.toTypedArray(), angle) as ConcavePolygon
        return clone
    }

    override fun toString(): String {
        return "ConcavePolygon(id=$id, position=$position, velocity=$velocity, mass=$mass, angle=$angle, angularVelocity=$angularVelocity, inertia=$inertia, static=$static, color=$color, vertices=$vertices)"
    }
}

