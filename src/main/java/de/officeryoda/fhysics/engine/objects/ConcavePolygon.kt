package de.officeryoda.fhysics.engine.objects

import de.officeryoda.fhysics.engine.collision.CollisionInfo
import de.officeryoda.fhysics.engine.math.Vector2

/**
 * A ConcavePolygon is a polygon where at least one interior angle is greater than 180 degrees.
 *
 * It is made up of multiple convex [subPolygons].
 */
class ConcavePolygon(
    vertices: Array<Vector2>,
    subPolygonIndices: Array<IntArray>, // The indices of the vertices that form the convex sub-polygons
    angle: Float = 0f,
) : Polygon(vertices, angle) {

    override val type = FhysicsObjectType.CONCAVE_POLYGON

    /** The convex polygons that form this polygon */
    var subPolygons: MutableList<SubPolygon> = mutableListOf()

    init {
        // Create the sub-polygons
        for (indices: IntArray in subPolygonIndices) {
            val subVertices: Array<Vector2> = indices.map { vertices[it] + position }.toTypedArray()
            val relativePosition: Vector2 = calculatePolygonCenter(subVertices) - position
            subPolygons.add(SubPolygon(subVertices, this, relativePosition))
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

