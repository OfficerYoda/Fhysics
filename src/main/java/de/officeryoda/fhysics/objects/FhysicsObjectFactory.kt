package de.officeryoda.fhysics.objects

import de.officeryoda.fhysics.engine.FhysicsCore
import de.officeryoda.fhysics.engine.Vector2
import java.awt.geom.Rectangle2D
import java.util.*

object FhysicsObjectFactory {
    private val RANDOM: Random = Random()

    fun randomCircle(): Circle {
        val radius: Double = RANDOM.nextDouble(0.2, 0.3)
        val pos: Vector2 = randomPosInsideBounds(buffer = radius)
        val circle = Circle(pos, radius)

        circle.velocity.set(randomVector2(-10.0, 10.0))

        return circle
    }

    fun customCircle(pos: Vector2, radius: Double, vel: Vector2): Circle {
        val circle = Circle(pos, radius)
        circle.velocity += vel
        return circle
    }

    fun randomBox(): Box {
        val width: Double = RANDOM.nextDouble(1.0, 20.0)
        val height: Double = RANDOM.nextDouble(1.0, 20.0)
        val pos: Vector2 = randomPosInsideBounds(0.0)
        val box = Box(pos, width, height)

//        box.velocity.set(randomVector2(-10.0, 10.0))

        return box
    }

    fun customBox(pos: Vector2, width: Double, height: Double, vel: Vector2): Box {
        val box = Box(pos, width, height)
        box.velocity += vel
        return box
    }

    private fun randomPosInsideBounds(buffer: Double): Vector2 {
        val border: Rectangle2D = FhysicsCore.BORDER
        val minX: Double = buffer
        val maxX: Double = border.width - minX - buffer
        val x: Double = RANDOM.nextDouble(minX, maxX)

        val minY: Double = buffer
        val maxY: Double = border.height - minY - buffer
        val y: Double = RANDOM.nextDouble(minY, maxY)

        return Vector2(x, y)
    }

    private fun randomVector2(min: Double, max: Double): Vector2 {
        val x: Double = RANDOM.nextDouble(min, max)
        val y: Double = RANDOM.nextDouble(min, max)

        return Vector2(x, y)
    }
}
