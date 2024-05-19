package de.officeryoda.fhysics.engine.collision

data class Projection(private val min: Float, private val max: Float) {
    fun overlaps(other: Projection): Boolean {
        return min <= other.max && max >= other.min
    }
}