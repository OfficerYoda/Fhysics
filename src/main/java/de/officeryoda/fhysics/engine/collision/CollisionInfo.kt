package de.officeryoda.fhysics.engine.collision

import de.officeryoda.fhysics.engine.math.Vector2
import de.officeryoda.fhysics.engine.objects.FhysicsObject

/**
 * Data class representing collision information between two physics objects.
 */
data class CollisionInfo(
    val objA: FhysicsObject?,
    val objB: FhysicsObject?,
    /** The normal vector along the line of collision. (Points from objA to objB) */
    val normal: Vector2,
    /** The overlap distance between the objects. */
    val depth: Float,
) {

    /** Indicates whether a collision has occurred */
    val hasCollision: Boolean get() = depth != Float.POSITIVE_INFINITY

    /**
     * Secondary constructor used to create a CollisionInfo instance when no collision occurred.
     */
    constructor() : this(null, null, Vector2.ZERO, Float.POSITIVE_INFINITY)

    override fun toString(): String {
        return "CollisionInfo(hasCollision=$hasCollision, normal=$normal, depth=$depth, objA.id=${objA?.id}, objB.id=${objB?.id})"
    }
}