package de.officeryoda.fhysics.engine.datastructures

import de.officeryoda.fhysics.engine.math.Vector2
import de.officeryoda.fhysics.engine.objects.Circle
import de.officeryoda.fhysics.engine.objects.Polygon
import kotlin.math.max
import kotlin.math.min

/**
 * A BoundingBox is an axis-aligned rectangle that encloses an object.
 */
data class BoundingBox(
    /** Lower left corner x-coordinate */
    var x: Float = 0.0f,
    /** Lower left corner y-coordinate */
    var y: Float = 0.0f,
    /** Width of the bounding box */
    var width: Float = 0.0f,
    /** Height of the bounding box */
    var height: Float = 0.0f,
) {

    /**
     * Checks if this bounding box overlaps with [another][other] bounding box.
     */
    fun overlaps(other: BoundingBox): Boolean {
        return this.x <= (other.x + other.width) &&
                this.y <= (other.y + other.height) &&
                other.x <= (this.x + this.width) &&
                other.y <= (this.y + this.height)
    }

    /**
     * Checks if the given [position][pos] is contained within this bounding box.
     */
    fun contains(pos: Vector2): Boolean {
        return pos.x in x..(x + width) &&
                pos.y in y..(y + height)
    }

    /**
     * Checks if this bounding box contains the [given][other] bounding box.
     */
    fun contains(other: BoundingBox): Boolean {
        return this.x <= other.x &&
                this.y <= other.y &&
                (this.x + this.width) >= (other.x + other.width) &&
                (this.y + this.height) >= (other.y + other.height)
    }


    /**
     * Sets the bounding box to the bounding box of the [given][circle] circle.
     */
    fun setFromCircle(circle: Circle) {
        this.x = circle.position.x - circle.radius
        this.y = circle.position.y - circle.radius
        this.width = circle.radius * 2
        this.height = circle.radius * 2
    }

    /**
     * Sets the bounding box to the bounding box of the [given][poly] polygon.
     */
    fun setFromPolygon(poly: Polygon) {
        val transformedVertices: Array<Vector2> = poly.getTransformedVertices()

        var minX: Float = Float.MAX_VALUE
        var maxX: Float = Float.MIN_VALUE
        var minY: Float = Float.MAX_VALUE
        var maxY: Float = Float.MIN_VALUE

        // Find the minimum and maximum x and y values
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