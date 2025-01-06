package de.officeryoda.fhysics.engine.objects

import de.officeryoda.fhysics.engine.FhysicsCore
import de.officeryoda.fhysics.engine.FhysicsCore.dt
import de.officeryoda.fhysics.engine.collision.BorderEdge
import de.officeryoda.fhysics.engine.collision.CollisionInfo
import de.officeryoda.fhysics.engine.datastructures.spatial.BoundingBox
import de.officeryoda.fhysics.engine.math.Projection
import de.officeryoda.fhysics.engine.math.Vector2
import de.officeryoda.fhysics.rendering.FhysicsObjectDrawer
import de.officeryoda.fhysics.rendering.UIController.Companion.damping
import java.awt.Color

abstract class FhysicsObject protected constructor(
    open val position: Vector2,
    val velocity: Vector2 = Vector2.ZERO,
    mass: Float,
    /** In radians */
    open var angle: Float = 0f,
    /** In radians per second */
    var angularVelocity: Float = 0f,
) {
    /** Unique id */
    val id: Int = nextId
    abstract val type: FhysicsObjectType
    var color: Color = colorFromId()
    val boundingBox: BoundingBox = BoundingBox()

    val acceleration: Vector2 = Vector2.ZERO

    open var static: Boolean = false
        set(value) {
            field = value
            if (value) {
                invMass = 0f
                invInertia = 0f
            } else {
                invMass = 1f / mass
                invInertia = 1f / inertia
            }

            // Stop any movement if the object is set to static
            acceleration.set(Vector2.ZERO)
            velocity.set(Vector2.ZERO)
            angularVelocity = 0f
        }

    var mass: Float = mass
        set(value) {
            field = value
            invMass = if (static) 0f else 1f / value
            inertia = calculateInertia()
            invInertia = if (static) 0f else 1f / inertia
        }

    /** Inverse mass. 0 if the object static */
    var invMass: Float = 1f / mass
        private set

    /**
     * Last frame this object was updated.
     * This is used to prevent multiple updates in the same
     * frame due to multiple references in the quadtree
     */
    private var lastUpdate = -1

    var inertia: Float = -1f
        get() {
            if (field == -1f) {
                field = calculateInertia()
            }

            invInertia = if (static) 0f else 1f / field
            return field
        }

    /** Inverse inertia. 0 if the object static */
    var invInertia: Float = -1f
        get() {
            if (field == -1f) {
                field = if (static) 0f else 1f / inertia
            }
            return field
        }

    /**
     * Coefficient of restitution.
     *
     * The bounciness of the object.
     */
    var restitution: Float = 0.5f
        set(value) {
            field = value.coerceIn(0f, 1f)
        }

    var frictionStatic: Float = 0.5f
        set(value) {
            // Can be over one in real life, but it's very rare (rubber on dry concrete: ~1.0)
            field = value.coerceIn(0f, 1f)
        }

    var frictionDynamic: Float = 0.45f
        set(value) {
            // Can be over one in real life, but it's very rare (rubber on dry concrete: ~0.8)
            field = value.coerceIn(0f, 1f)
        }

    private val twoPi: Float = Math.PI.toFloat() * 2

    fun update() {
        // Static objects don't move
        if (static) return
        // Needed because multiple quadtree nodes can contain the same object
        if (lastUpdate == FhysicsCore.updateCount) return
        lastUpdate = FhysicsCore.updateCount

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
        // Normalize angle for better precision
        angle %= twoPi

        updateBoundingBox()
    }

    /**
     * Projects this object onto the given axis.
     */
    abstract fun project(axis: Vector2): Projection

    /**
     * Checks if this object contains the given [position][pos].
     */
    abstract fun contains(pos: Vector2): Boolean

    /**
     * Calculates the inertia of this object.
     */
    abstract fun calculateInertia(): Float

    /**
     * Tests if this object collides with the given [border].
     */
    fun testCollision(border: BorderEdge): CollisionInfo {
        return border.testCollision(this)
    }

    /**
     * Tests if this object collides with the given [object][other].
     */
    abstract fun testCollision(other: FhysicsObject): CollisionInfo

    /**
     * Tests if this object collides with the given [circle][other].
     */
    abstract fun testCollision(other: Circle): CollisionInfo

    /**
     * Tests if this object collides with the given [polygon][other].
     */
    abstract fun testCollision(other: Polygon): CollisionInfo

    /**
     * Finds the contact points between this object and the given [border].
     */
    abstract fun findContactPoints(border: BorderEdge): Array<Vector2>

    /**
     * Finds the contact points between this object and the given [object][other].
     */
    abstract fun findContactPoints(other: FhysicsObject, info: CollisionInfo): Array<Vector2>

    /**
     * Finds the contact points between this object and the given [circle][other].
     */
    abstract fun findContactPoints(other: Circle, info: CollisionInfo): Array<Vector2>

    /**
     * Finds the contact points between this object and the given [polygon][other].
     */
    abstract fun findContactPoints(other: Polygon, info: CollisionInfo): Array<Vector2>

    abstract fun updateBoundingBox()

    /**
     * Draws this object.
     */
    abstract fun draw(drawer: FhysicsObjectDrawer)

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

    companion object {
        /**
         * The next free id.
         *
         * Getting the next id will increment its value.
         */
        private var nextId = 0
            get() = field++
    }
}
