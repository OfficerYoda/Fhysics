package de.officeryoda.fhysics.engine.objects

import de.officeryoda.fhysics.engine.BoundingBox
import de.officeryoda.fhysics.engine.FhysicsCore
import de.officeryoda.fhysics.engine.Vector2
import java.util.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object FhysicsObjectFactory {
    private val RANDOM: Random = Random()

    fun randomCircle(): Circle {
        val radius: Float = RANDOM.nextFloat(0.5F, 0.7F)
        val pos: Vector2 = randomPosInsideBounds(buffer = radius)
        val circle = Circle(pos, radius)

        circle.velocity.set(randomVector2(-10.0F, 10.0F))

        return circle
    }

    fun randomRectangle(): Rectangle {
        val width: Float = RANDOM.nextFloat(1.0F, 10.0F)
        val height: Float = RANDOM.nextFloat(1.0F, 10.0F)
        val pos: Vector2 = randomPosInsideBounds(0.0F)
        val rot: Float = RANDOM.nextFloat(2 * PI.toFloat())

        val rect = Rectangle(pos, width, height, rot)
        rect.velocity += randomVector2(-10.0F, 10.0F)

        return rect
    }

    fun randomPolygon(): Polygon {
        var vertices: MutableList<Vector2>
        val numVertices: Int = RANDOM.nextInt(4, 6)

        do {
            vertices = mutableListOf()

            // Generate random angles
            val angles: List<Float> = List(numVertices) { RANDOM.nextFloat(2 * PI.toFloat()) }
            // Sort the angles in ascending order
            val sortedAngles: List<Float> = angles.sorted()

            // Generate vertices using the sorted angles
            for (angle: Float in sortedAngles) {
                val x: Float = cos(angle) * RANDOM.nextFloat(-10f, 10f)
                val y: Float = sin(angle) * RANDOM.nextFloat(-10f, 10f)
                vertices.add(Vector2(x, y))
            }
            // Check if the generated polygon is concave
            val isValid: Boolean = PolygonCreator.validatePolyVertices(vertices)
        } while (!isValid) // Repeat until a valid polygon is generated

        val pos: Vector2 = randomPosInsideBounds(5F)
        val poly: Polygon = PolygonCreator.createPolygon(vertices.map { it + pos }.toTypedArray())
        poly.velocity.set(randomVector2(-10.0F, 10.0F))

        return poly
    }

    private fun randomPosInsideBounds(buffer: Float): Vector2 {
        val border: BoundingBox = FhysicsCore.BORDER
        val minX: Float = buffer
        val maxX: Float = border.width - minX - buffer
        val x: Float = RANDOM.nextFloat(minX, maxX)

        val minY: Float = buffer
        val maxY: Float = border.height - minY - buffer
        val y: Float = RANDOM.nextFloat(minY, maxY)

        return Vector2(x, y)
    }

    private fun randomVector2(min: Float, max: Float): Vector2 {
        val x: Float = RANDOM.nextFloat(min, max)
        val y: Float = RANDOM.nextFloat(min, max)

        return Vector2(x, y)
    }
}
