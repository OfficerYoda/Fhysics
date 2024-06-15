package de.officeryoda.fhysics.engine.objects

import de.officeryoda.fhysics.engine.Vector2
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.math.sqrt

class PolygonTest {

    @Test
    fun `calculateInertia returns correct inertia for regular polygon (rectangle)`() {
        val vertices = arrayOf(Vector2(0f, 0f), Vector2(1f, 0f), Vector2(1f, 1f), Vector2(0f, 1f))
        val polygon = PolygonCreator.createPolygon(vertices)

        val inertia = polygon.calculateInertia()

        assertEquals(0.16666667f, inertia, 0.0001f)
    }

    @Test
    fun `calculateInertia returns correct inertia for regular polygon (triangle)`() {
        val vertices = arrayOf(Vector2(0f, 0f), Vector2(2f, 0f), Vector2(1f, sqrt(3f)))
        val polygon = PolygonCreator.createPolygon(vertices)

        val inertia = polygon.calculateInertia()

        assertEquals(0.57735026f, inertia, 0.0001f)
    }

    @Test
    fun `calculateInertia returns correct inertia for irregular polygon`() {
        val vertices = arrayOf(Vector2(0f, 0f), Vector2(2f, 0f), Vector2(0f, 2f))
        val polygon = PolygonCreator.createPolygon(vertices)

        val inertia = polygon.calculateInertia()

        assertEquals(0.8888889f, inertia, 0.0001f)
    }

    @Test
    fun `calculateInertia returns correct inertia for rotated polygon`() {
        val vertices = arrayOf(Vector2(0f, 0f), Vector2(1f, 0f), Vector2(1f, 1f), Vector2(0f, 1f))
        val polygon = PolygonCreator.createPolygon(vertices)
        polygon.rotation = Math.PI.toFloat() / 4f

        val inertia = polygon.calculateInertia()

        assertEquals(0.16666667f, inertia, 0.0001f)
    }

    @Test
    fun `RigidBody2 from shape`() {
        val vertices = arrayOf(
            Vector2(0.75f, 0f),
            Vector2(2f, 0f),
            Vector2(2f, 0.5f),
            Vector2(1.25f, 0.5f),
            Vector2(1.25f, 1.5f),
            Vector2(0f, 1.5f),
            Vector2(0f, 1f),
            Vector2(0.75f, 1f)
        )
        val polygon = PolygonCreator.createPolygon(vertices.map { it.copy() }.toTypedArray())

        assertEquals(Vector2(1.0f, 0.75f), Polygon.calculatePolygonCenter(vertices))
        assertEquals(0.6875f, polygon.calculateInertia(), 1.0e-6f)
    }
}