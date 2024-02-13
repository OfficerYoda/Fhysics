package de.officeryoda.fhysics.engine

import kotlin.math.sqrt

/**
 * A 2D vector class providing basic vector operations.
 *
 * @property x The x-coordinate of the Vector2.
 * @property y The y-coordinate of the Vector2.
 */
class Vector2
@JvmOverloads constructor(
    var x: Double = 0.0, var y: Double = 0.0
) {
    /**
     * Normalizes the Vector2 to have a magnitude of 1.0.
     *
     * @return The normalized Vector2.
     */
    fun normalized(): Vector2 {
        val magnitude: Double = magnitude()
        if (magnitude != 0.0) {
            this.x /= magnitude
            this.y /= magnitude
        }
        return this
    }

    /**
     * Calculates the dot product of this Vector2 with another Vector2.
     *
     * @param other The other Vector2.
     * @return The dot product of the two Vector2 instances.
     */
    fun dot(other: Vector2): Double {
        return this.x * other.x + this.y * other.y
    }

    /**
     * Calculates the magnitude (length) of the Vector2.
     *
     * @return The magnitude of the Vector2.
     */
    fun magnitude(): Double {
        return sqrt(sqrMagnitude())
    }

    /**
     * Calculates the squared magnitude (length) of the Vector2.
     *
     * @return The squared magnitude of the Vector2.
     */
    fun sqrMagnitude(): Double {
        return x * x + y * y
    }

    /**
     * Calculates the Euclidean distance between this Vector2 and another Vector2.
     *
     * @param other The other Vector2.
     * @return The distance between the two Vector2 instances.
     */
    fun distance(other: Vector2): Double {
        return sqrt(sqrDistance(other))
    }

    /**
     * Calculates the squared Euclidean distance between this Vector2 and another Vector2.
     *
     * @param other The other Vector2.
     * @return The squared distance between the two Vector2 instances.
     */
    fun sqrDistance(other: Vector2): Double {
        val dx: Double = this.x - other.x
        val dy: Double = this.y - other.y
        return dx * dx + dy * dy
    }

    /**
     * Sets the components of this Vector2 to be equal to another Vector2.
     *
     * @param other The Vector2 whose components will be copied.
     */
    fun set(other: Vector2) {
        this.x = other.x
        this.y = other.y
    }

    /**
     * Adds another Vector2 to this Vector2.
     *
     * @param other The Vector2 to add.
     * @return The result of the addition.
     */
    operator fun plus(other: Vector2): Vector2 {
        return Vector2(this.x + other.x, this.y + other.y)
    }

    /**
     * Subtracts another Vector2 from this Vector2.
     *
     * @param other The Vector2 to subtract.
     * @return The result of the subtraction.
     */
    operator fun minus(other: Vector2): Vector2 {
        return Vector2(this.x - other.x, this.y - other.y)
    }

    /**
     * Multiplies the Vector2 by a scalar.
     *
     * @param scalar The scalar value.
     * @return The result of the multiplication.
     */
    operator fun times(scalar: Double): Vector2 {
        return Vector2(this.x * scalar, this.y * scalar)
    }

    /**
     * Divides the Vector2 by a scalar.
     *
     * @param scalar The scalar value.
     * @return The result of the division.
     * @throws IllegalArgumentException if division by zero is attempted.
     */
    operator fun div(scalar: Double): Vector2 {
        return Vector2(this.x / scalar, this.y / scalar)
    }

    /**
     * Adds another Vector2 to this Vector2 in-place.
     *
     * @param other The Vector2 to add.
     */
    operator fun plusAssign(other: Vector2) {
        this.x += other.x
        this.y += other.y
    }

    /**
     * Subtracts another Vector2 from this Vector2 in-place.
     *
     * @param other The Vector2 to subtract.
     */
    operator fun minusAssign(other: Vector2) {
        this.x -= other.x
        this.y -= other.y
    }

    /**
     * Multiplies the Vector2 by a scalar in-place.
     *
     * @param scalar The scalar value.
     */
    operator fun timesAssign(scalar: Double) {
        this.x *= scalar
        this.y *= scalar
    }

    /**
     * Divides the Vector2 by a scalar in-place.
     *
     * @param scalar The scalar value.
     * @throws IllegalArgumentException if division by zero is attempted.
     */
    operator fun divAssign(scalar: Double) {
        this.x /= scalar
        this.y /= scalar
    }

    /**
     * Returns a new Vector2 instance that represents the negation of the current vector.
     *
     * @return The negated Vector2.
     */
    operator fun unaryMinus(): Vector2 {
        return Vector2(-this.x, -this.y)
    }

    /**
     * Converts this Vector2 to a Vector2Int by rounding the x and y coordinates to the nearest integer values.
     *
     * @return The converted Vector2Int.
     */
    fun toIntVector2(): Vector2Int {
        return Vector2Int(x.toInt(), y.toInt())
    }

    /**
     * Returns a string representation of the Vector2.
     *
     * @return A string containing the values of the x and y components.
     */
    override fun toString(): String {
        return "Vector2(x=$x, y=$y)"
    }

    /**
     * Companion object providing utility functions for Vector2.
     */
    companion object {
        /**
         * Creates a Vector2 with both components set to zero.
         *
         * @return The zero Vector2.
         */
        @JvmStatic
        val ZERO: Vector2
            get() = Vector2(0.0, 0.0)
    }
}
