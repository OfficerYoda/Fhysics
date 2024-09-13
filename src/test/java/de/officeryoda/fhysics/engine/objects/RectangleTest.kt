package de.officeryoda.fhysics.engine.objects

import de.officeryoda.fhysics.engine.math.Vector2
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RectangleTest {

    @Test
    fun `calculateInertia returns correct inertia for square`() {
        val rectangle = Rectangle(Vector2(10f, 0f), 1f, 1f)

        val inertia = rectangle.calculateInertia()

        assertEquals(0.16666667f, inertia, 0.0001f)
    }

    @Test
    fun `calculateInertia returns correct inertia for rectangle with different width and height`() {
        val rectangle = Rectangle(Vector2(0f, 10f), 3f, 2f)

        val inertia = rectangle.calculateInertia()

        assertEquals(6.5f, inertia, 0.0001f)
    }

    @Test
    fun `calculateInertia returns correct inertia for rotated rectangle`() {
        val rectangle = Rectangle(Vector2(0f, 0f), 1f, 1f, Math.PI.toFloat() / 4)

        val inertia = rectangle.calculateInertia()

        assertEquals(0.16666667f, inertia, 0.0001f)
    }
}