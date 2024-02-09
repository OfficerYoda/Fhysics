package de.officeryoda.fhysics.objects

import de.officeryoda.fhysics.engine.FhysicsCore
import de.officeryoda.fhysics.engine.Vector2
import java.util.*

object FhysicsObjectFactory {
    private val RANDOM = Random()

    fun randomCircle(): Circle {
        val radius = RANDOM.nextDouble(5.0, 35.0)
        val pos = randomPosInsideBounds(radius)
        val circle = Circle(pos, radius)

        circle.velocity.set(randomVector2(-200.0, 200.0))

        return circle
    }

    private fun randomPosInsideBounds(buffer: Double): Vector2 {
        val border = FhysicsCore.BORDER
        val minX = border.leftBorder + buffer
        val maxX = border.rightBorder - minX - buffer
        val x = RANDOM.nextDouble(minX, maxX)

        val minY = border.bottomBorder + buffer
        val maxY = border.topBorder - minY - buffer
        val y = RANDOM.nextDouble(minY, maxY)

        return Vector2(x, y)
    }

    private fun randomVector2(min: Double, max: Double): Vector2 {
        val x = RANDOM.nextDouble(min, max)
        val y = RANDOM.nextDouble(min, max)

        return Vector2(x, y)
    }
}
