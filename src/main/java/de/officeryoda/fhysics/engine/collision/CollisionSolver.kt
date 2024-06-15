package de.officeryoda.fhysics.engine.collision

import de.officeryoda.fhysics.engine.FhysicsCore
import de.officeryoda.fhysics.engine.Projection
import de.officeryoda.fhysics.engine.ProjectionResult
import de.officeryoda.fhysics.engine.Vector2
import de.officeryoda.fhysics.engine.objects.Circle
import de.officeryoda.fhysics.engine.objects.FhysicsObject
import de.officeryoda.fhysics.engine.objects.Polygon
import de.officeryoda.fhysics.engine.objects.SubPolygon
import de.officeryoda.fhysics.extensions.times
import de.officeryoda.fhysics.rendering.DebugDrawer
import de.officeryoda.fhysics.rendering.UIController
import java.awt.Color

object CollisionSolver {

    /**
     * Solves the collision between two objects
     *
     * @param info The CollisionInfo object containing information about the collision
     * @param contactPoints The contact points of the collision
     */
    fun solveCollision(info: CollisionInfo, contactPoints: Array<Vector2>) {
        for (point in contactPoints) {
            DebugDrawer.addDebugPoint(point, Color.RED, 20)
        }

        val objA: FhysicsObject = info.objA!!
        val objB: FhysicsObject = info.objB!!

        if (objA is SubPolygon || objB is SubPolygon) {
            println("SubPolygon")
            throw Exception("SubPolygon")
        }

        // No need to solve collision if both objects are static
        if (objA.static && objB.static) return

        val e = 0.5f // Coefficient of restitution
        val impulseList: ArrayList<Vector2> = arrayListOf()

        // Calculate the impulses for each contact point
        for (contactPoint: Vector2 in contactPoints) {
            val ra: Vector2 = contactPoint - objA.position
            val rb: Vector2 = contactPoint - objB.position

            val raPerp = Vector2(-ra.y, ra.x)
            val rbPerp = Vector2(-rb.y, rb.x)

            val totalVelocityA: Vector2 = objA.velocity + raPerp * objA.angularVelocity
            val totalVelocityB: Vector2 = objB.velocity + rbPerp * objB.angularVelocity

            val relativeVelocity: Vector2 = totalVelocityB - totalVelocityA

            // Continue if the objects are already moving away from each other
            val contactVelocityMag: Float = relativeVelocity.dot(info.normal)
            if (contactVelocityMag > 0) continue

            // Calculate the impulse
            val raPerpDotNormal: Float = raPerp.dot(info.normal)
            val rbPerpDotNormal: Float = rbPerp.dot(info.normal)

            var impulseMag: Float = -(1f + e) * contactVelocityMag
            impulseMag /= objA.invMass + objB.invMass +
                    (raPerpDotNormal * raPerpDotNormal) * objA.invInertia +
                    (rbPerpDotNormal * rbPerpDotNormal) * objB.invInertia
            impulseMag /= contactPoints.size // Distribute the impulse over all contact points

            val impulse: Vector2 = impulseMag * info.normal
            impulseList.add(impulse)
        }

        // Apply the impulses
        for (i: Int in impulseList.indices) {
            val impulse: Vector2 = impulseList[i]

            objA.velocity += -impulse * objA.invMass
            objA.angularVelocity += impulse.cross(contactPoints[i] - objA.position) * objA.invInertia
            objB.velocity += impulse * objB.invMass
            objB.angularVelocity += -impulse.cross(contactPoints[i] - objB.position) * objB.invInertia
        }
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
        if (obj.static) return

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
