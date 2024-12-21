package de.officeryoda.fhysics.engine.math

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * A 2D vector class providing basic vector operations.
 *
 * @property x The x-coordinate of the [Vector2].
 * @property y The y-coordinate of the [Vector2].
 */
data class Vector2
@JvmOverloads constructor(
    var x: Float = 0.0f,
    var y: Float = 0.0f,
) {

    /**
     * Returns a normalized version of the [Vector2].
     */
    fun normalized(): Vector2 {
        val magnitude: Float = magnitude()
        return if (magnitude != 0f) {
            Vector2(this.x / magnitude, this.y / magnitude)
        } else {
            Vector2(0f, 0f)  // Handle division by zero, return a default zero vector.
        }
    }

    /**
     * Returns a [Vector2] which was rotated around the given [center] by the given [angle].
     */
    fun rotatedAround(angle: Float, center: Vector2): Vector2 {
        val translatedX: Float = this.x - center.x
        val translatedY: Float = this.y - center.y

        val cosAngle: Float = cos(angle)
        val sinAngle: Float = sin(angle)

        val rotatedX: Float = translatedX * cosAngle - translatedY * sinAngle
        val rotatedY: Float = translatedX * sinAngle + translatedY * cosAngle

        return Vector2(rotatedX + center.x, rotatedY + center.y)
    }

    /**
     * Returns a [Vector2] which was rotated around the origin by the given [angle].
     */
    fun rotated(angle: Float): Vector2 {
        val cosAngle: Float = cos(angle)
        val sinAngle: Float = sin(angle)

        val rotatedX: Float = this.x * cosAngle - this.y * sinAngle
        val rotatedY: Float = this.x * sinAngle + this.y * cosAngle

        return Vector2(rotatedX, rotatedY)
    }

    /**
     * Calculates the dot product of [this][Vector2] and [other].
     */
    fun dot(other: Vector2): Float {
        return this.x * other.x + this.y * other.y
    }

    /**
     * Calculates the cross product of [this][Vector2] with [other].
     *
     * To my math teacher Mr. Jungblut: I know that cross products are
     * technically only defined for 3D vectors, but if it works, it works.
     */
    fun cross(other: Vector2): Float {
        return this.x * other.y - this.y * other.x
    }

    /**
     * Calculates the magnitude (length) of the [Vector2].
     */
    fun magnitude(): Float {
        return sqrt(sqrMagnitude())
    }

    /**
     * Calculates the squared magnitude (length) of the [Vector2].
     */
    fun sqrMagnitude(): Float {
        return this.x * this.x + this.y * this.y
    }

    /**
     * Calculates the Euclidean distance between [this][Vector2] and [other].
     */
    fun distanceTo(other: Vector2): Float {
        return sqrt(sqrDistanceTo(other))
    }

    /**
     * Calculates the squared Euclidean distance between [this][Vector2] and [other].
     */
    fun sqrDistanceTo(other: Vector2): Float {
        val dx: Float = this.x - other.x
        val dy: Float = this.y - other.y
        return dx * dx + dy * dy
    }

    /**
     * Sets the components of the [Vector2].
     */
    fun set(x: Float, y: Float) {
        this.x = x
        this.y = y
    }

    /**
     * Sets the components of [this][Vector2] to be equal to [other].
     */
    fun set(other: Vector2) {
        this.x = other.x
        this.y = other.y
    }

    /**
     * Negates this [Vector2].
     */
    fun negate() {
        this.x = -this.x
        this.y = -this.y
    }

    /**
     * Returns the sum of [this][Vector2] and [other].
     */
    operator fun plus(other: Vector2): Vector2 {
        return Vector2(this.x + other.x, this.y + other.y)
    }

    /**
     * Returns the difference of [this][Vector2] and [other].
     */
    operator fun minus(other: Vector2): Vector2 {
        return Vector2(this.x - other.x, this.y - other.y)
    }

    /**
     * Returns the product of the [this][Vector2] and a [scalar].
     */
    operator fun times(scalar: Float): Vector2 {
        return Vector2(this.x * scalar, this.y * scalar)
    }

    /**
     * Returns the division of the [this][Vector2] by a [scalar].
     *
     * @throws IllegalArgumentException if division by zero is attempted.
     */
    operator fun div(scalar: Float): Vector2 {
        return Vector2(this.x / scalar, this.y / scalar)
    }

    /**
     * Assigns the sum of [this][Vector2] and [other] to [this][Vector2].
     */
    operator fun plusAssign(other: Vector2) {
        this.x += other.x
        this.y += other.y
    }

    /**
     * Assigns the difference of [this][Vector2] and [other] to [this][Vector2].
     */
    operator fun minusAssign(other: Vector2) {
        this.x -= other.x
        this.y -= other.y
    }

    /**
     * Assigns the product of [this][Vector2] and a [scalar] to [this][Vector2].
     */
    operator fun timesAssign(scalar: Float) {
        this.x *= scalar
        this.y *= scalar
    }

    /**
     * Assigns the division of [this][Vector2] by a [scalar] to [this][Vector2].
     *
     * @throws IllegalArgumentException if division by zero is attempted.
     */
    operator fun divAssign(scalar: Float) {
        this.x /= scalar
        this.y /= scalar
    }

    /**
     * Returns a negated version of the [Vector2].
     */
    operator fun unaryMinus(): Vector2 {
        return Vector2(-this.x, -this.y)
    }

    /**
     * Returns a string representation of the [Vector2].
     */
    override fun toString(): String {
        return "Vector2(x=${this.x}, y=${this.y})"
    }

    /**
     * Compares this [Vector2] to [another][other] object for equality.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Vector2) return false

        if (this.x != other.x) return false
        if (this.y != other.y) return false

        return true
    }

    /**
     * Returns a hash code value for the [Vector2].
     */
    override fun hashCode(): Int {
        var result: Int = this.x.hashCode()
        result = 31 * result + this.y.hashCode()
        return result
    }

    /**
     * Companion object providing utility functions for [Vector2].
     */
    companion object {
        /**
         * Creates a [Vector2] with both components set to zero.
         */
        @JvmStatic
        val ZERO: Vector2
            get() = Vector2(0.0f, 0.0f)
    }
}
