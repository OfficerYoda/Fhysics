package de.officeryoda.fhysics.objects

import de.officeryoda.fhysics.engine.Vector2
import de.officeryoda.fhysics.engine.collision.CollisionFinder
import de.officeryoda.fhysics.engine.collision.CollisionInfo
import java.awt.Color

class Rectangle(
    position: Vector2,
    val width: Float,
    val height: Float,
) : FhysicsObject(position, width * height) {

    val minX: Float
        get() = position.x

    val minY: Float
        get() = position.y

    val maxX: Float
        get() = position.x + width

    val maxY: Float
        get() = position.y + height

    init {
        color = Color.decode("#4287f5")
        static = true
    }

    override fun testCollision(other: Circle): CollisionInfo {
        return CollisionFinder.testCollision(other, this)
    }

    override fun testCollision(other: Rectangle): CollisionInfo {
        return CollisionFinder.testCollision(this, other)
    }

    override fun toString(): String {
        return "Box(id=$id, position=$position, velocity=$velocity, acceleration=$acceleration, mass=$mass, static=$static, color=$color, width=$width, height=$height)"
    }
}
