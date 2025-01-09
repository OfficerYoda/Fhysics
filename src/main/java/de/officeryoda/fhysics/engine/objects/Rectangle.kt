package de.officeryoda.fhysics.engine.objects

import de.officeryoda.fhysics.engine.collision.CollisionInfo
import de.officeryoda.fhysics.engine.math.Vector2
import de.officeryoda.fhysics.visual.Renderer
import kotlin.math.cos
import kotlin.math.sin

/**
 * You know what a rectangle is.
 */
class Rectangle(
    position: Vector2,
    val width: Float,
    val height: Float,
    angle: Float = 0f,
) : Polygon(createRectangleVertices(width, height).map { it + position }.toTypedArray(), angle) {

    override val type = FhysicsObjectType.RECTANGLE

    override fun getAxes(): List<Vector2> {
        // Calculate the normals of the rectangle's sides based on its rotation
        val sin: Float = sin(angle)
        val cos: Float = cos(angle)

        val axis1 = Vector2(cos, sin)
        val axis2 = Vector2(-sin, cos)

        return listOf(axis1, axis2)
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
        return mass / 12f * (width * width + height * height)
    }

    override fun testCollision(other: FhysicsObject): CollisionInfo {
        return other.testCollision(this) // works because FhysicsObject is abstract (aka double dispatch)
    }

    override fun findContactPoints(other: FhysicsObject, info: CollisionInfo): Array<Vector2> {
        return other.findContactPoints(this, info)
    }

    override fun draw(renderer: Renderer) {
        renderer.drawRectangle(this)
    }

    override fun toString(): String {
        return "Rectangle(id=$id, position=$position, velocity=$velocity, mass=$mass, angle=$angle, angularVelocity=$angularVelocity, inertia=$inertia,  static=$static, color=$color, width=$width, height=$height, rotation=$angle)"
    }

    companion object {
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
    }
}

