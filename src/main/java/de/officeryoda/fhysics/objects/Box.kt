package de.officeryoda.fhysics.objects

import de.officeryoda.fhysics.engine.Vector2
import de.officeryoda.fhysics.engine.collision.CollisionFinder
import de.officeryoda.fhysics.engine.collision.CollisionInfo
import java.awt.Color

class Box(
    position: Vector2,
    val width: Double,
    val height: Double,
) : FhysicsObject(position, width * height) {

    val minX: Double
        get() = position.x

    val minY: Double
        get() = position.y

    val maxX: Double
        get() = position.x + width

    val maxY: Double
        get() = position.y + height

    init {
        color = Color.decode("#4287f5")
        static = true
    }

    override fun testCollision(other: Circle): CollisionInfo {
        return CollisionFinder.testCollision(other, this)
    }

    override fun testCollision(other: Box): CollisionInfo {
        return CollisionFinder.testCollision(this, other)
    }

    override fun toString(): String {
        return "Box(id=$id, position=$position, velocity=$velocity, acceleration=$acceleration, mass=$mass, static=$static, color=$color, width=$width, height=$height)"
    }
}
