package de.officeryoda.fhysics.engine.collision

import de.officeryoda.fhysics.engine.FhysicsCore.BORDER
import de.officeryoda.fhysics.engine.Vector2
import de.officeryoda.fhysics.engine.objects.BorderObject
import de.officeryoda.fhysics.engine.objects.Circle
import de.officeryoda.fhysics.engine.objects.FhysicsObject
import de.officeryoda.fhysics.engine.objects.Polygon
import de.officeryoda.fhysics.extensions.times
import de.officeryoda.fhysics.rendering.UIController.Companion.wallElasticity
import kotlin.math.abs

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
        val objA: FhysicsObject = info.objA!!
        val objB: FhysicsObject = info.objB!!

        // No need to solve collision if both objects are static
        if (objA.static && objB.static) return

        val normalForces: ArrayList<Float> =
            solveImpulse(objA, objB, contactPoints, info)
        solveFriction(objA, objB, contactPoints, info, normalForces)
    }

    private fun solveImpulse(
        objA: FhysicsObject,
        objB: FhysicsObject,
        contactPoints: Array<Vector2>,
        info: CollisionInfo,
    ): ArrayList<Float> {
        val e: Float = (objA.restitution + objB.restitution) / 2 // Coefficient of restitution
        //        val e: Float = sqrt(objA.restitution * objB.restitution) // Coefficient of restitution <-- correct formula
        val impulseList: ArrayList<Vector2> = arrayListOf()
        val normalForces: ArrayList<Float> = arrayListOf() // Used for friction
        val normal: Vector2 = info.normal

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
            val contactVelocityMag: Float = relativeVelocity.dot(normal)
            if (contactVelocityMag > 0) {
                normalForces.add(0f)
                continue
            }

            // Calculate the impulse
            val raPerpDotNormal: Float = raPerp.dot(normal)
            val rbPerpDotNormal: Float = rbPerp.dot(normal)

            var impulseMag: Float = -(1f + e) * contactVelocityMag
            impulseMag /= objA.invMass + objB.invMass +
                    (raPerpDotNormal * raPerpDotNormal) * objA.invInertia +
                    (rbPerpDotNormal * rbPerpDotNormal) * objB.invInertia
            impulseMag /= contactPoints.size // Distribute the impulse over all contact points

            val impulse: Vector2 = impulseMag * normal

            impulseList.add(impulse)
            normalForces.add(impulseMag)
        }

        // Apply the impulses
        for (i: Int in impulseList.indices) {
            val impulse: Vector2 = impulseList[i]

            if (!objA.static) {
                objA.velocity += -impulse * objA.invMass
                objA.angularVelocity += impulse.cross(contactPoints[i] - objA.position) * objA.invInertia
            }
            if (!objB.static) {
                objB.velocity += impulse * objB.invMass
                objB.angularVelocity += -impulse.cross(contactPoints[i] - objB.position) * objB.invInertia
            }
        }

        return normalForces
    }

    private fun solveFriction(
        objA: FhysicsObject,
        objB: FhysicsObject,
        contactPoints: Array<Vector2>,
        info: CollisionInfo,
        normalForces: ArrayList<Float>,
    ) {
        val sf: Float = (objA.frictionStatic + objB.frictionStatic) / 2 // Coefficient of static friction
        val df: Float = (objA.frictionDynamic + objB.frictionDynamic) / 2 // Coefficient of dynamic friction

        val frictionList: ArrayList<Vector2> = arrayListOf()
        val normal: Vector2 = info.normal

        // Calculate the friction for each contact point
        for ((i: Int, contactPoint: Vector2) in contactPoints.withIndex()) {
            if (normalForces[i] == 0f) continue

            val ra: Vector2 = contactPoint - objA.position
            val rb: Vector2 = contactPoint - objB.position

            val raPerp = Vector2(-ra.y, ra.x)
            val rbPerp = Vector2(-rb.y, rb.x)

            val totalVelocityA: Vector2 = objA.velocity + raPerp * objA.angularVelocity
            val totalVelocityB: Vector2 = objB.velocity + rbPerp * objB.angularVelocity

            val relativeVelocity: Vector2 = totalVelocityB - totalVelocityA

            // Get the tangent vector of the normal
            var tangent: Vector2 = relativeVelocity - relativeVelocity.dot(normal) * normal
            if (tangent.sqrMagnitude() < 0.0001f) continue
            tangent = tangent.normalized()

            // Calculate the friction impulse
            val raPerpDotTangent: Float = raPerp.dot(tangent)
            val rbPerpDotTangent: Float = rbPerp.dot(tangent)

            var frictionMag: Float = -relativeVelocity.dot(tangent)
            frictionMag /= objA.invMass + objB.invMass +
                    (raPerpDotTangent * raPerpDotTangent) * objA.invInertia +
                    (rbPerpDotTangent * rbPerpDotTangent) * objB.invInertia
            frictionMag /= contactPoints.size // Distribute the impulse over all contact points

            // Apply Coulomb's law
            val normalForce: Float = normalForces[i]
            val frictionImpulse: Vector2 =
                if (abs(frictionMag) <= normalForce * sf) {
                    frictionMag * tangent
                } else {
                    -normalForce * df * tangent
                }

            frictionList.add(frictionImpulse)
        }

        // Apply the impulses
        for (i: Int in frictionList.indices) {
            val frictionImpulse: Vector2 = frictionList[i]

            if (!objA.static) {
                objA.velocity += -frictionImpulse * objA.invMass
                objA.angularVelocity += frictionImpulse.cross(contactPoints[i] - objA.position) * objA.invInertia
            }
            if (!objB.static) {
                objB.velocity += frictionImpulse * objB.invMass
                objB.angularVelocity += -frictionImpulse.cross(contactPoints[i] - objB.position) * objB.invInertia
            }
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
