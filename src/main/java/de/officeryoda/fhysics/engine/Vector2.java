package de.officeryoda.fhysics.engine;

import lombok.Data;

/**
 * A class representing a 2D vector with double precision.
 * This class provides basic operations for vector manipulation.
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
