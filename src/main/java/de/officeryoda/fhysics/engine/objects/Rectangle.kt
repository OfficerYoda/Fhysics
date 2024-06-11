package de.officeryoda.fhysics.engine.objects

import de.officeryoda.fhysics.engine.Projection
import de.officeryoda.fhysics.engine.Vector2
import de.officeryoda.fhysics.engine.collision.CollisionInfo
import kotlin.math.cos
import kotlin.math.sin

class Rectangle(
    position: Vector2,
    val width: Float,
    val height: Float,
    rotation: Float = 0f,
) : Polygon(createRectangleVertices(width, height).map { it + position }.toTypedArray(), rotation) {

    override fun getAxes(): Set<Vector2> {
        // Calculate the normals of the rectangle's sides based on its rotation
        val axis1 = Vector2(cos(rotation), sin(rotation))
        val axis2 = Vector2(-sin(rotation), cos(rotation))
        return setOf(axis1, axis2)
    }

    override fun project(axis: Vector2): Projection {
        val transformedVertices: List<Vector2> = getTransformedVertices()
        val min: Float = transformedVertices.minOf { it.dot(axis) }
        val max: Float = transformedVertices.maxOf { it.dot(axis) }
        return Projection(min, max)
    }

    override fun contains(pos: Vector2): Boolean {
        // Rotate the point to make the rectangle axis-aligned
        val rotatedPos: Vector2 = pos.rotatedAround(position, -rotation)

        val halfWidth: Float = width / 2f
        val halfHeight: Float = height / 2f

        // Check if the point is inside the axis-aligned rectangle
        return rotatedPos.x in (position.x - halfWidth)..(position.x + halfWidth) &&
                rotatedPos.y in (position.y - halfHeight)..(position.y + halfHeight)
    }

    override fun getTransformedVertices(): List<Vector2> {
        // Get the four corners of the rectangle before rotation
        val corners: Array<Vector2> = createRectangleVertices(width, height)

        // Rotate the rectangle's corners
        return corners.map { it.rotatedAround(Vector2.ZERO, rotation) + position }
    }

    override fun testCollision(other: FhysicsObject): CollisionInfo {
        return other.testCollision(this) // works because FhysicsObject is abstract (aka double dispatch)
    }

    override fun clone(): FhysicsObject {
        return Rectangle(position.copy(), width, height, rotation)
    }

    override fun toString(): String {
        return "Rectangle(id=$id, position=$position, velocity=$velocity, acceleration=$acceleration, mass=$mass, static=$static, color=$color, width=$width, height=$height, rotation=$rotation)"
    }
}

/**
 * Creates the vertices of a rectangle with the given width and height
 *
 * @param width The width of the rectangle
 * @param height The height of the rectangle
 * @return The vertices of the rectangle
 */
fun createRectangleVertices(width: Float, height: Float): Array<Vector2> {
    val halfWidth: Float = width / 2
    val halfHeight: Float = height / 2

    return arrayOf(
        Vector2(-halfWidth, -halfHeight),
        Vector2(halfWidth, -halfHeight),
        Vector2(halfWidth, halfHeight),
        Vector2(-halfWidth, halfHeight)
    )
}
