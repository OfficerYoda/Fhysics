package de.officeryoda.fhysics.objects

import de.officeryoda.fhysics.engine.Vector2
import de.officeryoda.fhysics.engine.collision.CollisionFinder
import de.officeryoda.fhysics.engine.collision.CollisionInfo
import java.awt.Color
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

class Rectangle(
    position: Vector2,
    val width: Float,
    val height: Float,
    val rotation: Float = 0F,
) : FhysicsObject(position, width * height) {

    val minX: Float = min(position.x - rotatedOffsetX().toFloat(), position.x + rotatedOffsetX().toFloat())

    val maxX: Float = max(position.x - rotatedOffsetX().toFloat(), position.x + rotatedOffsetX().toFloat())

    val minY: Float = min(position.y - rotatedOffsetY().toFloat(), position.y + rotatedOffsetY().toFloat())

    val maxY: Float = max(position.y - rotatedOffsetY().toFloat(), position.y + rotatedOffsetY().toFloat())

    init {
        color = Color.decode("#4287f5")
        static = true // rectangles must be static for min/max values to work
    }

    override fun testCollision(other: Circle): CollisionInfo {
        return CollisionFinder.testCollision(other, this)
    }

    override fun testCollision(other: Rectangle): CollisionInfo {
        return CollisionFinder.testCollision(this, other)
    }

    private fun rotatedOffsetX(): Double {
        return (width / 2) * cos(rotation.toDouble()) - (height / 2) * sin(rotation.toDouble())
    }

    private fun rotatedOffsetY(): Double {
        return (height / 2) * sin(rotation.toDouble()) + (width / 2) * cos(rotation.toDouble())
    }

    override fun toString(): String {
        return "Box(id=$id, position=$position, velocity=$velocity, acceleration=$acceleration, mass=$mass, static=$static, color=$color, width=$width, height=$height)"
    }
}
