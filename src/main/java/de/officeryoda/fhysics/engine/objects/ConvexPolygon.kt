package de.officeryoda.fhysics.engine.objects

import de.officeryoda.fhysics.engine.math.Vector2

/**
 * A ConvexPolygon is a polygon where all interior angles are less than 180 degrees.
 */
open class ConvexPolygon(
    position: Vector2,
    velocity: Vector2,
    vertices: Array<Vector2>,
    angle: Float,
    angularVelocity: Float,
) : Polygon(position, velocity, vertices, angle, angularVelocity) {

    override val type = FhysicsObjectType.CONVEX_POLYGON

    // Used for creating every polygon except sub-polygons
    constructor(vertices: Array<Vector2>, angle: Float) : this(
        calculatePolygonCenter(vertices),
        Vector2.ZERO, vertices, angle, 0f
    )

    override fun toString(): String {
        return "ConvexPolygon(id=$id, position=$position, velocity=$velocity, mass=$mass, angle=$angle, angularVelocity=$angularVelocity, inertia=$inertia, static=$static, color=$color, vertices=${vertices.contentToString()})"
    }
}