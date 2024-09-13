package de.officeryoda.fhysics.engine.math

import kotlin.math.min

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