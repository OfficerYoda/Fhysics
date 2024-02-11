package de.officeryoda.fhysics.objects

import de.officeryoda.fhysics.engine.FhysicsCore
import de.officeryoda.fhysics.engine.Vector2
import lombok.ToString
import java.awt.Color

@ToString
abstract class FhysicsObject protected constructor(
    val position: Vector2 = Vector2.ZERO,
    val mass: Double = 1.0,
    val velocity: Vector2 = Vector2.ZERO,
    private val acceleration: Vector2 = Vector2.ZERO
) {
    val id = FhysicsCore.nextId()
    val color: Color = colorFromIndex()

    fun update(dt: Double, gravity: Vector2) {
        val damping = 0.05

        acceleration += gravity
        velocity += (acceleration - velocity * damping) * dt
        position += velocity * dt

        acceleration.set(Vector2.ZERO)
    }

    private fun colorFromIndex(): Color {
        val color = Color.getHSBColor(((id / 3.0f) / 255f) % 1f, 1f, 1f)

        return color
    }
}
