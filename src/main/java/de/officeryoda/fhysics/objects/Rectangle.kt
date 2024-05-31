package de.officeryoda.fhysics.objects

import de.officeryoda.fhysics.engine.Vector2
import de.officeryoda.fhysics.engine.collision.CollisionFinder
import de.officeryoda.fhysics.engine.collision.CollisionInfo
import de.officeryoda.fhysics.engine.collision.Projection
import java.awt.Color
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

class Rectangle(
    position: Vector2,
    val width: Float,
    val height: Float,
    rotation: Float = 0f
) : FhysicsObject(position, width * height, rotation) {

    val minX: Float
    val maxX: Float
    val minY: Float
    val maxY: Float

    init {
        color = Color.decode("#4287f5")
        static = true // rectangles must be static for min/max values to work

        val offsets: Vector2 = calculateRotationOffsets()
        minX = position.x - offsets.x
        maxX = position.x + offsets.x
        minY = position.y - offsets.y
        maxY = position.y + offsets.y
    }

    override fun testCollision(other: Circle): CollisionInfo {
        return CollisionFinder.testCollision(other, this)
    }

    override fun testCollision(other: Rectangle): CollisionInfo {
        return CollisionFinder.testCollision(this, other)
    }

    private fun calculateRotationOffsets(): Vector2 {
        val cosRot: Float = cos(rotation)
        val sinRot: Float = sin(rotation)
        val halfWidth: Float = width / 2
        val halfHeight: Float = height / 2

        // idk how this works, but it does
        val offsetX: Float = abs(halfWidth * cosRot) + abs(halfHeight * sinRot)
        val offsetY: Float = abs(halfWidth * sinRot) + abs(halfHeight * cosRot)

        return Vector2(offsetX, offsetY)
    }

    fun getAxes(): List<Vector2> {
        // Calculate the normals of the rectangle's sides based on its rotation
        val axis1 = Vector2(cos(rotation), sin(rotation))
        val axis2 = Vector2(-sin(rotation), cos(rotation))
        return listOf(axis1, axis2)
    }

    override fun project(axis: Vector2): Projection {
        // Project the rectangle's vertices onto the axis and return the range of scalar values
        val vertices: List<Vector2> = getVertices()
        val min: Float = vertices.minOf { it.dot(axis) }
        val max: Float = vertices.maxOf { it.dot(axis) }
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

    private fun getVertices(): List<Vector2> {
        val halfWidth: Float = width / 2
        val halfHeight: Float = height / 2

        // Calculate the four corners of the rectangle before rotation
        val topLeft = Vector2(position.x - halfWidth, position.y - halfHeight)
        val topRight = Vector2(position.x + halfWidth, position.y - halfHeight)
        val bottomRight = Vector2(position.x + halfWidth, position.y + halfHeight)
        val bottomLeft = Vector2(position.x - halfWidth, position.y + halfHeight)

        // Rotate the corners around the rectangle's center
        val rotatedTopLeft: Vector2 = topLeft.rotatedAround(position, rotation)
        val rotatedTopRight: Vector2 = topRight.rotatedAround(position, rotation)
        val rotatedBottomRight: Vector2 = bottomRight.rotatedAround(position, rotation)
        val rotatedBottomLeft: Vector2 = bottomLeft.rotatedAround(position, rotation)

        return listOf(rotatedTopLeft, rotatedTopRight, rotatedBottomRight, rotatedBottomLeft)
    }

    override fun toString(): String {
        return "Rectangle(id=$id, position=$position, velocity=$velocity, acceleration=$acceleration, mass=$mass, static=$static, color=$color, width=$width, height=$height, rotation=$rotation)"
    }
}
