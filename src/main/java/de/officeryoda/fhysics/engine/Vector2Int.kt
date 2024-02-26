package de.officeryoda.fhysics.engine

import kotlin.math.sqrt

/**
 * A 2D vector class providing basic vector operations.
 *
 * @property x The x-coordinate of the Vector2Int.
 * @property y The y-coordinate of the Vector2Int.
 */
data class Vector2Int
@JvmOverloads constructor(
    var x: Int = 0, var y: Int = 0
) {
    /**
     * Calculates the dot product of this Vector2Int with another Vector2Int.
     *
     * @param other The other Vector2Int.
     * @return The dot product of the two Vector2Int instances.
     */
    fun dot(other: Vector2Int): Int {
        return this.x * other.x + this.y * other.y
    }

    /**
     * Calculates the magnitude (length) of the Vector2Int.
     *
     * @return The magnitude of the Vector2Int.
     */
    fun magnitude(): Float {
        return sqrt(sqrMagnitude().toFloat())
    }

    /**
     * Calculates the squared magnitude (length) of the Vector2Int.
     *
     * @return The squared magnitude of the Vector2Int.
     */
    fun sqrMagnitude(): Int {
        return x * x + y * y
    }

    /**
     * Calculates the Euclidean distance between this Vector2Int and another Vector2Int.
     *
     * @param other The other Vector2Int.
     * @return The distance between the two Vector2Int instances.
     */
    fun distance(other: Vector2Int): Float {
        return sqrt(sqrDistance(other).toFloat())
    }

    /**
     * Calculates the squared Euclidean distance between this Vector2Int and another Vector2Int.
     *
     * @param other The other Vector2Int.
     * @return The squared distance between the two Vector2Int instances.
     */
    fun sqrDistance(other: Vector2Int): Int {
        val dx: Int = this.x - other.x
        val dy: Int = this.y - other.y
        return dx * dx + dy * dy
    }

    /**
     * Sets the components of this Vector2Int to be equal to another Vector2.
     *
     * @param other The Vector2Int whose components will be copied.
     */
    fun set(other: Vector2Int) {
        this.x = other.x
        this.y = other.y
    }

    /**
     * Adds another Vector2Int to this Vector2Int.
     *
     * @param other The Vector2Int to add.
     * @return The result of the addition.
     */
    operator fun plus(other: Vector2Int): Vector2Int {
        return Vector2Int(this.x + other.x, this.y + other.y)
    }

    /**
     * Subtracts another Vector2Int from this Vector2Int.
     *
     * @param other The Vector2Int to subtract.
     * @return The result of the subtraction.
     */
    operator fun minus(other: Vector2Int): Vector2Int {
        return Vector2Int(this.x - other.x, this.y - other.y)
    }

    /**
     * Multiplies the Vector2Int by a scalar.
     *
     * @param scalar The scalar value.
     * @return The result of the multiplication.
     */
    operator fun times(scalar: Float): Vector2Int {
        return Vector2Int((this.x * scalar).toInt(), (this.y * scalar).toInt())
    }

    /**
     * Divides the Vector2Int by a scalar.
     *
     * @param scalar The scalar value.
     * @return The result of the division.
     * @throws IllegalArgumentException if division by zero is attempted.
     */
    operator fun div(scalar: Float): Vector2Int {
        return Vector2Int((this.x / scalar).toInt(), (this.y / scalar).toInt())
    }

    /**
     * Adds another Vector2Int to this Vector2Int in-place.
     *
     * @param other The Vector2Int to add.
     */
    operator fun plusAssign(other: Vector2Int) {
        this.x += other.x
        this.y += other.y
    }

    /**
     * Subtracts another Vector2Int from this Vector2Int in-place.
     *
     * @param other The Vector2Int to subtract.
     */
    operator fun minusAssign(other: Vector2Int) {
        this.x -= other.x
        this.y -= other.y
    }

    /**
     * Multiplies the Vector2Int by a scalar in-place.
     *
     * @param scalar The scalar value.
     */
    operator fun timesAssign(scalar: Float) {
        this.x = this.x.times(scalar).toInt()
        this.y = this.y.times(scalar).toInt()
    }

    /**
     * Divides the Vector2Int by a scalar in-place.
     *
     * @param scalar The scalar value.
     * @throws IllegalArgumentException if division by zero is attempted.
     */
    operator fun divAssign(scalar: Float) {
        this.x = this.x.div(scalar).toInt()
        this.y = this.y.div(scalar).toInt()
    }

    /**
     * Returns a new Vector2Int instance that represents the negation of the current vector.
     *
     * @return The negated Vector2Int.
     */
    operator fun unaryMinus(): Vector2Int {
        return Vector2Int(-this.x, -this.y)
    }

    /**
     * Converts this Vector2Int to a Vector2 by converting the x and y coordinates to Float values.
     *
     * @return The converted Vector2.
     */
    fun toFloatVector2(): Vector2 {
        return Vector2(x.toFloat(), y.toFloat())
    }

    /**
     * Creates and returns a copy (clone) of the Vector2Int.
     *
     * @return A new Vector2Int object with the same x and y components.
     */
    fun clone(): Vector2Int {
        return Vector2Int(this.x, this.y)
    }

    /**
     * Returns a string representation of the Vector2Int.
     *
     * @return A string containing the values of the x and y components.
     */
    override fun toString(): String {
        return "Vector2Int(x=$x, y=$y)"
    }

    /**
     * Companion object providing utility functions for Vector2Int.
     */
    companion object {
        /**
         * Creates a Vector2Int with both components set to zero.
         *
         * @return The zero Vector2.
         */
        @JvmStatic
        val ZERO: Vector2Int
            get() = Vector2Int(0, 0)
    }
}
