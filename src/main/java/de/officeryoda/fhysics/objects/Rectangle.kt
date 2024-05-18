package de.officeryoda.fhysics.objects

import de.officeryoda.fhysics.engine.Vector2
import de.officeryoda.fhysics.engine.collision.CollisionFinder
import de.officeryoda.fhysics.engine.collision.CollisionInfo
import java.awt.Color
import java.lang.Math.toRadians
import kotlin.math.*

class Rectangle(
    position: Vector2,
    val width: Float,
    val height: Float,
    val rotation: Float = 0F, // in Degrees
) : FhysicsObject(position, width * height) {

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
        val rot: Double = toRadians(rotation.toDouble())
        val cosRot: Double = cos(rot)
        val sinRot: Double = sin(rot)
        val halfWidth: Double = width / 2.0
        val halfHeight: Double = height / 2.0

        // idk how this works, but it does
        val offsetX: Double = abs(halfWidth * cosRot) + abs(halfHeight * sinRot)
        val offsetY: Double = abs(halfWidth * sinRot) + abs(halfHeight * cosRot)

        return Vector2(offsetX.toFloat(), offsetY.toFloat())
    }

    override fun toString(): String {
        return "Box(id=$id, position=$position, velocity=$velocity, acceleration=$acceleration, mass=$mass, static=$static, color=$color, width=$width, height=$height)"
    }
}
