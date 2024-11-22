package de.officeryoda.fhysics.engine.math

import de.officeryoda.fhysics.engine.objects.Circle
import de.officeryoda.fhysics.engine.objects.Polygon
import kotlin.math.max
import kotlin.math.min

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
        return this.x <= (other.x + other.width)
                && this.y <= (other.y + other.height)
                && other.x <= (this.x + this.width)
                && other.y <= (this.y + this.height)
    }

    /**
     * Checks if the given position is contained within this bounding box.
     *
     * @param pos The position to check for containment.
     * @return True if the position is contained within the bounding box, false otherwise.
     */
    fun contains(pos: Vector2): Boolean {
        return pos.x in x..(x + width) &&
                pos.y in y..(y + height)
    }

    /**
     * Checks if this bounding box contains the given bounding box.
     *
     * @param other The other bounding box to check for containment.
     * @return True if this bounding box contains the other bounding box, false otherwise.
     */
    fun contains(other: BoundingBox): Boolean {
        return this.x <= other.x
                && this.y <= other.y
                && (this.x + this.width) >= (other.x + other.width)
                && (this.y + this.height) >= (other.y + other.height)
    }


    /**
     * Sets the bounding box to the bounding box of the given circle.
     *
     * @param circle The circle to set the bounding box from.
     */
    fun setFromCircle(circle: Circle) {
        this.x = circle.position.x - circle.radius
        this.y = circle.position.y - circle.radius
        this.width = circle.radius * 2
        this.height = circle.radius * 2
    }

    /**
     * Sets the bounding box to the bounding box of the given polygon.
     *
     * @param poly The polygon to set the bounding box from.
     */
    fun setFromPolygon(poly: Polygon) {
        val transformedVertices: Array<Vector2> = poly.getTransformedVertices()

        var minX: Float = Float.MAX_VALUE
        var maxX: Float = Float.MIN_VALUE
        var minY: Float = Float.MAX_VALUE
        var maxY: Float = Float.MIN_VALUE

        for (vector: Vector2 in transformedVertices) {
            minX = min(minX, vector.x)
            maxX = max(maxX, vector.x)
            minY = min(minY, vector.y)
            maxY = max(maxY, vector.y)
        }

        this.x = minX
        this.y = minY
        this.width = maxX - minX
        this.height = maxY - minY
    }
}