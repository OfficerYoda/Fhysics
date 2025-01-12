package de.officeryoda.fhysics.engine.objects.factories

import de.officeryoda.fhysics.engine.FhysicsCore
import de.officeryoda.fhysics.engine.datastructures.BoundingBox
import de.officeryoda.fhysics.engine.math.Vector2
import de.officeryoda.fhysics.engine.objects.Circle
import de.officeryoda.fhysics.engine.objects.Polygon
import de.officeryoda.fhysics.engine.objects.Rectangle
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.math.*
import kotlin.random.Random.Default as KtRandom

@Suppress("SameParameterValue", "SameParameterValue", "SameParameterValue")
/**
 * A factory for creating random [FhysicsObjects][de.officeryoda.fhysics.engine.objects.FhysicsObject].
 */
object FhysicsObjectFactory {

    private val RANDOM: Random = Random()

    // Executor for creating polygons
    // Cached to not hold unused threads
    private val executor = Executors.newCachedThreadPool()

    /**
     * Returns a random circle with a random position, radius, and velocity.
     */
    fun randomCircle(): Circle {
        val radius: Float = RANDOM.nextFloat(0.2f, 0.4f)
        val pos: Vector2 = randomPosInsideBounds(buffer = radius)
        val circle = Circle(pos, radius)

        circle.velocity.set(randomVector2(-10.0f, 10.0f))

        return circle
    }

    /**
     * Returns a random rectangle with a random position, width, height, rotation, and velocity.
     */
    fun randomRectangle(): Rectangle {
        val width: Float = RANDOM.nextFloat(1.0f, 10.0f)
        val height: Float = RANDOM.nextFloat(1.0f, 10.0f)
        val pos: Vector2 = randomPosInsideBounds(0.0f)
        val rot: Float = RANDOM.nextFloat(2 * PI.toFloat())

        val rect = Rectangle(pos, width, height, rot)
        rect.velocity += randomVector2(-10.0f, 10.0f)

        return rect
    }

    /**
     * Returns a random polygon with a random position and velocity.
     *
     * This only works when called from the main thread.
     */
    fun randomPolygon(): Polygon {
        val center: Vector2 = randomPosInsideBounds(0.0f)
        val avgRadius: Float = RANDOM.nextFloat(1.0f, 5.0f)
        val irregularity: Float = RANDOM.nextFloat(0.0f, 1.0f)
        val spikiness: Float = RANDOM.nextFloat(0.0f, 1.0f)
        val numVertices: Int = RANDOM.nextInt(3, 10)

        val points: Array<Vector2> =
            generatePolygon(center, avgRadius, irregularity, spikiness, numVertices).toTypedArray()

        val polygon: Polygon = try {
            val future: CompletableFuture<Polygon> = CompletableFuture.supplyAsync({
                PolygonFactory.createPolygon(points)
            }, executor)

            future.get(50, TimeUnit.MILLISECONDS)
        } catch (_: TimeoutException) {
            println("Polygon creation timed out, retrying...")
            return randomPolygon()
        }

        // Check if any of the Vertices are the same
        for (i: Int in 0 until polygon.vertices.size) {
            for (j: Int in i + 1 until polygon.vertices.size) {
                if (polygon.vertices[i] == polygon.vertices[j]) {
                    println("Polygon vertices are the same, retrying...")
                    return randomPolygon()
                }
            }
        }

        polygon.position.set(randomPosInsideBounds(avgRadius + avgRadius * spikiness))
        polygon.velocity += randomVector2(-10.0f, 10.0f)

        return polygon
    }

