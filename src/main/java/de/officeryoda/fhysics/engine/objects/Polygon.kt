package de.officeryoda.fhysics.engine.objects

import de.officeryoda.fhysics.engine.Projection
import de.officeryoda.fhysics.engine.Vector2
import de.officeryoda.fhysics.engine.collision.CollisionFinder
import de.officeryoda.fhysics.engine.collision.CollisionInfo
import kotlin.math.abs

abstract class Polygon(
    val vertices: Array<Vector2>, // must be CCW and in global space
    rotation: Float = 0f,
) : FhysicsObject(calculatePolygonCenter(vertices), calculatePolygonArea(vertices), rotation) {

    init {
        vertices.forEach { it -= position }
        color = colorFromIndex(2)
    }

    open fun getAxes(): Set<Vector2> {
        // Calculate the normals of the polygon's sides based on its rotation
        val axes: MutableSet<Vector2> = mutableSetOf()
        val transformedVertices: List<Vector2> = getTransformedVertices()

        for (i: Int in transformedVertices.indices) {
            val j: Int = (i + 1) % transformedVertices.size
            val edge: Vector2 = transformedVertices[j] - transformedVertices[i]
            val normal: Vector2 = Vector2(-edge.y, edge.x).normalized()
            axes.add(normal)
        }

        return axes
    }

    override fun project(axis: Vector2): Projection {
        val transformedVertices: List<Vector2> = getTransformedVertices()
        val min: Float = transformedVertices.minOf { it.dot(axis) }
        val max: Float = transformedVertices.maxOf { it.dot(axis) }
        return Projection(min, max)
    }

    override fun contains(pos: Vector2): Boolean {
        if (!boundingBox.contains(pos)) return false

        val transformedVertices: List<Vector2> = getTransformedVertices()
        var intersects = 0

        for (i: Int in transformedVertices.indices) {
            val j: Int = (i + 1) % transformedVertices.size
            val xi: Float = transformedVertices[i].x
            val yi: Float = transformedVertices[i].y
            val xj: Float = transformedVertices[j].x
            val yj: Float = transformedVertices[j].y

            val isIntersect: Boolean =
                ((yi > pos.y) != (yj > pos.y)) && (pos.x < (xj - xi) * (pos.y - yi) / (yj - yi) + xi)
            if (isIntersect) intersects++
        }

        return intersects and 1 == 1
    }

    /**
     * Transforms the vertices from local space to world space
     * taking into account the position and rotation of the polygon
     *
     * @return The transformed vertices
     */
    open fun getTransformedVertices(): List<Vector2> {
        return vertices.map { it.rotatedAround(Vector2.ZERO, rotation) + position }
    }

    abstract override fun testCollision(other: FhysicsObject): CollisionInfo

    override fun testCollision(other: Circle): CollisionInfo {
        return CollisionFinder.testCollision(this, other)
    }

    override fun testCollision(other: Rectangle): CollisionInfo {
        return CollisionFinder.testCollision(this, other)
    }

    override fun testCollision(other: Polygon): CollisionInfo {
        return CollisionFinder.testCollision(this, other)
    }
}

/**
 * Calculates the area of a polygon
 * The area will always be positive
 *
 * @param vertices the vertices of the polygon
 */
private fun calculatePolygonArea(vertices: Array<Vector2>): Float {
    var signedArea = 0f
    for (i: Int in vertices.indices) {
        val j: Int = (i + 1) % vertices.size
        signedArea += vertices[i].x * vertices[j].y
        signedArea -= vertices[j].x * vertices[i].y
    }
    signedArea /= 2

    // Ensure that the area is positive
    return abs(signedArea)
}

/**
 * Calculates the center of a polygon
 *
 * @param vertices the vertices of the polygon
 */
fun calculatePolygonCenter(vertices: Array<Vector2>): Vector2 {
    return vertices.reduce { acc, vector2 -> acc + vector2 } / vertices.size.toFloat()
}
