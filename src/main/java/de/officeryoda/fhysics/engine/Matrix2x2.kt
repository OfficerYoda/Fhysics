package de.officeryoda.fhysics.engine

class Matrix2x2(
    val m00: Float, val m01: Float,
    val m10: Float, val m11: Float,
) {

    // Addition of two 2x2 matrices
    operator fun plus(other: Matrix2x2): Matrix2x2 {
        return Matrix2x2(
            m00 + other.m00, m01 + other.m01,
            m10 + other.m10, m11 + other.m11
        )
    }

    // Subtraction of two 2x2 matrices
    operator fun minus(other: Matrix2x2): Matrix2x2 {
        return Matrix2x2(
            m00 - other.m00, m01 - other.m01,
            m10 - other.m10, m11 - other.m11
        )
    }

    // Multiplication of two 2x2 matrices
    operator fun times(other: Matrix2x2): Matrix2x2 {
        return Matrix2x2(
            m00 * other.m00 + m01 * other.m10, m00 * other.m01 + m01 * other.m11,
            m10 * other.m00 + m11 * other.m10, m10 * other.m01 + m11 * other.m11
        )
    }

    /**
     * Multiplication of a 2x2 matrix with a 2D vector.
     *
     * @param vector The 2D vector to multiply with.
     * @return The resulting 2D vector.
     */
    operator fun times(vector: Vector2): Vector2 {
        return Vector2(
            m00 * vector.x + m01 * vector.y,
            m10 * vector.x + m11 * vector.y
        )
    }

    // String representation of the matrix
    override fun toString(): String {
        return "Matrix2x2(\n" +
                "  [$m00, $m01],\n" +
                "  [$m10, $m11]\n" +
                ")"
    }
}