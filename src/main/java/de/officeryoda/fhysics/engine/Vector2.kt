package de.officeryoda.fhysics.engine;

import lombok.Data;

/**
 * A class representing a 2D vector with double precision.
 * This class provides basic operations for vector manipulation.
 */
@Data
public class Vector2 {

    /**
     * The x-component of the vector.
     */
    private double x;
    /**
     * The y-component of the vector.
     */
    private double y;

    /**
     * Constructs a new Vector2 with specified x and y components.
     *
     * @param x The x-component of the vector.
     * @param y The y-component of the vector.
     */
    public Vector2(double x, double y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Constructs a Vector2 with default values (0.0, 0.0).
     */
    public Vector2() {
        this(0.0, 0.0);
    }

    /**
     * Gets a constant Vector2 with components (0.0, 0.0).
     *
     * @return A Vector2 with components (0.0, 0.0).
     */
    public static Vector2 zero() {
        return new Vector2(0.0, 0.0);
    }

    /**
     * Sets the components of the vector based on another Vector2.
     *
     * @param vec2 The Vector2 to copy components from.
     */
    public void set(Vector2 vec2) {
        this.x = vec2.x;
        this.y = vec2.y;
    }

    /**
     * Adds another Vector2 to this vector.
     *
     * @param other The Vector2 to be added.
     * @return The updated Vector2 after addition.
     */
    public Vector2 add(Vector2 other) {
        this.x += other.x;
        this.y += other.y;
        return this;
    }

    /**
     * Creates a new Vector2 by adding another Vector2 to this vector.
     *
     * @param other The Vector2 to be added.
     * @return A new Vector2 representing the sum of the two vectors.
     */
    public Vector2 addNew(Vector2 other) {
        return new Vector2(this.x + other.x, this.y + other.y);
    }

    /**
     * Subtracts another Vector2 from this vector.
     *
     * @param other The Vector2 to be subtracted.
     * @return The updated Vector2 after subtraction.
     */
    public Vector2 subtract(Vector2 other) {
        this.x -= other.x;
        this.y -= other.y;
        return this;
    }

    /**
     * Creates a new Vector2 by subtracting another Vector2 from this vector.
     *
     * @param other The Vector2 to be subtracted.
     * @return A new Vector2 representing the difference of the two vectors.
     */
    public Vector2 subtractNew(Vector2 other) {
        return new Vector2(this.x - other.x, this.y - other.y);
    }

    /**
     * Multiplies the vector by a scalar.
     *
     * @param scalar The scalar value for multiplication.
     * @return The updated Vector2 after multiplication.
     */
    public Vector2 multiply(double scalar) {
        this.x *= scalar;
        this.y *= scalar;
        return this;
    }

    /**
     * Creates a new Vector2 by multiplying the vector by a scalar.
     *
     * @param scalar The scalar value for multiplication.
     * @return A new Vector2 representing the result of the multiplication.
     */
    public Vector2 multiplyNew(double scalar) {
        return new Vector2(this.x * scalar, this.y * scalar);
    }

    /**
     * Calculates the magnitude (length) of the vector.
     *
     * @return The magnitude of the vector.
     */
    public double magnitude() {
        return Math.sqrt(sqrMagnitude());
    }

    /**
     * Normalizes the vector, making it a unit vector (magnitude = 1).
     *
     * @return The normalized Vector2.
     */
    public Vector2 normalize() {
        double magnitude = magnitude();
        if (magnitude != 0) {
            this.x /= magnitude;
            this.y /= magnitude;
        }
        return this;
    }

    /**
     * Calculates the dot product of this vector and another Vector2.
     *
     * @param other The Vector2 to calculate the dot product with.
     * @return The dot product of the two vectors.
     */
    public double dot(Vector2 other) {
        return this.x * other.x + this.y * other.y;
    }

    /**
     * Calculates the squared magnitude of the vector.
     *
     * @return The squared magnitude of the vector.
     */
    public double sqrMagnitude() {
        return x * x + y * y;
    }

    /**
     * Sets the x and y components of the vector.
     *
     * @param x The new x-component.
     * @param y The new y-component.
     */
    public void set(double x, double y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Calculates the Euclidean distance between this vector and another Vector2.
     *
     * @param other The Vector2 to calculate the distance to.
     * @return The Euclidean distance between this vector and the specified Vector2.
     */
    public double distance(Vector2 other) {
        return Math.sqrt(sqrDistance(other));
    }

    /**
     * Calculates the squared Euclidean distance between this vector and another Vector2.
     * This method is computationally less expensive than distance() as it avoids the square root operation.
     *
     * @param other The Vector2 to calculate the squared distance to.
     * @return The squared Euclidean distance between this vector and the specified Vector2.
     */
    public double sqrDistance(Vector2 other) {
        double dx = this.x - other.x;
        double dy = this.y - other.y;
        return dx * dx + dy * dy;
    }
}
