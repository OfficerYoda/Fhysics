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
    var color: Color = colorFromIndex()

    fun update(dt: Double, gravity: Vector2) {
        val damping = 0.00

        acceleration += gravity
        velocity += (acceleration - velocity * damping) * dt
        position += velocity * dt

        acceleration.set(Vector2.ZERO)
    }

    fun updateColor() {
        color = colorFromIndex()
    }

    private fun colorFromIndex(): Color {
        val colors =
            listOf(Color.decode("#32a852"), Color.decode("#4287f5"), Color.decode("#eb4034"), Color.decode("#fcba03"))
        return colors[id % 1]
//        val color = Color.getHSBColor(((id / 3.0f) / 255f) % 1f, 1f, 1f)
//
//        return color
    }

    fun handleCollision(other: FhysicsObject) {
        other.handleCollision(this) // double dispatch (should not loop infinitely because there should never be a plane FhysicsObject)
    }

    abstract fun handleCollision(other: Circle)

    abstract fun handleCollision(other: Box)

    override fun toString(): String {
        return "FhysicsObject(position=$position, mass=$mass, velocity=$velocity, acceleration=$acceleration, id=$id, color=$color)"
    }
}
