package de.officeryoda.fhysics.engine;

import lombok.Data;

/**
 * A class representing a 2D vector with int precision.
 * This class provides basic operations for vector manipulation.
 */
@Data
public class Vector2Int {

    /**
     * The x-component of the vector.
     */
    private int x;
    /**
     * The y-component of the vector.
     */
    private int y;

    /**
     * Constructs a new Vector2Int with specified x and y components.
     *
     * @param x The x-component of the vector.
     * @param y The y-component of the vector.
     */
    public Vector2Int(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Constructs a Vector2Int with default values (0, 0).
     */
    public Vector2Int() {
        this(0, 0);
    }

    /**
     * Gets a constant Vector2Int with components (0, 0).
     *
     * @return A Vector2Int with components (0, 0).
     */
    public static Vector2Int zero() {
        return new Vector2Int(0, 0);
    }

    /**
     * Adds another Vector2Int to this vector.
     *
     * @param other The Vector2Int to be added.
     * @return The updated Vector2Int after addition.
     */
    public Vector2Int add(Vector2Int other) {
        this.x += other.x;
        this.y += other.y;
        return this;
    }

    /**
     * Creates a new Vector2Int by adding another Vector2Int to this vector.
     *
     * @param other The Vector2Int to be added.
     * @return A new Vector2Int representing the sum of the two vectors.
     */
    public Vector2Int addNew(Vector2Int other) {
        return new Vector2Int(this.x + other.x, this.y + other.y);
    }

    /**
     * Subtracts another Vector2Int from this vector.
     *
     * @param other The Vector2Int to be subtracted.
     * @return The updated Vector2Int after subtraction.
     */
    public Vector2Int subtract(Vector2Int other) {
        this.x -= other.x;
        this.y -= other.y;
        return this;
    }

    /**
     * Creates a new Vector2Int by subtracting another Vector2Int from this vector.
     *
     * @param other The Vector2Int to be subtracted.
     * @return A new Vector2Int representing the difference of the two vectors.
     */
    public Vector2Int subtractNew(Vector2Int other) {
        return new Vector2Int(this.x - other.x, this.y - other.y);
    }

    /**
     * Multiplies the vector by a scalar.
     *
     * @param scalar The scalar value for multiplication.
     * @return The updated Vector2Int after multiplication.
     */
    public Vector2Int multiply(int scalar) {
        this.x *= scalar;
        this.y *= scalar;
        return this;
    }

    /**
     * Creates a new Vector2Int by multiplying the vector by a scalar.
     *
     * @param scalar The scalar value for multiplication.
     * @return A new Vector2Int representing the result of the multiplication.
     */
    public Vector2Int multiplyNew(int scalar) {
        return new Vector2Int(this.x * scalar, this.y * scalar);
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
    public void set(int x, int y) {
        this.x = x;
        this.y = y;
    }

    /**
     * Sets the components of the vector based on another Vector2Int.
     *
     * @param vec2 The Vector2Int to copy components from.
     */
    public void set(Vector2Int vec2) {
        this.x = vec2.x;
        this.y = vec2.y;
    }
}
