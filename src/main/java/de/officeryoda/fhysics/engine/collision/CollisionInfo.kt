package de.officeryoda.fhysics.engine.collision

import de.officeryoda.fhysics.engine.Vector2
import de.officeryoda.fhysics.objects.FhysicsObject

/**
 * Data class representing collision information between two physics objects.
 * @property hasCollision Indicates whether a collision has occurred.
 * @property objA The first physics object involved in the collision.
 * @property objB The second physics object involved in the collision.
 * @property normal The normal vector along the line of collision. (Points from objA to objB)
 * @property depth The overlap distance between the objects.
 */
data class CollisionInfo(
    val hasCollision: Boolean = false,
    val objA: FhysicsObject? = null,
    val objB: FhysicsObject? = null,
    val normal: Vector2 = Vector2.ZERO,
    val depth: Float = -1.0F,
) {
    /**
     * Secondary constructor used to create a CollisionInfo instance when a collision occurs.
     * @param objA The first physics object involved in the collision.
     * @param objB The second physics object involved in the collision.
     * @param normal The normal vector along the line of collision. (Points from objA to objB)
     * @param depth The overlap distance between the objects.
     */
    constructor(objA: FhysicsObject?, objB: FhysicsObject?, normal: Vector2, depth: Float) : this(
        true,
        objA,
        objB,
        normal,
        depth
    )
}