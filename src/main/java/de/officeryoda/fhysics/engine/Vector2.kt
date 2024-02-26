package de.officeryoda.fhysics.engine

import kotlin.math.sqrt

/**
 * A 2D vector class providing basic vector operations.
 *
 * @property x The x-coordinate of the Vector2.
 * @property y The y-coordinate of the Vector2.
 */
data class Vector2
@JvmOverloads constructor(
    var x: Float = 0.0F, var y: Float = 0.0F
) {
    /**
     * Returns a normalized version of the  Vector2.
     *
     * @return The normalized Vector2.
     */
    fun normalized(): Vector2 {
        val magnitude: Float = magnitude()
        return if (magnitude != 0.0F) {
            Vector2(this.x / magnitude, this.y / magnitude)
        } else {
            Vector2(0.0F, 0.0F)  // Handle division by zero, return a default zero vector.
        }
    }


    /**
     * Calculates the dot product of this Vector2 with another Vector2.
     *
     * @param other The other Vector2.
     * @return The dot product of the two Vector2 instances.
     */
    fun dot(other: Vector2): Float {
        return this.x * other.x + this.y * other.y
    }

    /**
     * Calculates the magnitude (length) of the Vector2.
     *
     * @return The magnitude of the Vector2.
     */
    fun magnitude(): Float {
        return sqrt(sqrMagnitude())
    }

    /**
     * Calculates the squared magnitude (length) of the Vector2.
     *
     * @return The squared magnitude of the Vector2.
     */
    fun sqrMagnitude(): Float {
        return x * x + y * y
    }

    /**
     * Calculates the Euclidean distance between this Vector2 and another Vector2.
     *
     * @param other The other Vector2.
     * @return The distance between the two Vector2 instances.
     */
    fun distance(other: Vector2): Float {
        return sqrt(sqrDistance(other))
    }

    /**
     * Calculates the squared Euclidean distance between this Vector2 and another Vector2.
     *
     * @param other The other Vector2.
     * @return The squared distance between the two Vector2 instances.
     */
    fun sqrDistance(other: Vector2): Float {
        val dx: Float = this.x - other.x
        val dy: Float = this.y - other.y
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
    operator fun times(scalar: Float): Vector2 {
        return Vector2(this.x * scalar, this.y * scalar)
    }

    /**
     * Divides the Vector2 by a scalar.
     *
     * @param scalar The scalar value.
     * @return The result of the division.
     * @throws IllegalArgumentException if division by zero is attempted.
     */
    operator fun div(scalar: Float): Vector2 {
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
    operator fun timesAssign(scalar: Float) {
        this.x *= scalar
        this.y *= scalar
    }

    /**
     * Divides the Vector2 by a scalar in-place.
     *
     * @param scalar The scalar value.
     * @throws IllegalArgumentException if division by zero is attempted.
     */
    operator fun divAssign(scalar: Float) {
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
            get() = Vector2(0.0F, 0.0F)
    }
}
