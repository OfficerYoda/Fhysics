package de.officeryoda.fhysics.engine.collision

import de.officeryoda.fhysics.engine.math.Vector2
import de.officeryoda.fhysics.engine.objects.FhysicsObject

/**
 * Data class representing collision information between two physics objects.
 * @property hasCollision Indicates whether a collision has occurred.
 * @property objA The first physics object involved in the collision.
 * @property objB The second physics object involved in the collision.
 * @property normal The normal vector along the line of collision. (Points from objA to objB)
 * @property depth The overlap distance between the objects.
 */
data class CollisionInfo(
    val objA: FhysicsObject?,
    val objB: FhysicsObject?,
    val normal: Vector2,
    val depth: Float, // Might be negative under some concave polygon collisions conditions
) {

    /**
     * Indicates whether a collision has occurred.
     */
    val hasCollision: Boolean = depth != Float.NEGATIVE_INFINITY

    /**
     * Secondary constructor used to create a CollisionInfo instance when no collision occurred.
     */
    constructor() : this(null, null, Vector2.ZERO, Float.NEGATIVE_INFINITY)

    /**
     * Returns a string representation of the CollisionInfo object.
     */
    override fun toString(): String {
        return "CollisionInfo(hasCollision=$hasCollision, normal=$normal, depth=$depth, objA.id=${objA?.id}, objB.id=${objB?.id})"
    }
}