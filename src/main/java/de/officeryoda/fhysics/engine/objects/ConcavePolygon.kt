package de.officeryoda.fhysics.engine.objects

import de.officeryoda.fhysics.engine.collision.CollisionInfo
import de.officeryoda.fhysics.engine.math.Vector2

class ConcavePolygon(
    vertices: Array<Vector2>,
    subPolygonIndices: Array<Array<Int>>, // The indices of the vertices that form the convex sub-polygons
    angle: Float = 0f,
) : Polygon(vertices, angle) {

    override val type = FhysicsObjectType.CONCAVE_POLYGON

    /**
     * The concave polygons that form this polygon.
     */
    var subPolygons: MutableList<SubPolygon> = mutableListOf()

    init {
        // Create the sub-polygons
        for (indices: Array<Int> in subPolygonIndices) {
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

    override fun updateBoundingBox() {
        boundingBox.setFromPolygon(this)
        for (it: SubPolygon in subPolygons) {
            it.updateBoundingBox()
        }
    }

    override fun toString(): String {
        return "ConcavePolygon(id=$id, position=$position, velocity=$velocity, mass=$mass, angle=$angle, angularVelocity=$angularVelocity, inertia=$inertia, static=$static, color=$color, vertices=$vertices)"
    }
}

