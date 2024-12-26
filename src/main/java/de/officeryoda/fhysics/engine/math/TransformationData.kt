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
/**
 * A data class for storing the transformation data of a 2D transformation.
 */
value class TransformationData(
    /**
     * The transformation data.
     *
     * The data is stored in the following order:
     * 0. sin(angle)
     * 1. cos(angle)
     * 2. x-translation
     * 3. y-translation
     */
    private val data: FloatArray,
) {

    init {
        require(data.size == 4) { "TransformationData requires exactly 4 float values (sin, cos, x, y)." }
    }

    /**
     * Constructs a transformation data object from an [angle] and [translation].
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
    fun applyTo(vector: Vector2): Vector2 {
        return Vector2(
            vector.x * data[1] - vector.y * data[0] + data[2],
            vector.x * data[0] + vector.y * data[1] + data[3]
        )
    }

    override fun toString(): String {
        return "TransformationData([${data[0]}, ${data[1]}, ${data[2]}, ${data[3]}])"
    }
}
