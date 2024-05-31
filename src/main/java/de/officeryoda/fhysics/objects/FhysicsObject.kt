package de.officeryoda.fhysics.objects

import de.officeryoda.fhysics.engine.FhysicsCore
import de.officeryoda.fhysics.engine.Vector2
import de.officeryoda.fhysics.engine.collision.CollisionInfo
import de.officeryoda.fhysics.engine.collision.Projection
import java.awt.Color

abstract class FhysicsObject protected constructor(
    val position: Vector2,
    var mass: Float,
    var rotation: Float = 0f,
) {
    val id: Int = FhysicsCore.nextId()
    var color: Color = colorFromIndex()

    var static: Boolean = false
        set(value) {
            field = value
            if (value) {
                // Stop any movement if the object is set to static
                acceleration.set(Vector2.ZERO)
                velocity.set(Vector2.ZERO)
            }
        }
    val acceleration: Vector2 = Vector2.ZERO
    val velocity: Vector2 = Vector2.ZERO

    private var lastUpdate = -1

    open fun updatePosition() {
        // Static objects don't move
        if (static) return
        // Needed because multiple quadtree nodes can contain the same object
        if (lastUpdate == FhysicsCore.updateCount) return
        lastUpdate = FhysicsCore.updateCount

        val dt: Float = FhysicsCore.dt
//        val damping = 0.00F

        acceleration += FhysicsCore.gravityAt(position)
        // Update velocity before position (semi-implicit Euler)
        velocity += acceleration * dt
//        velocity *= (1 - damping)
        position += velocity * dt
        acceleration.set(Vector2.ZERO)
    }

    private fun colorFromIndex(): Color {
        val colors: List<Color> =
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

    abstract fun project(axis: Vector2): Projection

    abstract fun contains(pos: Vector2): Boolean

    override fun toString(): String {
        return "FhysicsObject(id=$id, position=$position, velocity=$velocity, acceleration=$acceleration, mass=$mass, static=$static, color=$color)"
    }

    // This method exist to make the list.contains() method faster
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FhysicsObject) return false

        // Id is unique
        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}
