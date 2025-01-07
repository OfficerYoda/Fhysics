package de.officeryoda.fhysics.engine.util

import de.officeryoda.fhysics.engine.math.Vector2
import kotlin.math.ceil

/**
 * Multiplies a float with a vector.
 */
operator fun Float.times(vector: Vector2): Vector2 {
    return Vector2(this * vector.x, this * vector.y)
}

/**
 * Rounds the float value down to the nearest integer.
 */
fun Float.floorToInt(): Int {
    return this.toInt()
}

/**
 * Rounds the float value up to the nearest integer.
 */
fun Float.ceilToInt(): Int {
    return ceil(this).toInt()
}

