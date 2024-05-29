package de.officeryoda.fhysics.extensions

import de.officeryoda.fhysics.engine.Vector2
import de.officeryoda.fhysics.objects.Circle
import de.officeryoda.fhysics.objects.FhysicsObject
import de.officeryoda.fhysics.objects.Rectangle
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
    // AABB check
    val radiusOffset = Vector2(circle.radius, circle.radius)
    return contains(circle.position - radiusOffset) && contains(circle.position + radiusOffset)
}

/**
 * Checks if the Rectangle contains the specified Rectangle
.
 *
 * @param rect The Rectangle
 to check for containment.
 * @return True if the Rectangle contains the Rectangle
, false otherwise.
 */
fun Rectangle2D.contains(rect: Rectangle): Boolean {
    // AABB check
    return contains(Vector2(rect.minX, rect.minY)) && contains(Vector2(rect.maxX, rect.maxY))
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
    // AABB check
    return this.intersects(
        circle.position.x.toDouble() - circle.radius.toDouble(),
        circle.position.y.toDouble() - circle.radius.toDouble(),
        2 * circle.radius.toDouble(),
        2 * circle.radius.toDouble()
    )
}

/**
 * Checks if the Rectangle intersects with the specified Rectangle
 *
 * @param rect The Rectangle to check for intersection.
 * @return True if the Rectangle intersects with the Rectangle, false otherwise.
 */
fun Rectangle2D.intersects(rect: Rectangle): Boolean {
    // AABB check
    return this.intersects(
        rect.minX.toDouble(),
        rect.minY.toDouble(),
        (rect.maxX - rect.minX).toDouble(),
        (rect.maxY - rect.minY).toDouble()
    )
}

