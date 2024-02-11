package de.officeryoda.fhysics.extensions

import de.officeryoda.fhysics.engine.Vector2

/**
 * Extension functions for Double class related to Vector2 operations.
 *
 * @property vector The Vector2 instance to be multiplied.
 * @return A new Vector2 instance resulting from multiplying each component of the vector by the double value.
 */
operator fun Double.times(vector: Vector2): Vector2 {
    return Vector2(this * vector.x, this * vector.y)
}
