package de.officeryoda.fhysics.engine.collision

import de.officeryoda.fhysics.engine.Projection
import de.officeryoda.fhysics.engine.Vector2
import de.officeryoda.fhysics.engine.objects.FhysicsObject

data class BorderEdge(
    val normal: Vector2,
    val borderPosition: Float,
    val edgeCorner: Vector2, // The corner of the border from where the tangent of the normal points towards the center of the edge
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
