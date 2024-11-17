package de.officeryoda.fhysics.extensions

import de.officeryoda.fhysics.engine.math.Vector2

/**
 * Extension functions for Float class related to Vector2 operations.
 *
 * @property vector The Vector2 instance to be multiplied.
 * @return A new Vector2 instance resulting from multiplying each component of the vector by the Float value.
 */
operator fun Float.times(vector: Vector2): Vector2 {
    return Vector2(this * vector.x, this * vector.y)
}

/**
 * Extension functions for Double class related to Vector2 operations.
 *
 * @property vector The Vector2 instance to be multiplied.
 * @return A new Vector2 instance resulting from multiplying each component of the vector by the Double value.
 */
operator fun Double.times(vector: Vector2): Vector2 {
    return this.toFloat() * vector
}

/**
 * Extension functions for Int class related to Vector2 operations.
 *
 * @property vector The Vector2 instance to be multiplied.
 * @return A new Vector2 instance resulting from multiplying each component of the vector by the int value.
 */
operator fun Int.times(vector: Vector2): Vector2 {
    return Vector2(this * vector.x, this * vector.y)
}

/**
 * Returns the sum of the results of applying the given [selector] function to each element in the collection.
 * Kotlin does not have a built-in sumOf function for Floats, so we need to implement it ourselves.
 *
 * @param selector the function to apply to each element to get the value to sum
 * @return the sum of the results of applying the given [selector] function to each element in the collection
 * @see Iterable.sumOf
 */
inline fun <T> Iterable<T>.sumOf(selector: (T) -> Float): Float {
    var sum: Float = 0.toFloat()
    for (element: T in this) {
        sum += selector(element)
    }
    return sum
}

