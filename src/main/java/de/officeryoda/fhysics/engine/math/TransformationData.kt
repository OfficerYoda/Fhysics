package de.officeryoda.fhysics.engine.math

import kotlin.math.cos
import kotlin.math.sin

/**
 * A data class for storing transformation data.
 * It stores the transformation data in the following order:
 * 0. sin(angle)
 * 1. cos(angle)
 * 2. x-translation
 * 3. y-translation
 */
@JvmInline
// Inline classes reduce memory overhead and improve performance
// by eliminating runtime object allocation and directly using the underlying value.
value class TransformationData(
    /**
     * The transformation data.
     */
    private val data: FloatArray,
) {

    init {
        require(data.size == 4) { "TransformationData requires exactly 4 float values (sin, cos, x, y)." }
    }

    /**
     * Constructs a transformation data object from an angle and translation.
     */
    constructor(angle: Float, translation: Vector2) : this(
        floatArrayOf(
            sin(angle),
            cos(angle),
            translation.x,
            translation.y
        )
    )

    /**
     * Applies the transformation to a vector.
     */
    operator fun times(vector2: Vector2): Vector2 {
        // Order: sin, cos, x, y
        return Vector2(
            vector2.x * data[1] - vector2.y * data[0] + data[2],
            vector2.x * data[0] + vector2.y * data[1] + data[3]
        )
    }

    override fun toString(): String {
        return "TransformationData([${data[0]}, ${data[1]}, ${data[2]}, ${data[3]}])"
    }
}
