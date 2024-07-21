package de.officeryoda.fhysics.engine.objects

import de.officeryoda.fhysics.engine.BoundingBox
import de.officeryoda.fhysics.engine.FhysicsCore
import de.officeryoda.fhysics.engine.FhysicsCore.dt
import de.officeryoda.fhysics.engine.Projection
import de.officeryoda.fhysics.engine.Vector2
import de.officeryoda.fhysics.engine.collision.BorderEdge
import de.officeryoda.fhysics.engine.collision.CollisionInfo
import java.awt.Color

abstract class FhysicsObject protected constructor(
    open val position: Vector2,
    val velocity: Vector2 = Vector2.ZERO,
    mass: Float,
    open var angle: Float = 0f, // In radians
    var angularVelocity: Float = 0f, // In radians per second
) {

    val id: Int = FhysicsCore.nextId()
    var color: Color = colorFromId()
    val boundingBox: BoundingBox = BoundingBox()

    val acceleration: Vector2 = Vector2.ZERO
    open var static: Boolean = false
        set(value) {
            field = value
            if (value) {
                // Stop any movement if the object is set to static
                acceleration.set(Vector2.ZERO)
                velocity.set(Vector2.ZERO)
                angularVelocity = 0f
                invMass = 0f
                invInertia = 0f
            } else {
                invMass = 1f / mass
                invInertia = 1f / inertia
            }

            boundingBox.setFromFhysicsObject(this)
        }

    var mass: Float = mass
        set(value) {
            field = value
            invMass = if (static) 0f else 1f / value
            inertia = calculateInertia()
            invInertia = if (static) 0f else 1f / inertia
        }
    var invMass: Float = 1f / mass
        protected set

    private var lastUpdate = -1

    var inertia: Float = -1f
        get() {
            if (field == -1f) {
                field = calculateInertia()
            }

            invInertia = if (static) 0f else 1f / field
            return field
        }
    var invInertia: Float = -1f
        get() {
            if (field == -1f) {
                field = if (static) 0f else 1f / inertia
            }
            return field
        }

    var restitution: Float = 0.5f
        set(value) {
            field = Math.clamp(value, 0f, 1f)
        }

    var frictionStatic: Float = 0.5f
        set(value) {
            // Can be over one in real life, but it's very rare (rubber on dry concrete: ~1.0)
            field = Math.clamp(value, 0f, 1f)
        }
    var frictionDynamic: Float = 0.35f
        set(value) {
            // Can be over one in real life, but it's very rare (rubber on dry concrete: ~0.8)
            field = Math.clamp(value, 0f, 1f)
        }

    open fun update() {
        // Static objects don't move
        if (static) return
        // Needed because multiple quadtree nodes can contain the same object
        if (lastUpdate == FhysicsCore.updateCount) return
        lastUpdate = FhysicsCore.updateCount

        val damping = 0.0f

        // Update Position
        acceleration += FhysicsCore.gravityAt(position)
        // Update velocity before position (semi-implicit Euler)
        velocity += acceleration * dt
        velocity *= (1 - damping)
        position += velocity * dt
        acceleration.set(Vector2.ZERO)

        // Update rotation
        angularVelocity *= (1 - damping)
        angle += angularVelocity * dt

        // Update bounding box
        boundingBox.setFromFhysicsObject(this)
    }

    abstract fun project(axis: Vector2): Projection

    abstract fun contains(pos: Vector2): Boolean

    abstract fun calculateInertia(): Float

    fun testCollision(border: BorderEdge): CollisionInfo {
        return border.testCollision(this)
    }

    abstract fun testCollision(other: FhysicsObject): CollisionInfo

    abstract fun testCollision(other: Circle): CollisionInfo

    abstract fun testCollision(other: Polygon): CollisionInfo

    abstract fun findContactPoints(other: BorderEdge): Array<Vector2>

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
        return "FhysicsObject(id=$id, position=$position, velocity=$velocity, mass=$mass, angle=$angle, angularVelocity=$angularVelocity, inertia=$inertia, static=$static, color=$color)"
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
