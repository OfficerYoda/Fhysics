package de.officeryoda.fhysics.engine

import lombok.Data
import kotlin.math.sqrt

/**
 * A class representing a 2D vector with int precision.
 * This class provides basic operations for vector manipulation.
 */
@Data
class Vector2Int
/**
 * Constructs a Vector2Int with default values (0, 0).
 */ @JvmOverloads constructor(
    /**
     * The x-component of the vector.
     */
    var x: Int = 0,
    /**
     * The y-component of the vector.
     */
    var y: Int = 0
) {
    /**
     * Constructs a new Vector2Int with specified x and y components.
     *
     * @param x The x-component of the vector.
     * @param y The y-component of the vector.
     */

    /**
     * Sets the x and y components of the vector.
     *
     * @param x The new x-component.
     * @param y The new y-component.
     */
    fun set(x: Int, y: Int) {
        this.x = x
        this.y = y
    }

    /**
     * Sets the components of the vector based on another Vector2Int.
     *
     * @param vec2 The Vector2Int to copy components from.
     */
    fun set(vec2: Vector2Int) {
        this.x = vec2.x
        this.y = vec2.y
    }

    /**
     * Adds another Vector2Int to this vector.
     *
     * @param other The Vector2Int to be added.
     * @return The updated Vector2Int after addition.
     */
    fun add(other: Vector2Int): Vector2Int {
        this.x += other.x
        this.y += other.y
        return this
    }

    /**
     * Creates a new Vector2Int by adding another Vector2Int to this vector.
     *
     * @param other The Vector2Int to be added.
     * @return A new Vector2Int representing the sum of the two vectors.
     */
    fun addNew(other: Vector2Int): Vector2Int {
        return Vector2Int(this.x + other.x, this.y + other.y)
    }

    /**
     * Subtracts another Vector2Int from this vector.
     *
     * @param other The Vector2Int to be subtracted.
     * @return The updated Vector2Int after subtraction.
     */
    fun subtract(other: Vector2Int): Vector2Int {
        this.x -= other.x
        this.y -= other.y
        return this
    }

    /**
     * Creates a new Vector2Int by subtracting another Vector2Int from this vector.
     *
     * @param other The Vector2Int to be subtracted.
     * @return A new Vector2Int representing the difference of the two vectors.
     */
    fun subtractNew(other: Vector2Int): Vector2Int {
        return Vector2Int(this.x - other.x, this.y - other.y)
    }

    /**
     * Multiplies the vector by a scalar.
     *
     * @param scalar The scalar value for multiplication.
     * @return The updated Vector2Int after multiplication.
     */
    fun multiply(scalar: Int): Vector2Int {
        this.x *= scalar
        this.y *= scalar
        return this
    }

    /**
     * Creates a new Vector2Int by multiplying the vector by a scalar.
     *
     * @param scalar The scalar value for multiplication.
     * @return A new Vector2Int representing the result of the multiplication.
     */
    fun multiplyNew(scalar: Int): Vector2Int {
        return Vector2Int(this.x * scalar, this.y * scalar)
    }

    /**
     * Calculates the magnitude (length) of the vector.
     *
     * @return The magnitude of the vector.
     */
    fun magnitude(): Double {
        return sqrt(sqrMagnitude().toDouble())
    }

    /**
     * Calculates the dot product of this vector and another Vector2.
     *
     * @param other The Vector2Int to calculate the dot product with.
     * @return The dot product of the two vectors.
     */
    fun dot(other: Vector2Int): Double {
        return (x * other.x + this.y * other.y).toDouble()
    }

    /**
     * Calculates the squared magnitude of the vector.
     *
     * @return The squared magnitude of the vector.
     */
    fun sqrMagnitude(): Int {
        return x * x + y * y
    }

    /**
     * Calculates the Euclidean distance between this vector and another Vector2.
     *
     * @param other The Vector2 to calculate the distance to.
     * @return The Euclidean distance between this vector and the specified Vector2.
     */
    fun distance(other: Vector2Int): Double {
        return sqrt(sqrDistance(other).toDouble())
    }

    /**
     * Calculates the squared Euclidean distance between this vector and another Vector2.
     * This method is computationally less expensive than distance() as it avoids the square root operation.
     *
     * @param other The Vector2 to calculate the squared distance to.
     * @return The squared Euclidean distance between this vector and the specified Vector2.
     */
    fun sqrDistance(other: Vector2Int): Int {
        val dx = this.x - other.x
        val dy = this.y - other.y
        return dx * dx + dy * dy
    }

    companion object {
        /**
         * Gets a constant Vector2Int with components (0, 0).
         *
         * @return A Vector2Int with components (0, 0).
         */
        fun zero(): Vector2Int {
            return Vector2Int(0, 0)
        }
    }
}
