package de.officeryoda.fhysics.objects

import de.officeryoda.fhysics.engine.Vector2
import de.officeryoda.fhysics.engine.collision.CollisionFinder
import de.officeryoda.fhysics.engine.collision.CollisionInfo
import java.awt.Color
import kotlin.math.cos
import kotlin.math.sin

class Rectangle(
    position: Vector2,
    val width: Float,
    val height: Float,
    val rotation: Float = 0F,
) : FhysicsObject(position, width * height) {

    val minX: Float
        get() {
            val offsetX: Float = width / 2
            val rotatedOffsetX: Double = offsetX * cos(rotation.toDouble()) - (height / 2) * sin(rotation.toDouble())
            return position.x - rotatedOffsetX.toFloat()
        }

    val maxX: Float
        get() {
            val offsetX: Float = width / 2
            val rotatedOffsetX: Double = offsetX * cos(rotation.toDouble()) - (height / 2) * sin(rotation.toDouble())
            return position.x + rotatedOffsetX.toFloat()
        }

    val minY: Float
        get()  {
            val offsetY: Float = height / 2
            val rotatedOffsetY: Double = offsetY * sin(rotation.toDouble()) + (width / 2) * cos(rotation.toDouble())
            return position.y - rotatedOffsetY.toFloat()
        }

    val maxY: Float
        get()  {
            val offsetY: Float = height / 2
            val rotatedOffsetY: Double = offsetY * sin(rotation.toDouble()) + (width / 2) * cos(rotation.toDouble())
            return position.y + rotatedOffsetY.toFloat()
        }

    init {
        color = Color.decode("#4287f5")
        static = true
    }

    override fun testCollision(other: Circle): CollisionInfo {
        return CollisionFinder.testCollision(other, this)
    }

    override fun testCollision(other: Rectangle): CollisionInfo {
        return CollisionFinder.testCollision(this, other)
    }

    override fun toString(): String {
        return "Box(id=$id, position=$position, velocity=$velocity, acceleration=$acceleration, mass=$mass, static=$static, color=$color, width=$width, height=$height)"
    }
}
