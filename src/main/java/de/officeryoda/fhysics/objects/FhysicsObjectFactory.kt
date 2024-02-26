package de.officeryoda.fhysics.objects

import de.officeryoda.fhysics.engine.FhysicsCore
import de.officeryoda.fhysics.engine.Vector2
import java.awt.geom.Rectangle2D
import java.util.*

object FhysicsObjectFactory {
    private val RANDOM: Random = Random()

    fun randomCircle(): Circle {
        val radius: Float = RANDOM.nextFloat(0.2F, 0.3F)
        val pos: Vector2 = randomPosInsideBounds(buffer = radius)
        val circle = Circle(pos, radius)

        circle.velocity.set(randomVector2(-10.0F, 10.0F))

        return circle
    }

    fun customCircle(pos: Vector2, radius: Float, vel: Vector2): Circle {
        val circle = Circle(pos, radius)
        circle.velocity += vel
        return circle
    }

    fun randomBox(): Box {
        val width: Float = RANDOM.nextFloat(1.0F, 20.0F)
        val height: Float = RANDOM.nextFloat(1.0F, 20.0F)
        val pos: Vector2 = randomPosInsideBounds(0.0F)
        val box = Box(pos, width, height)

//        box.velocity.set(randomVector2(-10.0, 10.0))

        return box
    }

    fun customBox(pos: Vector2, width: Float, height: Float, vel: Vector2): Box {
        val box = Box(pos, width, height)
        box.velocity += vel
        return box
    }

    private fun randomPosInsideBounds(buffer: Float): Vector2 {
        val border: Rectangle2D = FhysicsCore.BORDER
        val minX: Float = buffer
        val maxX: Float = border.width.toFloat() - minX - buffer
        val x: Float = RANDOM.nextFloat(minX, maxX)

        val minY: Float = buffer
        val maxY: Float = border.height.toFloat() - minY - buffer
        val y: Float = RANDOM.nextFloat(minY, maxY)

        return Vector2(x, y)
    }

    private fun randomVector2(min: Float, max: Float): Vector2 {
        val x: Float = RANDOM.nextFloat(min, max)
        val y: Float = RANDOM.nextFloat(min, max)

        return Vector2(x, y)
    }
}
