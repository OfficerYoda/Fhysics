package de.officeryoda.fhysics.engine.objects

import de.officeryoda.fhysics.engine.Projection
import de.officeryoda.fhysics.engine.Vector2
import de.officeryoda.fhysics.engine.collision.CollisionFinder
import de.officeryoda.fhysics.engine.collision.CollisionInfo
import kotlin.math.abs

class Polygon(
    position: Vector2,
    val vertices: Array<Vector2>,
    rotation: Float = 0f,
) : FhysicsObject(position, calculatePolygonArea(vertices), rotation) {

    init {
        ensureCCW(vertices)
    }

    override fun project(axis: Vector2): Projection {
        val min: Float = vertices.minOf { it.dot(axis) }
        val max: Float = vertices.maxOf { it.dot(axis) }
        return Projection(min, max)
    }

    override fun contains(pos: Vector2): Boolean {
        if (!boundingBox.contains(pos)) return false

        // TODO: This might be wrong
        // Rotate the point to make the polygon axis-aligned
        val rotatedPos: Vector2 = pos.rotatedAround(position, -rotation)

        // Check if the point is inside the axis-aligned polygon
        return vertices.all { it.dot(rotatedPos) >= 0 }
    }

    fun getTranslatedVertices(): List<Vector2> {
        return vertices.map { it.rotatedAround(position, rotation) }
    }

    override fun testCollision(other: FhysicsObject): CollisionInfo {
        return other.testCollision(this) // works because FhysicsObject is abstract (aka double dispatch)
    }

    override fun testCollision(other: Circle): CollisionInfo {
        return CollisionFinder.testCollision(this, other)
    }

    override fun testCollision(other: Rectangle): CollisionInfo {
        return CollisionFinder.testCollision(this, other)
    }

    override fun testCollision(other: Polygon): CollisionInfo {
        return CollisionFinder.testCollision(this, other)
    }

    override fun clone(): FhysicsObject {
        return Polygon(position, vertices.map { it.copy() }.toTypedArray(), rotation)
    }

    override fun toString(): String {
        return "Polygon(id=$id, position=$position, velocity=$velocity, acceleration=$acceleration, mass=$mass, static=$static, color=$color, vertices=${vertices.contentToString()})"
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
 * Ensures that the polygon vertices are in counter-clockwise order
 * by reversing the vertices if the polygon is clockwise
 *
 * @param vertices the vertices of the polygon
 */
private fun ensureCCW(vertices: Array<Vector2>) {
    // Calculate the signed area of the polygon
    var signedArea = 0f
    for (i: Int in vertices.indices) {
        val j: Int = (i + 1) % vertices.size
        signedArea += vertices[i].x * vertices[j].y - vertices[j].x * vertices[i].y
    }
    signedArea /= 2

    // Reverse the vertices if the polygon is CW
    if (signedArea < 0) {
        vertices.reverse()
    }
}