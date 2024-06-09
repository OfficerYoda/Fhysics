package de.officeryoda.fhysics.engine.objects

import de.officeryoda.fhysics.engine.Vector2
import de.officeryoda.fhysics.engine.collision.CollisionInfo

class ConcavePolygon(
    vertices: Array<Vector2>,
    subPolygonIndices: Array<Array<Int>>, // the indices of the vertices that form the convex sub-polygons
    rotation: Float = 0f,
) : Polygon(vertices, rotation) {
    override fun testCollision(other: FhysicsObject): CollisionInfo {
        return other.testCollision(this)
    }

    override fun clone(): FhysicsObject {
        TODO("Not yet implemented")
    }
}