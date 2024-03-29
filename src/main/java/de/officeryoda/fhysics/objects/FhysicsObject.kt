package de.officeryoda.fhysics.objects

import de.officeryoda.fhysics.engine.FhysicsCore
import de.officeryoda.fhysics.engine.Vector2
import de.officeryoda.fhysics.engine.collision.CollisionInfo
import java.awt.Color

abstract class FhysicsObject protected constructor(
    val position: Vector2 = Vector2.ZERO,
    val mass: Float = 1.0F,
    val velocity: Vector2 = Vector2.ZERO,
    val acceleration: Vector2 = Vector2.ZERO,
) {
    val id: Int = FhysicsCore.nextId()
    var color: Color = colorFromIndex()
    var static: Boolean = false

    private var lastUpdate = -1

    open fun updatePosition() {
        // static objects don't move
        if (static) return
        if(lastUpdate == FhysicsCore.updateCount) return
        
        val dt: Float = FhysicsCore.dt
        val damping = 0.00F

        acceleration += FhysicsCore.gravityAt(position)
        velocity += (acceleration - velocity * damping) * dt
        position += velocity * dt

        acceleration.set(Vector2.ZERO)

        lastUpdate = FhysicsCore.updateCount
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
            is Rectangle -> testCollision(other)
            else -> throw IllegalArgumentException("Unsupported object type for collision")
        }
    }

    abstract fun testCollision(other: Circle): CollisionInfo

    abstract fun testCollision(other: Rectangle): CollisionInfo

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
