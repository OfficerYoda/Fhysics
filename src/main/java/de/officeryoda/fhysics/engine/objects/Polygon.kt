package de.officeryoda.fhysics.engine.objects

import de.officeryoda.fhysics.engine.collision.BorderEdge
import de.officeryoda.fhysics.engine.collision.CollisionFinder
import de.officeryoda.fhysics.engine.collision.CollisionInfo
import de.officeryoda.fhysics.engine.collision.ContactFinder
import de.officeryoda.fhysics.engine.math.Projection
import de.officeryoda.fhysics.engine.math.TransformationData
import de.officeryoda.fhysics.engine.math.Vector2
import de.officeryoda.fhysics.visual.Renderer
import kotlin.math.abs

/**
 * A Polygon whose [position] is at the average position of the [vertices].
 */
// Primary constructor is used to sync position and velocity from sub-polygons with the main polygon
abstract class Polygon(
    position: Vector2,
    velocity: Vector2,
    /** The vertices of the polygon in local space */
    val vertices: Array<Vector2>,
    angle: Float,
    angularVelocity: Float,
) : FhysicsObject(position, velocity, calculatePolygonArea(vertices), angle, angularVelocity) {

    // Used for creating every polygon except sub-polygons
    constructor(vertices: Array<Vector2>, angle: Float) : this(
        calculatePolygonCenter(vertices),
        Vector2.ZERO, vertices, angle, 0f
    )

    init {
        // Convert vertices to local space
        for (it: Vector2 in vertices) {
            it -= position
        }
    }

    open fun getAxes(): List<Vector2> {
        val transformedVertices: Array<Vector2> = getTransformedVertices()
        val axes: MutableList<Vector2> = ArrayList(transformedVertices.size)

        // Calculate the normals of the polygon's sides based on its rotation
        for (i: Int in transformedVertices.indices) {
            val j: Int = (i + 1) % transformedVertices.size
            val edge: Vector2 = transformedVertices[j] - transformedVertices[i]
            val normal: Vector2 = Vector2(-edge.y, edge.x).normalized()
            axes.add(normal)
        }

        return axes
    }

    override fun project(axis: Vector2): Projection {
        val transformedVertices: Array<Vector2> = getTransformedVertices()

        // Project the polygon's vertices onto the axis
        var min: Float = Float.POSITIVE_INFINITY
        var max: Float = Float.NEGATIVE_INFINITY
        for (vertex: Vector2 in transformedVertices) {
            val projection: Float = vertex.dot(axis)
            if (projection < min) min = projection
            if (projection > max) max = projection
        }

        return Projection(min, max)
    }

    override fun contains(pos: Vector2): Boolean {
        if (!boundingBox.contains(pos)) return false

        val transformedVertices: Array<Vector2> = getTransformedVertices()
        var intersects = 0

        for (i: Int in transformedVertices.indices) {
            val j: Int = (i + 1) % transformedVertices.size
            val xi: Float = transformedVertices[i].x
            val yi: Float = transformedVertices[i].y
            val xj: Float = transformedVertices[j].x
            val yj: Float = transformedVertices[j].y

            val isIntersect: Boolean =
                ((yi > pos.y) != (yj > pos.y)) && (pos.x < (xj - xi) * (pos.y - yi) / (yj - yi) + xi)
            if (isIntersect) intersects++
        }

        return intersects and 1 == 1
    }

    override fun calculateInertia(): Float {
        var numerator = 0.0f
        var denominator = 0.0f
        val n: Int = vertices.size

        for (i: Int in 0 until n) {
            val current: Vector2 = vertices[i]
            val next: Vector2 = vertices[(i + 1) % n]

            val crossProduct: Float = current.cross(next)
            val distanceTerm: Float = current.dot(current) + current.dot(next) + next.dot(next)

            numerator += crossProduct * distanceTerm
            denominator += crossProduct
        }

        return (mass * numerator) / (6 * denominator)
    }

    /**
     * Returns an array of vertices transformed from local space to global space.
     */
    fun getTransformedVertices(): Array<Vector2> {
        // Create the transformation data
        val transformationData = TransformationData(angle, super.position)

        // Transform the vertices
        return Array(vertices.size) { i -> transformationData.applyTo(vertices[i]) }
    }

    abstract override fun testCollision(other: FhysicsObject): CollisionInfo

    override fun testCollision(other: Circle): CollisionInfo {
        return CollisionFinder.testCollision(this, other)
    }

    override fun testCollision(other: Polygon): CollisionInfo {
        return CollisionFinder.testCollision(this, other)
    }

    override fun findContactPoints(border: BorderEdge): Array<Vector2> {
        return ContactFinder.findContactPoints(border, this)
    }

    abstract override fun findContactPoints(other: FhysicsObject, info: CollisionInfo): Array<Vector2>

    override fun findContactPoints(other: Circle, info: CollisionInfo): Array<Vector2> {
        return ContactFinder.findContactPoints(other, info)
    }

    override fun findContactPoints(other: Polygon, info: CollisionInfo): Array<Vector2> {
        return ContactFinder.findContactPoints(this, other)
    }

    override fun updateBoundingBox() {
        boundingBox.setFromPolygon(this)
    }

    override fun draw(renderer: Renderer) {
        renderer.drawPolygon(this)
    }

    override fun toString(): String {
        return "Polygon(id=$id, position=$position, velocity=$velocity, mass=$mass, angle=$angle, angularVelocity=$angularVelocity, inertia=$inertia, static=$static, color=$color, vertices=$vertices)"
    }

    companion object {
        /**
         * Calculates the area of a polygon with the given [vertices].
         */
        private fun calculatePolygonArea(vertices: Array<Vector2>): Float {
            var signedArea = 0f
            for (i: Int in vertices.indices) {
                val j: Int = (i + 1) % vertices.size
                signedArea += vertices[i].x * vertices[j].y
                signedArea -= vertices[j].x * vertices[i].y
            }
            signedArea /= 2

            // Ensure that the area is positive
            return abs(signedArea)
        }

        /**
         * Calculates the center of a polygon with the given [vertices].
         */
        @JvmStatic
        protected fun calculatePolygonCenter(vertices: Array<Vector2>): Vector2 {
            return vertices.reduce { acc, vector2 -> acc + vector2 } / vertices.size.toFloat()
        }
    }
}