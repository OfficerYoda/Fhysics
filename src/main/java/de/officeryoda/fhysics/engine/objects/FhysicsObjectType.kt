package de.officeryoda.fhysics.engine.objects

enum class FhysicsObjectType(val value: Int) {
    CIRCLE(0),
    RECTANGLE(1),
    CONVEX_POLYGON(2),
    CONCAVE_POLYGON(3),
    SUB_POLYGON(4),
}