package de.officeryoda.fhysics.engine

import lombok.Data
import kotlin.math.sqrt

/**
 * A class representing a 2D vector with double precision.
 * This class provides basic operations for vector manipulation.
 */
@Data
class Vector2
/**
 * Constructs a Vector2 with default values (0.0, 0.0).
 */ @JvmOverloads constructor(
    /**
     * The x-component of the vector.
     */
    var x: Double = 0.0,
    /**
     * The y-component of the vector.
     */
    var y: Double = 0.0
) {
    /**
     * Constructs a new Vector2 with specified x and y components.
     *
     * @param x The x-component of the vector.
     * @param y The y-component of the vector.
     */

    /**
     * Sets the components of the vector based on another Vector2.
     *
     * @param vec2 The Vector2 to copy components from.
     */
    fun set(vec2: Vector2) {
        this.x = vec2.x
        this.y = vec2.y
    }

    /**
     * Adds another Vector2 to this vector.
     *
     * @param other The Vector2 to be added.
     * @return The updated Vector2 after addition.
     */
    fun add(other: Vector2): Vector2 {
        this.x += other.x
        this.y += other.y
        return this
    }

    /**
     * Creates a new Vector2 by adding another Vector2 to this vector.
     *
     * @param other The Vector2 to be added.
     * @return A new Vector2 representing the sum of the two vectors.
     */
    fun addNew(other: Vector2): Vector2 {
        return Vector2(this.x + other.x, this.y + other.y)
    }

    /**
     * Subtracts another Vector2 from this vector.
     *
     * @param other The Vector2 to be subtracted.
     * @return The updated Vector2 after subtraction.
     */
    fun subtract(other: Vector2): Vector2 {
        this.x -= other.x
        this.y -= other.y
        return this
    }

    /**
     * Creates a new Vector2 by subtracting another Vector2 from this vector.
     *
     * @param other The Vector2 to be subtracted.
     * @return A new Vector2 representing the difference of the two vectors.
     */
    fun subtractNew(other: Vector2): Vector2 {
        return Vector2(this.x - other.x, this.y - other.y)
    }

    /**
     * Multiplies the vector by a scalar.
     *
     * @param scalar The scalar value for multiplication.
     * @return The updated Vector2 after multiplication.
     */
    fun multiply(scalar: Double): Vector2 {
        this.x *= scalar
        this.y *= scalar
        return this
    }

    /**
     * Creates a new Vector2 by multiplying the vector by a scalar.
     *
     * @param scalar The scalar value for multiplication.
     * @return A new Vector2 representing the result of the multiplication.
     */
    fun multiplyNew(scalar: Double): Vector2 {
        return Vector2(this.x * scalar, this.y * scalar)
    }

    /**
     * Calculates the magnitude (length) of the vector.
     *
     * @return The magnitude of the vector.
     */
    fun magnitude(): Double {
        return sqrt(sqrMagnitude())
    }

    /**
     * Normalizes the vector, making it a unit vector (magnitude = 1).
     *
     * @return The normalized Vector2.
     */
    fun normalize(): Vector2 {
        val magnitude = magnitude()
        if (magnitude != 0.0) {
            this.x /= magnitude
            this.y /= magnitude
        }
        return this
    }

    /**
     * Calculates the dot product of this vector and another Vector2.
     *
     * @param other The Vector2 to calculate the dot product with.
     * @return The dot product of the two vectors.
     */
    fun dot(other: Vector2): Double {
        return this.x * other.x + this.y * other.y
    }

    /**
     * Calculates the squared magnitude of the vector.
     *
     * @return The squared magnitude of the vector.
     */
    fun sqrMagnitude(): Double {
        return x * x + y * y
    }

    /**
     * Sets the x and y components of the vector.
     *
     * @param x The new x-component.
     * @param y The new y-component.
     */
    fun set(x: Double, y: Double) {
        this.x = x
        this.y = y
    }

    /**
     * Calculates the Euclidean distance between this vector and another Vector2.
     *
     * @param other The Vector2 to calculate the distance to.
     * @return The Euclidean distance between this vector and the specified Vector2.
     */
    fun distance(other: Vector2): Double {
        return sqrt(sqrDistance(other))
    }

    /**
     * Calculates the squared Euclidean distance between this vector and another Vector2.
     * This method is computationally less expensive than distance() as it avoids the square root operation.
     *
     * @param other The Vector2 to calculate the squared distance to.
     * @return The squared Euclidean distance between this vector and the specified Vector2.
     */
    fun sqrDistance(other: Vector2): Double {
        val dx = this.x - other.x
        val dy = this.y - other.y
        return dx * dx + dy * dy
    }

    companion object {
        /**
         * Gets a constant Vector2 with components (0.0, 0.0).
         *
         * @return A Vector2 with components (0.0, 0.0).
         */
        @JvmStatic
        fun zero(): Vector2 {
            return Vector2(0.0, 0.0)
        }
    }
}
