package de.officeryoda.fhysics.objects

import de.officeryoda.fhysics.engine.FhysicsCore
import de.officeryoda.fhysics.engine.Vector2
import de.officeryoda.fhysics.engine.collision.CollisionInfo
import java.awt.Color

abstract class FhysicsObject protected constructor(
    val position: Vector2 = Vector2.ZERO,
    val mass: Double = 1.0,
    val velocity: Vector2 = Vector2.ZERO,
    val acceleration: Vector2 = Vector2.ZERO,
) {
    val id = FhysicsCore.nextId()
    var color: Color = colorFromIndex()
    var static: Boolean = false

    open fun updatePosition(dt: Double, gravity: Vector2) {
        // static objects don't move
        if (static) return

        val damping = 0.00

        acceleration += gravity
        velocity += (acceleration - velocity * damping) * dt
        position += velocity * dt

        acceleration.set(Vector2.ZERO)
    }

    private fun colorFromIndex(): Color {
        val colors =
            listOf(Color.decode("#32a852"), Color.decode("#4287f5"), Color.decode("#eb4034"), Color.decode("#fcba03"))
        return colors[id % 1]
//        val color = Color.getHSBColor(((id / 3.0f) / 255f) % 1f, 1f, 1f)
//
//        return color
    }

    fun testCollision(other: FhysicsObject): CollisionInfo {
        return when (other) {
            is Circle -> testCollision(other)
            is Box -> testCollision(other)
            else -> throw IllegalArgumentException("Unsupported object type for collision")
        }
    }

    abstract fun testCollision(other: Circle): CollisionInfo

    abstract fun testCollision(other: Box): CollisionInfo

    override fun toString(): String {
        return "FhysicsObject(id=$id, position=$position, velocity=$velocity, acceleration=$acceleration, mass=$mass, static=$static, color=$color)"
    }

    // this method exist to make the list.contains() method faster
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FhysicsObject) return false

        // id is unique
        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}
