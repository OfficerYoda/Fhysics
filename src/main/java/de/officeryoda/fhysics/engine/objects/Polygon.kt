package de.officeryoda.fhysics.engine.objects

import de.officeryoda.fhysics.engine.Projection
import de.officeryoda.fhysics.engine.Vector2
import de.officeryoda.fhysics.engine.collision.CollisionFinder
import de.officeryoda.fhysics.engine.collision.CollisionInfo

class Polygon(
    position: Vector2,
    val vertices: Array<Vector2>,
    rotation: Float = 0f,
) : FhysicsObject(position, calculatePolygonArea(vertices), rotation) {

    override fun project(axis: Vector2): Projection {
        val min: Float = vertices.minOf { it.dot(axis) }
        val max: Float = vertices.maxOf { it.dot(axis) }
        return Projection(min, max)
    }

    override fun contains(pos: Vector2): Boolean {
        // TODO: This might be wrong
        // Rotate the point to make the polygon axis-aligned
        val rotatedPos: Vector2 = pos.rotatedAround(position, -rotation)

        // Check if the point is inside the axis-aligned polygon
        return vertices.all { it.dot(rotatedPos) >= 0 }
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

private fun calculatePolygonArea(vertices: Array<Vector2>): Float {
    var area = 0f
    for (i: Int in vertices.indices) {
        val j: Int = (i + 1) % vertices.size
        area += vertices[i].x * vertices[j].y
        area -= vertices[j].x * vertices[i].y
    }
    return area / 2
}