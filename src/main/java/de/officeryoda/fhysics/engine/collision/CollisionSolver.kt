package de.officeryoda.fhysics.engine.collision

import de.officeryoda.fhysics.engine.FhysicsCore
import de.officeryoda.fhysics.engine.Projection
import de.officeryoda.fhysics.engine.ProjectionResult
import de.officeryoda.fhysics.engine.Vector2
import de.officeryoda.fhysics.engine.objects.Circle
import de.officeryoda.fhysics.engine.objects.FhysicsObject
import de.officeryoda.fhysics.engine.objects.Polygon
import de.officeryoda.fhysics.extensions.times
import de.officeryoda.fhysics.rendering.DebugDrawer
import de.officeryoda.fhysics.rendering.UIController

object CollisionSolver {

    /**
     * Solves a collision between two objects in a perfectly elastic manner
     *
     * @param info The CollisionInfo object containing information about the collision
     */
    fun solveCollision(info: CollisionInfo, contactPoints: Array<Vector2>) {
        for (element: Vector2 in contactPoints) {
            DebugDrawer.addDebugPoint(element)
        }

        // separate the objects to prevent tunneling and other anomalies
        separateOverlappingObjects(info)

        // Get the objects
        val objA: FhysicsObject = info.objA!!
        val objB: FhysicsObject = info.objB!!

        // Calculate relative velocity before collision; circleB doesn't move relatively speaking
        val relativeVelocity: Vector2 = objB.velocity - objA.velocity

        // Return if the objects are already moving away from each other
        if (relativeVelocity.dot(info.normal) > 0) return

        val impulseMagnitude: Float = -2f * relativeVelocity.dot(info.normal) / (objA.invMass + objB.invMass)
        val impulse: Vector2 = impulseMagnitude * info.normal

        objA.velocity -= impulse * objA.invMass
        objB.velocity += impulse * objB.invMass
    }

    /**
     * Separates two overlapping objects
     *
     * @param info The CollisionInfo object containing information about the collision
     */
    fun separateOverlappingObjects(info: CollisionInfo) {
        val objA: FhysicsObject = info.objA!!
        val objB: FhysicsObject = info.objB!!

        if (objA.static && objB.static) return

        val overlap: Vector2 = info.depth * info.normal

        // if both objects are non-static, separate them by their mass ratio else move the non-static object by the overlap
        if (!objA.static) objA.position -= if (!objB.static) 0.5f * overlap else overlap
        if (!objB.static) objB.position += if (!objA.static) 0.5f * overlap else overlap
    }

    fun checkBorderCollision(obj: FhysicsObject) {
        when (obj) {
            is Circle -> handleCircleBorderCollision(obj)
            is Polygon -> handlePolygonBorderCollision(obj)
        }
    }

    private fun handleCircleBorderCollision(obj: Circle) {
        when {
            obj.position.x - obj.radius < 0.0F -> {
                obj.velocity.x = -obj.velocity.x * UIController.wallElasticity
                obj.position.x = obj.radius
            }

            obj.position.x + obj.radius > FhysicsCore.BORDER.width -> {
                obj.velocity.x = -obj.velocity.x * UIController.wallElasticity
                obj.position.x = (FhysicsCore.BORDER.width - obj.radius)
            }
        }

        when {
            obj.position.y - obj.radius < 0.0F -> {
                obj.velocity.y = -obj.velocity.y * UIController.wallElasticity
                obj.position.y = obj.radius
            }

            obj.position.y + obj.radius > FhysicsCore.BORDER.height -> {
                obj.velocity.y = -obj.velocity.y * UIController.wallElasticity
                obj.position.y = (FhysicsCore.BORDER.height - obj.radius)
            }
        }
    }

    private fun handlePolygonBorderCollision(obj: Polygon) {
        val axesBorderProjection: List<Pair<Vector2, Projection>> = listOf(
            Pair(Vector2(-1f, 0f), Projection(Float.MIN_VALUE, FhysicsCore.BORDER.x)),
            Pair(Vector2(1f, 0f), Projection(FhysicsCore.BORDER.x + FhysicsCore.BORDER.width, Float.MAX_VALUE)),
            Pair(Vector2(0f, -1f), Projection(Float.MIN_VALUE, FhysicsCore.BORDER.y)),
            Pair(Vector2(0f, 1f), Projection(FhysicsCore.BORDER.y + FhysicsCore.BORDER.height, Float.MAX_VALUE))
        )

        axesBorderProjection.forEach { (axis: Vector2, borderProj: Projection) ->
            val projection: Projection = obj.project(axis)
            val projResult = ProjectionResult(projection, borderProj)

            if (projResult.hasOverlap) {
                val overlap: Float = projResult.getOverlap()

                obj.position -= axis * overlap
                when {
                    axis.x != 0f -> obj.velocity.x = -obj.velocity.x * UIController.wallElasticity
                    axis.y != 0f -> obj.velocity.y = -obj.velocity.y * UIController.wallElasticity
                }
            }
        }
    }

}
