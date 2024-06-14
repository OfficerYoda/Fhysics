package de.officeryoda.fhysics.engine

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * A 2D vector class providing basic vector operations.
 *
 * @property x The x-coordinate of the Vector2.
 * @property y The y-coordinate of the Vector2.
 */
data class Vector2
@JvmOverloads constructor(
    var x: Float = 0.0f, var y: Float = 0.0f,
) {
    /**
     * Constructs a Vector2 with the same components as another Vector2.
     *
     * @param other The other Vector2.
     */
    constructor(other: Vector2) : this(other.x, other.y)

    /**
     * Returns a normalized version of the  Vector2.
     *
     * @return The normalized Vector2.
     */
    fun normalized(): Vector2 {
        val magnitude: Float = magnitude()
        return if (magnitude != 0.0f) {
            Vector2(this.x / magnitude, this.y / magnitude)
        } else {
            Vector2(0.0f, 0.0f)  // Handle division by zero, return a default zero vector.
        }
    }

    /**
     * Returns a rotated version of the Vector2 around the given center point.
     *
     * @param center The center of rotation.
     * @param angle The angle of rotation in radians.
     *
     * @return The rotated Vector2.
     */
    fun rotatedAround(center: Vector2, angle: Float): Vector2 {
        val cosAngle: Float = cos(angle)
        val sinAngle: Float = sin(angle)

        val translatedX: Float = this.x - center.x
        val translatedY: Float = this.y - center.y

        val rotatedX: Float = translatedX * cosAngle - translatedY * sinAngle
        val rotatedY: Float = translatedX * sinAngle + translatedY * cosAngle

        return Vector2(rotatedX + center.x, rotatedY + center.y)
    }

    /**
     * Rotates the Vector2 around the origin by the given angle.
     *
     * @param angle The angle of rotation in radians.
     * @return The rotated Vector2.
     */
    fun rotated(angle: Float): Vector2 {
        val cosAngle: Float = cos(angle)
        val sinAngle: Float = sin(angle)

        val rotatedX: Float = this.x * cosAngle - this.y * sinAngle
        val rotatedY: Float = this.x * sinAngle + this.y * cosAngle

        return Vector2(rotatedX, rotatedY)
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
    fun distanceTo(other: Vector2): Float {
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
     * Sets the components of the Vector2.
     *
     * @param x The new x-coordinate.
     * @param y The new y-coordinate.
     */
    fun set(x: Float, y: Float) {
        this.x = x
        this.y = y
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
     * Negates the Vector2.
     */
    fun negate() {
        this.x = -this.x
        this.y = -this.y
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
     * Compares this Vector2 to another object for equality.
     *
     * @param other The other object.
     * @return `true` if the objects are equal, `false` otherwise.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Vector2) return false

        if (x != other.x) return false
        if (y != other.y) return false

        return true
    }

    /**
     * Returns a hash code value for the Vector2.
     *
     * @return A hash code value.
     */
    override fun hashCode(): Int {
        var result = x.hashCode()
        result = 31 * result + y.hashCode()
        return result
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
            get() = Vector2(0.0f, 0.0f)
    }
}
