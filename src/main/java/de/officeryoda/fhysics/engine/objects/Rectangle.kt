package de.officeryoda.fhysics.engine.objects

import de.officeryoda.fhysics.engine.collision.CollisionInfo
import de.officeryoda.fhysics.engine.math.Vector2
import de.officeryoda.fhysics.rendering.FhysicsObjectDrawer
import kotlin.math.cos
import kotlin.math.sin

class Rectangle(
    position: Vector2,
    val width: Float,
    val height: Float,
    angle: Float = 0f,
) : Polygon(createRectangleVertices(width, height).map { it + position }.toTypedArray(), angle) {

    override fun getAxes(): Set<Vector2> {
        // Calculate the normals of the rectangle's sides based on its rotation
        val sin: Float = sin(angle)
        val cos: Float = cos(angle)
        val axis1 = Vector2(cos, sin)
        val axis2 = Vector2(-sin, cos)
        return setOf(axis1, axis2)
    }

    override fun contains(pos: Vector2): Boolean {
        // Rotate the point to make the rectangle axis-aligned
        val rotatedPos: Vector2 = pos.rotatedAround(-angle, position)

        val halfWidth: Float = width / 2f
        val halfHeight: Float = height / 2f

        // Check if the point is inside the axis-aligned rectangle
        return rotatedPos.x in (position.x - halfWidth)..(position.x + halfWidth) &&
                rotatedPos.y in (position.y - halfHeight)..(position.y + halfHeight)
    }

    override fun calculateInertia(): Float {
        return (mass * (width * width + height * height)) / 12f
    }

    override fun testCollision(other: FhysicsObject): CollisionInfo {
        return other.testCollision(this) // works because FhysicsObject is abstract (aka double dispatch)
    }

    override fun findContactPoints(other: FhysicsObject, info: CollisionInfo): Array<Vector2> {
        return other.findContactPoints(this, info)
    }

    override fun draw(drawer: FhysicsObjectDrawer) {
        drawer.drawRectangle(this)
    }

    override fun toString(): String {
        return "Rectangle(id=$id, position=$position, velocity=$velocity, mass=$mass, angle=$angle, angularVelocity=$angularVelocity, inertia=$inertia,  static=$static, color=$color, width=$width, height=$height, rotation=$angle)"
    }
}

/**
 * Creates the vertices of a rectangle with the given [width] and [height].
 */
private fun createRectangleVertices(width: Float, height: Float): Array<Vector2> {
    val halfWidth: Float = width / 2
    val halfHeight: Float = height / 2

    return arrayOf(
        Vector2(-halfWidth, -halfHeight),
        Vector2(halfWidth, -halfHeight),
        Vector2(halfWidth, halfHeight),
        Vector2(-halfWidth, halfHeight)
    )
}
