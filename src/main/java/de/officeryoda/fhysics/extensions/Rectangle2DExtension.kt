package de.officeryoda.fhysics.extensions

import de.officeryoda.fhysics.engine.Vector2
import de.officeryoda.fhysics.engine.Vector2Int
import de.officeryoda.fhysics.objects.Box
import de.officeryoda.fhysics.objects.Circle
import de.officeryoda.fhysics.objects.FhysicsObject
import java.awt.geom.Rectangle2D

/**
 * Checks if the Rectangle contains the specified Vector2.
 *
 * @param vector2 The Vector2 to check for containment.
 * @return True if the Rectangle contains the Vector2, false otherwise.
 */
fun Rectangle2D.contains(vector2: Vector2): Boolean {
    return contains(vector2.x, vector2.y)
}

/**
 * Checks if the Rectangle contains the specified Vector2Int.
 *
 * @param vector2Int The Vector2Int to check for containment.
 * @return True if the Rectangle contains the Vector2Int, false otherwise.
 */
fun Rectangle2D.contains(vector2Int: Vector2Int): Boolean {
    return contains(vector2Int.x.toDouble(), vector2Int.y.toDouble())
}

fun Rectangle2D.intersects(obj: FhysicsObject): Boolean {
    if (obj is Circle) {
        return intersects(obj)
    } else if (obj is Box) {
        return intersects(obj)
    }

    System.err.println("Unknown FhysicsObject: $obj")
    return false
}

fun Rectangle2D.intersects(circle: Circle): Boolean {
// Check if the rectangle intersects with the circle
    if (this.intersects(
            circle.position.x - circle.radius,
            circle.position.y - circle.radius,
            2 * circle.radius,
            2 * circle.radius
        )
    ) {
        // Bound the closest X/Y-coordinate within the horizontal/vertical range of the rectangle.
        val closestX = circle.position.x.coerceIn(this.minX, this.maxX)
        val closestY = circle.position.y.coerceIn(this.minY, this.maxY)
        val closestPos = Vector2(closestX, closestY)

        // Check if the distance is less than or equal to the radius of the circle
        val distanceSquared = circle.position.sqrDistance(closestPos)
        return distanceSquared <= circle.radius * circle.radius
    }

    return false
}

fun Rectangle2D.intersects(box: Box): Boolean {
    return this.intersects(
        box.position.x,
        box.position.y,
        box.width,
        box.height)
}

