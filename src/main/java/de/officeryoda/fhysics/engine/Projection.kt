package de.officeryoda.fhysics.engine

data class Projection(val min: Float, val max: Float) {
    fun overlaps(other: Projection): Boolean {
        return min <= other.max && max >= other.min
    }
}

data class ProjectionResult(val hasOverlap: Boolean, val projectionA: Projection, val projectionB: Projection)
