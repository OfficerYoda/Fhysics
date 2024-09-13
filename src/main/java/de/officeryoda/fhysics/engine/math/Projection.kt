package de.officeryoda.fhysics.engine.math

/**
 * Represents a projection of a shape onto an axis
 *
 * @param min The minimum scalar value of the projection
 * @param max The maximum scalar value of the projection
 */
data class Projection(val min: Float, val max: Float) {
    /**
     * Checks if this projection overlaps with another projection
     *
     * @param other The other projection
     * @return Whether the projections overlap
     */
    fun overlaps(other: Projection): Boolean {
        return min <= other.max && other.min <= max
    }
}

