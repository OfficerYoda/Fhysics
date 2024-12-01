package de.officeryoda.fhysics.engine.math

import kotlin.math.min

/**
 * Represents the result of a projection test.
 * Containing the projection of two objects and whether they overlap.
 */
data class ProjectionResult(
    /** The first projection */
    val projectionA: Projection,
    /** The second projection */
    val projectionB: Projection,
) {

    /**
     * Whether the projections have an overlap.
     */
    val hasOverlap: Boolean = projectionA.overlaps(projectionB)

    /**
     * Returns the overlap of the projections
     * Will be negative if the projections don't overlap
     */
    fun getOverlap(): Float {
        return min(
            projectionA.max - projectionB.min,
            projectionB.max - projectionA.min
        )
    }
}