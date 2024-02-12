package de.officeryoda.fhysics.extensions

import de.officeryoda.fhysics.engine.Vector2
import de.officeryoda.fhysics.engine.Vector2Int
import java.awt.Rectangle
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
