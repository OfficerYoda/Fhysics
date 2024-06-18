package de.officeryoda.fhysics.engine.collision

import de.officeryoda.fhysics.engine.FhysicsCore.BORDER
import de.officeryoda.fhysics.engine.Vector2
import de.officeryoda.fhysics.engine.objects.*
import de.officeryoda.fhysics.extensions.times
import de.officeryoda.fhysics.rendering.DebugDrawer
import de.officeryoda.fhysics.rendering.UIController.Companion.wallElasticity
import java.awt.Color

object CollisionSolver {

    private val borderObjects: Array<BorderObject>

    init {
        // Create the border objects
        val borderObjectList: MutableList<BorderObject> = mutableListOf()
        val bigNumber = 1E6f

        // Right edge
        var vertices: Array<Vector2> = arrayOf(
            Vector2(BORDER.x + BORDER.width, bigNumber),
            Vector2(BORDER.x + BORDER.width, -bigNumber),
            Vector2(bigNumber, -bigNumber),
            Vector2(bigNumber, bigNumber)
        )
        borderObjectList.add(BorderObject(Vector2(1f, 0f), vertices))

        // Left edge
        vertices = arrayOf(
            Vector2(-bigNumber, bigNumber),
            Vector2(-bigNumber, -bigNumber),
            Vector2(BORDER.x, -bigNumber),
            Vector2(BORDER.x, bigNumber)
        )
        borderObjectList.add(BorderObject(Vector2(-1f, 0f), vertices))

        // Top edge
        vertices = arrayOf(
            Vector2(-bigNumber, bigNumber),
            Vector2(-bigNumber, BORDER.y + BORDER.height),
            Vector2(bigNumber, BORDER.y + BORDER.height),
            Vector2(bigNumber, bigNumber)
        )
        borderObjectList.add(BorderObject(Vector2(0f, 1f), vertices))

        // Bottom edge
        vertices = arrayOf(
            Vector2(-bigNumber, BORDER.y),
            Vector2(-bigNumber, -bigNumber),
            Vector2(bigNumber, -bigNumber),
            Vector2(bigNumber, BORDER.y)
        )
        borderObjectList.add(BorderObject(Vector2(0f, -1f), vertices))

        borderObjects = borderObjectList.toTypedArray()
    }

    /**
     * Solves the collision between two objects
     *
     * @param info The CollisionInfo object containing information about the collision
     * @param contactPoints The contact points of the collision
     */
    fun solveCollision(info: CollisionInfo, contactPoints: Array<Vector2>) {
        for (point: Vector2 in contactPoints) {
            DebugDrawer.addDebugPoint(point, Color.RED, 1)
        }

        val objA: FhysicsObject = info.objA!!
        val objB: FhysicsObject = info.objB!!

        // No need to solve collision if both objects are static
        if (objA.static && objB.static) return

        val e = 0.5f // Coefficient of restitution
        val impulseList: ArrayList<Vector2> = arrayListOf()

        // Calculate the impulses for each contact point
        for (contactPoint: Vector2 in contactPoints) {
//            if (objA is ConcavePolygon) DebugDrawer.addDebugVector(objA.position, objA.velocity, Color.PINK, 1)
//            if (objB is ConcavePolygon) DebugDrawer.addDebugVector(objB.position, objB.velocity, Color.PINK, 1)

            val ra: Vector2 = contactPoint - objA.position
            val rb: Vector2 = contactPoint - objB.position

            val raPerp = Vector2(-ra.y, ra.x)
            val rbPerp = Vector2(-rb.y, rb.x)

            if (objA is ConcavePolygon) DebugDrawer.addDebugVector(contactPoint, raPerp, Color.BLUE, 1)
            if (objB is ConcavePolygon) DebugDrawer.addDebugVector(contactPoint, rbPerp, Color.BLUE, 1)

            val totalVelocityA: Vector2 = objA.velocity + raPerp * objA.angularVelocity
            val totalVelocityB: Vector2 = objB.velocity + rbPerp * objB.angularVelocity

            if (objA is ConcavePolygon) DebugDrawer.addDebugVector(contactPoint, totalVelocityA, Color.ORANGE, 1)
            if (objB is ConcavePolygon) DebugDrawer.addDebugVector(contactPoint, totalVelocityB, Color.ORANGE, 1)

            val relativeVelocity: Vector2 = totalVelocityB - totalVelocityA

            DebugDrawer.addDebugVector(contactPoint, relativeVelocity, Color.MAGENTA, 1)

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

            if (objA is ConcavePolygon) DebugDrawer.addDebugVector(
                contactPoints[i],
                -impulse * objA.invMass,
                Color.GREEN,
                1
            )
            if (objB is ConcavePolygon) DebugDrawer.addDebugVector(
                contactPoints[i],
                impulse * objB.invMass,
                Color.GREEN,
                1
            )

            if (objA is ConcavePolygon) DebugDrawer.addDebugVector(
                contactPoints[i],
                impulse.cross(contactPoints[i] - objA.position) * objA.invInertia * Vector2(-1f, 1f),
                Color.CYAN,
                1
            )
            if (objB is ConcavePolygon) DebugDrawer.addDebugVector(
                contactPoints[i],
                -impulse.cross(contactPoints[i] - objB.position) * objB.invInertia * Vector2(-1f, 1f),
                Color.CYAN,
                1
            )
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
                obj.velocity.x = -obj.velocity.x * wallElasticity
                obj.position.x = obj.radius
            }

            obj.position.x + obj.radius > BORDER.width -> {
                obj.velocity.x = -obj.velocity.x * wallElasticity
                obj.position.x = (BORDER.width - obj.radius)
            }
        }

        when {
            obj.position.y - obj.radius < 0.0F -> {
                obj.velocity.y = -obj.velocity.y * wallElasticity
                obj.position.y = obj.radius
            }

            obj.position.y + obj.radius > BORDER.height -> {
                obj.velocity.y = -obj.velocity.y * wallElasticity
                obj.position.y = (BORDER.height - obj.radius)
            }
        }
    }

    private fun handlePolygonBorderCollision(obj: Polygon) {
        for (border: BorderObject in borderObjects) {
            val info: CollisionInfo = border.testCollision(obj)
            if (info.hasCollision) {
                separateOverlappingObjects(info)
                val contactPoints: Array<Vector2> = border.findContactPoints(obj, info)
                solveCollision(info, contactPoints)
            }
        }
    }
}

