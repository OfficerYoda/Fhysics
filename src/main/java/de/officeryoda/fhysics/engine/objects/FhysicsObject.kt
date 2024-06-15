package de.officeryoda.fhysics.engine.objects

import de.officeryoda.fhysics.engine.BoundingBox
import de.officeryoda.fhysics.engine.FhysicsCore
import de.officeryoda.fhysics.engine.Projection
import de.officeryoda.fhysics.engine.Vector2
import de.officeryoda.fhysics.engine.collision.CollisionInfo
import java.awt.Color

abstract class FhysicsObject protected constructor(
    open val position: Vector2,
    val velocity: Vector2 = Vector2.ZERO,
    mass: Float,
    var rotation: Float = 0f, // in radians
) {

    val id: Int = FhysicsCore.nextId()
    var color: Color = colorFromId()
    val boundingBox: BoundingBox = BoundingBox()
        get() {
            // only update the bounding box if it hasn't been updated this update cycle
            if (lastBBoxUpdate != FhysicsCore.updateCount) {
                lastBBoxUpdate = FhysicsCore.updateCount
                boundingBox.setFromFhysicsObject(this)
            }
            return field
        }

    val acceleration: Vector2 = Vector2.ZERO
    open var static: Boolean = false
        set(value) {
            field = value
            if (value) {
                // Stop any movement if the object is set to static
                acceleration.set(Vector2.ZERO)
                velocity.set(Vector2.ZERO)
            }

            invMass = if (value) 0f else 1f / mass
        }

    var mass: Float = mass
        set(value) {
            field = value
            invMass = if (static) 0f else 1f / value
        }
    var invMass: Float = 1f / mass
        protected set

    private var lastUpdate = -1
    private var lastBBoxUpdate = -1

    var inertia: Float = -1f
        get() {
            if (field == -1f) {
                field = calculateInertia()
            }
            return field
        }
        private set

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

    abstract fun project(axis: Vector2): Projection

    abstract fun contains(pos: Vector2): Boolean

    abstract fun calculateInertia(): Float

    abstract fun testCollision(other: FhysicsObject): CollisionInfo

    abstract fun testCollision(other: Circle): CollisionInfo

    abstract fun testCollision(other: Polygon): CollisionInfo

    abstract fun findContactPoints(other: FhysicsObject, info: CollisionInfo): Array<Vector2>

    abstract fun findContactPoints(other: Circle, info: CollisionInfo): Array<Vector2>

    abstract fun findContactPoints(other: Polygon, info: CollisionInfo): Array<Vector2>

    abstract fun clone(): FhysicsObject

    private fun colorFromId(): Color {
        val colors: List<Color> =
            listOf(Color.decode("#32a852"), Color.decode("#4287f5"), Color.decode("#eb4034"), Color.decode("#fcba03"))
        return colors[id % colors.size]
//        return colors[index % colors.size]
//        val color = Color.getHSBColor(((id / 3.0f) / 255f) % 1f, 1f, 1f)
//
//        return color
    }

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
