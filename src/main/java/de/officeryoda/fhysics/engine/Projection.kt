package de.officeryoda.fhysics.engine

import kotlin.math.min

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
        return min <= other.max && max >= other.min
    }
}

/**
 * Represents the result of a projection test
 *
 * @param projectionA The first projection
 * @param projectionB The second projection
 */
data class ProjectionResult(val projectionA: Projection, val projectionB: Projection) {

    /**
     * Whether the projections have an overlap
     */
    val hasOverlap: Boolean = projectionA.overlaps(projectionB)

    /**
     * Returns the overlap of the projections
     * Will be negative if the projections don't overlap
     *
     * @return The overlap of the projections
     */
    fun getOverlap(): Float {
        return min(
            projectionA.max - projectionB.min,
            projectionB.max - projectionA.min
        )
    }
}
