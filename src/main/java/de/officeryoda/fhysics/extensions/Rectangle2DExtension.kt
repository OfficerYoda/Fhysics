package de.officeryoda.fhysics.extensions

import de.officeryoda.fhysics.engine.Vector2
import de.officeryoda.fhysics.objects.Rectangle
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
    return contains(vector2.x.toDouble(), vector2.y.toDouble())
}

/**
 * Checks if the Rectangle contains the specified FhysicsObject.
 *
 * @param obj The FhysicsObject to check for containment.
 * @return True if the Rectangle contains the FhysicsObject, false otherwise.
 */
fun Rectangle2D.contains(obj: FhysicsObject): Boolean {
    if (obj is Circle) {
        return contains(obj)
    } else if (obj is Rectangle) {
        return contains(obj)
    }

    System.err.println("Unknown FhysicsObject: $obj")
    return false
}

/**
 * Checks if the Rectangle contains the specified Circle.
 *
 * @param circle The Circle to check for containment.
 * @return True if the Rectangle contains the Circle, false otherwise.
 */
fun Rectangle2D.contains(circle: Circle): Boolean {
    // basically an AABB check
    val radiusOffset = Vector2(circle.radius, circle.radius)
    return contains(circle.position - radiusOffset) && contains(circle.position + radiusOffset)
}

/**
 * Checks if the Rectangle contains the specified Box.
 *
 * @param rect The Box to check for containment.
 * @return True if the Rectangle contains the Box, false otherwise.
 */
fun Rectangle2D.contains(rect: Rectangle): Boolean {
    // an AABB check
    return contains(rect.position) && contains(rect.position + Vector2(rect.width, rect.height))
}

/**
 * Checks if the Rectangle intersects with the specified FhysicsObject.
 *
 * @param obj The FhysicsObject to check for intersection.
 * @return True if the Rectangle intersects with the FhysicsObject, false otherwise.
 */
fun Rectangle2D.intersects(obj: FhysicsObject): Boolean {
    if (obj is Circle) {
        return intersects(obj)
    } else if (obj is Rectangle) {
        return intersects(obj)
    }

    System.err.println("Unknown FhysicsObject: $obj")
    return false
}

/**
 * Checks if the Rectangle intersects with the specified Circle.
 *
 * @param circle The Circle to check for intersection.
 * @return True if the Rectangle intersects with the Circle, false otherwise.
 */
fun Rectangle2D.intersects(circle: Circle): Boolean {
// Check if the rectangle intersects with the circle
    if (this.intersects(
            circle.position.x.toDouble() - circle.radius.toDouble(),
            circle.position.y.toDouble() - circle.radius.toDouble(),
            2 * circle.radius.toDouble(),
            2 * circle.radius.toDouble()
        )
    ) {
        // Bound the closest X/Y-coordinate within the horizontal/vertical range of the rectangle.
        val closestX = circle.position.x.coerceIn(this.minX.toFloat(), this.maxX.toFloat())
        val closestY = circle.position.y.coerceIn(this.minY.toFloat(), this.maxY.toFloat())
        val closestPos = Vector2(closestX, closestY)

        // Check if the distance is less than or equal to the radius of the circle
        val distanceSquared = circle.position.sqrDistance(closestPos)
        return distanceSquared <= circle.radius * circle.radius
    }

    return false
}

/**
 * Checks if the Rectangle intersects with the specified Box.
 *
 * @param rect The Box to check for intersection.
 * @return True if the Rectangle intersects with the Box, false otherwise.
 */
fun Rectangle2D.intersects(rect: Rectangle): Boolean {
    return this.intersects(
        rect.position.x.toDouble(),
        rect.position.y.toDouble(),
        rect.width.toDouble(),
        rect.height.toDouble())
}

