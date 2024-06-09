package de.officeryoda.fhysics.engine.objects

import de.officeryoda.fhysics.engine.Vector2
import de.officeryoda.fhysics.engine.collision.CollisionInfo

class ConcavePolygon(
    vertices: Array<Vector2>,
    subPolygonIndices: Array<Array<Int>>, // the indices of the vertices that form the convex sub-polygons
    rotation: Float = 0f,
) : Polygon(vertices, rotation) {

    var subPolygons: MutableList<SubPolygon> = mutableListOf()
    override var static: Boolean
        get() = super.static
        set(value) {
            super.static = value
            subPolygons.forEach { it.static = value }
        }

    init {
        subPolygonIndices.forEach { indices ->
            val subVertices: Array<Vector2> = indices.map { vertices[it] + position }.toTypedArray()
            val center = calculatePolygonCenter(subVertices)
            subPolygons.add(SubPolygon(position, center, velocity, subVertices, rotation))
        }
    }

    override fun testCollision(other: FhysicsObject): CollisionInfo {
        return other.testCollision(this)
    }

    override fun clone(): FhysicsObject {
        TODO("Not yet implemented")
    }
}

class SubPolygon(
    position: Vector2,
    center: Vector2,
    velocity: Vector2,
    vertices: Array<Vector2>,
    rotation: Float = 0f,
) : Polygon(position, velocity, vertices, rotation) {

    private val centerOffset = center - position
    override val center
        get() = position + centerOffset


    override fun testCollision(other: FhysicsObject): CollisionInfo {
        return other.testCollision(this)
    }

    override fun clone(): FhysicsObject {
        TODO()
//        return SubPolygon(vertices.map { it.copy() }.toTypedArray(), indices, position.copy(), velocity.copy(), rotation)
    }

    override fun toString(): String {
        return "SubPolygon(id=$id, position=$position, velocity=$velocity, acceleration=$acceleration, mass=$mass, static=$static, color=$color, vertices=${vertices.contentToString()})"
    }
}