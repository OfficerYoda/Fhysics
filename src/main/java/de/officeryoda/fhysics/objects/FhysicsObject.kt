package de.officeryoda.fhysics.objects

import de.officeryoda.fhysics.engine.FhysicsCore
import de.officeryoda.fhysics.engine.Vector2
import java.awt.Color

abstract class FhysicsObject protected constructor(
    val position: Vector2 = Vector2.ZERO,
    val mass: Double = 1.0,
    val velocity: Vector2 = Vector2.ZERO,
    val acceleration: Vector2 = Vector2.ZERO
) {
    val id = FhysicsCore.nextId()
    var color: Color = colorFromIndex()

    open fun update(dt: Double, gravity: Vector2) {
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
        when (other) {
            is Circle -> handleCollision(other)
            is Box -> handleCollision(other)
            else -> throw IllegalArgumentException("Unsupported object type for collision")
        }
    }

    abstract fun handleCollision(other: Circle)

    abstract fun handleCollision(other: Box)

    override fun toString(): String {
        return "FhysicsObject(id=$id, position=$position, velocity=$velocity, acceleration=$acceleration, mass=$mass, color=$color)"
    }
}
