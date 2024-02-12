package de.officeryoda.fhysics.objects

import de.officeryoda.fhysics.engine.Border
import de.officeryoda.fhysics.engine.FhysicsCore
import de.officeryoda.fhysics.engine.Vector2
import java.util.*

object FhysicsObjectFactory {
    private val RANDOM: Random = Random()

    fun randomCircle(): Circle {
        val radius: Double = RANDOM.nextDouble(0.2, 0.3)
        val pos: Vector2 = randomPosInsideBounds(radius)
        val circle = Circle(pos, radius)

        circle.velocity.set(randomVector2(-200.0, 200.0))

        return circle
    }

    fun customCircle(pos: Vector2, radius: Double, vel: Vector2): Circle {
        val circle = Circle(pos, radius)
        circle.velocity += vel
        return circle
    }

    private fun randomPosInsideBounds(buffer: Double): Vector2 {
        val border: Border = FhysicsCore.BORDER
        val minX: Double = border.leftBorder + buffer
        val maxX: Double = border.rightBorder - minX - buffer
        val x: Double = RANDOM.nextDouble(minX, maxX)

        val minY: Double = border.bottomBorder + buffer
        val maxY: Double = border.topBorder - minY - buffer
        val y: Double = RANDOM.nextDouble(minY, maxY)

        return Vector2(x, y)
    }

    private fun randomVector2(min: Double, max: Double): Vector2 {
        val x: Double = RANDOM.nextDouble(min, max)
        val y: Double = RANDOM.nextDouble(min, max)

        return Vector2(x, y)
    }
}
