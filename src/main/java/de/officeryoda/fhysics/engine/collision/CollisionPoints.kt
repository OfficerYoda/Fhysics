package de.officeryoda.fhysics.engine.collision

import de.officeryoda.fhysics.engine.Vector2
import de.officeryoda.fhysics.engine.Vector2.Companion.ZERO

data class CollisionPoints @JvmOverloads constructor(

    /**
     * Furthest point of objB inside objA
     */
    val pointInA: Vector2 = ZERO,

    /**
     * Furthest point of objA inside objB
     */
    val pointInB: Vector2 = ZERO,

    /**
     * pointInB - pointInA normalized
     */
    val normal: Vector2 = ZERO,

    /**
     * Length of B-A
     */
    val depth: Double = -1.0,
)