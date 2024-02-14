package de.officeryoda.fhysics.engine.collision

import de.officeryoda.fhysics.engine.Vector2
import de.officeryoda.fhysics.engine.Vector2.Companion.ZERO

data class CollisionPoints @JvmOverloads constructor(
    val a: Vector2 = ZERO,
    val b: Vector2 = ZERO,
    val normal: Vector2 = ZERO,
    val depth: Double = -1.0,
)