    /**
     * Generates a polygon by sampling points on a circle around a center.
     * Random noise is added by varying the angular spacing between sequential points,
     * and by varying the radial distance of each point from the center.
     *
     * @param center The center of the circumference used to generate the polygon.
     * @param avgRadius The average radius (distance of each generated vertex to the center of the circumference).
     * @param irregularity Variance of the spacing of the angles between consecutive vertices.
     * @param spikiness Variance of the distance of each vertex to the center of the circumference.
     * @param numVertices The number of vertices of the polygon.
     * @return A list of vertices in counter-clockwise (CCW) order.
     * @throws IllegalArgumentException If the irregularity or spikiness values are outside the valid range [0, 1].
     *
     * @author <a href="https://stackoverflow.com/questions/8997099/algorithm-to-generate-random-2d-polygon">StackOverflow</a>
     */
    private fun generatePolygon(
        center: Vector2,
        avgRadius: Float,
        irregularity: Float,
        spikiness: Float,
        numVertices: Int,
    ): List<Vector2> {
        // Parameter check
        if (irregularity < 0 || irregularity > 1) {
            throw IllegalArgumentException("Irregularity must be between 0 and 1.")
        }
        if (spikiness < 0 || spikiness > 1) {
            throw IllegalArgumentException("Spikiness must be between 0 and 1.")
        }

        val adjustedIrregularity: Double = irregularity * 2 * Math.PI / numVertices
        val adjustedSpikiness: Float = spikiness * avgRadius
        val angleSteps: List<Double> = randomAngleSteps(numVertices, adjustedIrregularity)

        val points: MutableList<Vector2> = mutableListOf()
        var angle: Double = KtRandom.nextDouble(0.0, 2 * Math.PI)

        for (i: Int in 0 until numVertices) {
            val radius: Float = clip(nextGaussian(avgRadius, adjustedSpikiness), 0f, 2 * avgRadius)
            val point = Vector2(
                center.x + radius * cos(angle).toFloat(),
                center.y + radius * sin(angle).toFloat()
            )
            points.add(point)
            angle += angleSteps[i]
        }

        return points
    }

    /**
     * Generates the division of a circumference into random angles.
     *
     * @param steps The number of angles to generate.
     * @param irregularity Variance of the spacing of the angles between consecutive vertices.
     * @return A list of random angle steps.
     */
    private fun randomAngleSteps(steps: Int, irregularity: Double): List<Double> {
        val angles: MutableList<Double> = mutableListOf()
        val lower: Double = (2 * Math.PI / steps) - irregularity
        val upper: Double = (2 * Math.PI / steps) + irregularity
        var angleSum = 0.0

        repeat(steps) {
            val angle: Double = KtRandom.nextDouble(lower, upper)
            angles.add(angle)
            angleSum += angle
        }

        // Normalize the steps so that point 0 and point n+1 are the same
        angleSum /= (2 * Math.PI).toFloat()
        for (i: Int in angles.indices) {
            angles[i] /= angleSum
        }
        return angles
    }

    /**
     * Clips a value to be within the given lower and upper bounds.
     *
     * @param value The value to be clipped.
     * @param lower The lower bound of the interval.
     * @param upper The upper bound of the interval.
     * @return The clipped value, within the range [lower, upper].
     */
    @Suppress("SameParameterValue", "SameParameterValue", "SameParameterValue")
    private fun clip(value: Float, lower: Float, upper: Float): Float {
        return value.coerceIn(lower, upper)
    }

    /**
     * Generates a Gaussian-distributed value with a given mean and standard deviation.
     * Uses the Box-Muller transform to generate normally distributed random numbers.
     *
     * @param mean The mean (center) of the distribution.
     * @param stdDev The standard deviation (spread) of the distribution.
     * @return A Gaussian-distributed value.
     */
    private fun nextGaussian(mean: Float, stdDev: Float): Float {
        val u1: Double = KtRandom.nextDouble(0.0, 1.0)
        val u2: Double = KtRandom.nextDouble(0.0, 1.0)
        val z0: Double = sqrt(-2.0 * ln(u1)) * cos(2.0 * Math.PI * u2)
        return (mean + z0 * stdDev).toFloat()
    }

    /**
     * Returns a position inside the border with a [buffer] to the edges
     * to prevent objects from spawning outside the border.
     */
    private fun randomPosInsideBounds(buffer: Float): Vector2 {
        val border: BoundingBox = FhysicsCore.border
        val minX: Float = buffer
        val maxX: Float = border.width - buffer
        val x: Float = RANDOM.nextFloat(minX, maxX)

        val minY: Float = buffer
        val maxY: Float = border.height - buffer
        val y: Float = RANDOM.nextFloat(minY, maxY)

        return Vector2(x, y)
    }

    /**
     * Generates a random 2D vector with components in the range [min, max].
     *
     * @param min The minimum value for the components.
     * @param max The maximum value for the components.
     * @return A random 2D vector.
     */
    private fun randomVector2(min: Float, max: Float): Vector2 {
        val x: Float = RANDOM.nextFloat(min, max)
        val y: Float = RANDOM.nextFloat(min, max)

        return Vector2(x, y)
    }
}
