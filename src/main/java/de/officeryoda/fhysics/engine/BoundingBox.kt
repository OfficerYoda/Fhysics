package de.officeryoda.fhysics.engine

import de.officeryoda.fhysics.engine.objects.Circle
import de.officeryoda.fhysics.engine.objects.FhysicsObject
import de.officeryoda.fhysics.engine.objects.Polygon
import de.officeryoda.fhysics.engine.objects.Rectangle
import kotlin.math.cos
import kotlin.math.sin

data class BoundingBox(
    var x: Float = 0.0f,
    var y: Float = 0.0f,
    var width: Float = 0.0f,
    var height: Float = 0.0f,
) {

    /**
     * Checks if this bounding box overlaps with the given bounding box.
     *
     * @param other The other bounding box to check for overlap.
     * @return True if the bounding boxes overlap, false otherwise.
     */
    fun overlaps(other: BoundingBox): Boolean {
        return (x <= (other.x + other.width))
                && ((x + width) >= other.x)
                && (y <= (other.y + other.height))
                && ((y + height) >= other.y)
    }

    /**
     * Sets the bounding box to the bounding box of the given circle.
     *
     * @param circle The circle to set the bounding box from.
     */
    private fun setFromCircle(circle: Circle) {
        this.x = circle.position.x - circle.radius
        this.y = circle.position.y - circle.radius
        this.width = circle.radius * 2
        this.height = circle.radius * 2
    }

    /**
     * Sets the bounding box to the bounding box of the given rectangle.
     *
     * @param rect The rectangle to set the bounding box from.
     */
    private fun setFromRectangle(rect: Rectangle) {
        val cos: Float = cos(rect.rotation)
        val sin: Float = sin(rect.rotation)

        val halfWidth: Float = rect.width / 2
        val halfHeight: Float = rect.height / 2

        val corners: Array<Vector2> = arrayOf(
            Vector2(-halfWidth, -halfHeight),
            Vector2(halfWidth, -halfHeight),
            Vector2(halfWidth, halfHeight),
            Vector2(-halfWidth, halfHeight)
        )

        val rotatedCorners: List<Vector2> = corners.map { corner ->
            Vector2(
                rect.position.x + corner.x * cos - corner.y * sin,
                rect.position.y + corner.x * sin + corner.y * cos
            )
        }

        val minX: Float = rotatedCorners.minOf { it.x }
        val maxX: Float = rotatedCorners.maxOf { it.x }
        val minY: Float = rotatedCorners.minOf { it.y }
        val maxY: Float = rotatedCorners.maxOf { it.y }

        this.x = minX
        this.y = minY
        this.width = maxX - minX
        this.height = maxY - minY
    }

    /**
     * Sets the bounding box to the bounding box of the given polygon.
     *
     * @param poly The polygon to set the bounding box from.
     */
    private fun setFromPolygon(poly: Polygon) {
        val translatedVertices: List<Vector2> = poly.getTranslatedVertices()

        val minX: Float = translatedVertices.minOf { it.x }
        val maxX: Float = translatedVertices.maxOf { it.x }
        val minY: Float = translatedVertices.minOf { it.y }
        val maxY: Float = translatedVertices.maxOf { it.y }

        this.x = minX
        this.y = minY
        this.width = maxX - minX
        this.height = maxY - minY
    }

    /**
     * Sets the bounding box to the bounding box of the given physics object.
     *
     * @param obj The physics object to set the bounding box from.
     * @throws IllegalArgumentException If the object type is not supported.
     */
    fun setFromFhysicsObject(obj: FhysicsObject) {
        return when (obj) {
            is Circle -> setFromCircle(obj)
            is Rectangle -> setFromRectangle(obj)
            is Polygon -> setFromPolygon(obj)
            else -> throw IllegalArgumentException("Unsupported object type for bounding box")
        }
    }

    /**
     * Checks if the given position is contained within this bounding box.
     *
     * @param pos The position to check for containment.
     * @return True if the position is contained within the bounding box, false otherwise.
     */
    fun contains(pos: Vector2): Boolean {
        return pos.x in x..(x + width) &&
                pos.y in pos.y..(y + height)
    }

    /**
     * Checks if this bounding box contains the given bounding box.
     *
     * @param other The other bounding box to check for containment.
     * @return True if this bounding box contains the other bounding box, false otherwise.
     */
    fun contains(other: BoundingBox): Boolean {
        return this.x <= other.x && this.y <= other.y &&
                (this.x + this.width) >= (other.x + other.width) &&
                (this.y + this.height) >= (other.y + other.height)
    }
}