package de.officeryoda.fhysics.engine.math

/**
 * Represents a projection of an object onto an axis.
 */
data class Projection(
    /** The minimum value of the projection */
    val min: Float,
    /** The maximum value of the projection */
    val max: Float,
) {

    /**
     * Returns a boolean indicating whether this projection overlaps with the [other] projection.
     */
    fun overlaps(other: Projection): Boolean {
        return this.min <= other.max && other.min <= this.max
    }
}

