package de.officeryoda.fhysics.engine.math

/**
 * This class is used for the transformation of Vector2 objects.
 * In this use case, it is a section of a 3x3 matrix where it is assumed that the third row is always [0, 0, 1].
 */
class Matrix2x3(
    val m00: Float, val m01: Float, val m02: Float,
    val m10: Float, val m11: Float, val m12: Float,
) {

    /**
     * Multiplies the Matrix2x3 by this Vector2.
     *
     * Assuming that the third row of the matrix is always [0, 0, 1] and
     * the Vector2 is treated as a 3x1 matrix [x, y, 1].
     *
     * @param vector2 The Vector2 to multiply with.
     * @return The result of the multiplication.
     */
    operator fun times(vector2: Vector2): Vector2 {
        return Vector2(
            vector2.x * m00 + vector2.y * m01 + m02,
            vector2.x * m10 + vector2.y * m11 + m12
        )
    }

    // String representation of the matrix
    override fun toString(): String {
        return "Matrix2x3(\n" +
                "  [$m00, $m01, $m02],\n" +
                "  [$m10, $m11, $m12]\n" +
                ")"
    }
}