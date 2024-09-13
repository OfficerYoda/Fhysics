package de.officeryoda.fhysics.engine.collision

import de.officeryoda.fhysics.engine.math.Projection
import de.officeryoda.fhysics.engine.math.Vector2
import de.officeryoda.fhysics.engine.objects.FhysicsObject

data class BorderEdge(
    /**
     * The normal vector of the border edge
     * Points from the inside to the outside of the border
     */
    val normal: Vector2,
    /**
     * The distance to move from the origin in direction of the normal to reach the border
     */
    val borderPosition: Float,
    /**
     * The corner of the border from where the left perpendicular vector of the normal points towards the center of the edge
     */
    val edgeCorner: Vector2,
) {

    /**
     * Tests for a collision between the border and the given physics object.
     *
     * @param obj The physics object to test for collision.
     * @return A CollisionInfo object containing information about the collision.
     */
    fun testCollision(obj: FhysicsObject): CollisionInfo {
        val projection: Projection = obj.project(normal)
        if (projection.max <= borderPosition) return CollisionInfo()

        return CollisionInfo(obj, null /*border*/, normal, projection.max - borderPosition)
    }
}
