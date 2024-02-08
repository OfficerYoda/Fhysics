package de.officeryoda.fhysics.engine;

import lombok.Data;

/**
 * A class representing a 2D vector with double precision.
 */
@Data
public class Vector2 {

    /**
     * A constant Vector2 with components (0.0, 0.0).
     */
    public static final Vector2 ZERO = new Vector2(0.0, 0.0);

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
     * Adds another vector to this vector.
     *
     * @param other The vector to be added.
     * @return A new Vector2 representing the sum of the two vectors.
     */
    public Vector2 add(Vector2 other) {
        this.x += other.x;
        this.y += other.y;
        return this;
    }

    /**
     * Subtracts another vector from this vector.
     *
     * @param other The vector to be subtracted.
     * @return A new Vector2 representing the difference between the two vectors.
     */
    public Vector2 subtract(Vector2 other) {
        this.x -= other.x;
        this.y -= other.y;
        return this;
    }

    /**
     * Multiplies the vector by a scalar value.
     *
     * @param scalar The scalar value.
     * @return A new Vector2 representing the scaled vector.
     */
    public Vector2 multiply(double scalar) {
        this.x *= scalar;
        this.y *= scalar;
        return this;
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
     * Sets the components of the vector based on another Vector2.
     *
     * @param vec2 The Vector2 to copy components from.
     */
    public void set(Vector2 vec2) {
        this.x = vec2.x;
        this.y = vec2.y;
    }
}
