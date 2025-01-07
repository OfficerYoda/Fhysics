package de.officeryoda.fhysics.engine.collision

import de.officeryoda.fhysics.engine.math.Projection
import de.officeryoda.fhysics.engine.math.Vector2
import de.officeryoda.fhysics.engine.objects.FhysicsObject

/**
 * Represents an edge of the simulation [border][de.officeryoda.fhysics.engine.FhysicsCore.BORDER].
 */
data class BorderEdge(
    /**
     * The normal vector of the border edge, pointing from the inside to the outside of the border.
     */
    val normal: Vector2,
    /**
     * The distance from the origin along the normal to the border.
     */
    val borderPosition: Float,
    /**
     * The corner of the border from which the left perpendicular vector of the normal points towards the center of the edge.
     */
    val edgeCorner: Vector2,
) {

    /**
     * Returns the [CollisionInfo] of a collision between the border and the given [object][obj].
     */
    fun testCollision(obj: FhysicsObject): CollisionInfo {
        val projection: Projection = obj.project(normal)
        if (projection.max <= borderPosition) return CollisionInfo()

        return CollisionInfo(obj, null /*border*/, normal, projection.max - borderPosition)
    }
}
