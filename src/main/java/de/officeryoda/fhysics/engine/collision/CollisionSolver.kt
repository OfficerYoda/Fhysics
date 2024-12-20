package de.officeryoda.fhysics.engine.collision

import de.officeryoda.fhysics.engine.FhysicsCore.BORDER
import de.officeryoda.fhysics.engine.FhysicsCore.EPSILON
import de.officeryoda.fhysics.engine.math.Vector2
import de.officeryoda.fhysics.engine.objects.FhysicsObject
import de.officeryoda.fhysics.engine.util.times
import de.officeryoda.fhysics.rendering.UIController.Companion.borderFrictionDynamic
import de.officeryoda.fhysics.rendering.UIController.Companion.borderFrictionStatic
import de.officeryoda.fhysics.rendering.UIController.Companion.borderRestitution
import java.awt.Color
import kotlin.math.abs
import kotlin.math.sqrt

object CollisionSolver {

    private var borderObjects: Array<BorderEdge> = arrayOf()

    init {
        updateBorderObjects()
    }

    /**
     * Recalculates the border objects based on the current [BORDER].
     */
    fun updateBorderObjects() {
        borderObjects = arrayOf(
            BorderEdge( // Right edge
                Vector2(1f, 0f), BORDER.x + BORDER.width,
                Vector2(BORDER.x + BORDER.width, BORDER.y)
            ),
            BorderEdge( // Left edge
                Vector2(-1f, 0f), BORDER.x,
                Vector2(BORDER.x, BORDER.y + BORDER.height)
            ),
            BorderEdge( // Top edge
                Vector2(0f, 1f), BORDER.y + BORDER.height,
                Vector2(BORDER.x + BORDER.width, BORDER.y + BORDER.height)
            ),
            BorderEdge( // Bottom edge
                Vector2(0f, -1f), BORDER.y,
                Vector2(BORDER.x, BORDER.y)
            )
        )
    }


    /// region =====Object Collision=====
    /**
     * Solves the collision detected by the [collision info][info].
     */
    fun solveCollision(info: CollisionInfo) {
        val objA: FhysicsObject = info.objA!!
        val objB: FhysicsObject = info.objB!!
        if (objA.static && objB.static) return

        // Separate and find contact points
        separateOverlappingObjects(info) // Separate before finding contact points or contact points might be inside objects
        val contactPoints: Array<Vector2> = objA.findContactPoints(objB, info)

        // Solve collision
        val normalForces: FloatArray =
            solveImpulse(objA, objB, contactPoints, info)
        solveFriction(objA, objB, contactPoints, info, normalForces)

        // Update bounding boxes
        objA.updateBoundingBox()
        objB.updateBoundingBox()
    }

    /**
     * Solves the impulse for a collision between [objA] and [objB].
     * @param contactPoints The contact points of the collision
     * @param info The CollisionInfo object containing information about the collision
     * @return A list of normal forces for each contact point
     */
    private fun solveImpulse(
        objA: FhysicsObject,
        objB: FhysicsObject,
        contactPoints: Array<Vector2>,
        info: CollisionInfo,
    ): FloatArray {
//        val e: Float = (objA.restitution + objB.restitution) / 2 // Coefficient of restitution <-- bad approximation
        val e: Float = sqrt(objA.restitution * objB.restitution) // Coefficient of restitution <-- correct formula

        val impulseList: ArrayList<Vector2> = ArrayList(contactPoints.size) // Will have a max size of contactPoints
        val normalForces = FloatArray(contactPoints.size) // Used for friction calculation
        val normal: Vector2 = info.normal

        var totalA = 0f
        var totalB = 0f

        for (contactPoint: Vector2 in contactPoints) {
//            DebugDrawer.addDebugPoint(contactPoint, Color.red)
            val ra: Vector2 = contactPoint - objA.position
            val rb: Vector2 = contactPoint - objB.position

            val raPerp = Vector2(-ra.y, ra.x)
            val rbPerp = Vector2(-rb.y, rb.x)

            // Calculate the impulse
            val raPerpDotNormal: Float = raPerp.dot(normal)
            val rbPerpDotNormal: Float = rbPerp.dot(normal)

            totalA += raPerpDotNormal
            totalB += rbPerpDotNormal
        }

        // If multi is 0, the collision will not have effect on the angular velocity
        val multi: Float = if (abs(totalA) < EPSILON || abs(totalB) < EPSILON) 0f else 1f

        // Calculate the impulses for each contact point
        for ((i: Int, contactPoint: Vector2) in contactPoints.withIndex()) {
            val ra: Vector2 = contactPoint - objA.position
            val rb: Vector2 = contactPoint - objB.position

            val raPerp = Vector2(-ra.y, ra.x)
            val rbPerp = Vector2(-rb.y, rb.x)

            val totalVelocityA: Vector2 = objA.velocity + raPerp * objA.angularVelocity
            val totalVelocityB: Vector2 = objB.velocity + rbPerp * objB.angularVelocity

            val relativeVelocity: Vector2 = totalVelocityB - totalVelocityA

            // Continue if the objects are already moving away from each other
            val contactVelocityMag: Float = relativeVelocity.dot(normal)
            if (contactVelocityMag > EPSILON) {
                normalForces[i] = 0f
                continue
            }

            // Calculate the impulse
            val raPerpDotNormal: Float = raPerp.dot(normal)
            val rbPerpDotNormal: Float = rbPerp.dot(normal)

            var impulseMag: Float = -(1f + e) * contactVelocityMag
            impulseMag /= objA.invMass + objB.invMass +
                    multi * ((raPerpDotNormal * raPerpDotNormal) * objA.invInertia
                    + (rbPerpDotNormal * rbPerpDotNormal) * objB.invInertia)
            impulseMag /= contactPoints.size // Distribute the impulse over all contact points

            val impulse: Vector2 = impulseMag * normal

            impulseList.add(impulse)
            normalForces[i] = impulseMag
        }

        // Apply impulses in separate loop to avoid affecting the calculation of following contact points
        for ((i: Int, impulse: Vector2) in impulseList.withIndex()) {
            objA.velocity += -impulse * objA.invMass
            objA.angularVelocity += impulse.cross(contactPoints[i] - objA.position) * objA.invInertia
            objB.velocity += impulse * objB.invMass
            objB.angularVelocity += -impulse.cross(contactPoints[i] - objB.position) * objB.invInertia
        }

        // Set angular velocity to 0 if it's very small (this improves stability)
        if (abs(objA.angularVelocity) < EPSILON) objA.angularVelocity = 0f
        if (abs(objB.angularVelocity) < EPSILON) objB.angularVelocity = 0f

        return normalForces
    }

    /**
     * Solves the friction between [objA] and [objB].
     * @param contactPoints The contact points of the collision
     * @param info The CollisionInfo object containing information about the collision
     * @param normalForces A list of normal forces for each contact point
     */
    private fun solveFriction(
        objA: FhysicsObject,
        objB: FhysicsObject,
        contactPoints: Array<Vector2>,
        info: CollisionInfo,
        normalForces: FloatArray,
    ) {
        val sf: Float = (objA.frictionStatic + objB.frictionStatic) / 2 // Coefficient of static friction
        val df: Float = (objA.frictionDynamic + objB.frictionDynamic) / 2 // Coefficient of dynamic friction

        val frictionList: ArrayList<Vector2> = ArrayList(contactPoints.size) // Will have a max size of contactPoints
        val normal: Vector2 = info.normal
        val frictionType: ArrayList<Color> = ArrayList() // TODO: Remove this

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
            if (tangent.sqrMagnitude() < EPSILON) continue // Continue if there is no tangential velocity
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
                    frictionMag * tangent // Static friction
                } else {
                    -normalForce * df * tangent // Dynamic friction
                }

            frictionList.add(frictionImpulse)
            if (abs(frictionMag) <= normalForce * sf) {
                frictionType.add(Color.red)
            } else {
                frictionType.add(Color.blue)
            }
        }

        // This is used so that the rectangle doesn't get a slight tilt when sliding down a slope
        // It is still sliding down the slope, but my current hypothesis is that it's caused by the rectangle slightly clipping into the slope,
        // which pushes it out but due to the current implementation the rectangle ends up in a slightly lower position
        // TODO: Do this better or find a better solution
        // How it works: If the vector from one contact point to the other is parallel to the average friction vector, the friction vector is set to 0
        // This should happen when the object is sliding down a slope
        // Somehow this doesn't affect other instances (noticeable) where those conditions are met as well (e.g. when a rect is hitting another stationary rect)
        var multi = 1f
        if (frictionList.size > 1) {
            val v1: Vector2 = contactPoints[0] - contactPoints[1]
            val v2: Vector2 = (frictionList[0] + frictionList[1]) / 2f
            if (abs(v1.cross(v2)) < EPSILON) multi = 0f
        }

        // Apply impulses in separate loop to avoid affecting the calculation of following contact points
        for ((i: Int, frictionImpulse: Vector2) in frictionList.withIndex()) {
            objA.velocity += -frictionImpulse * objA.invMass
            objA.angularVelocity += frictionImpulse.cross(contactPoints[i] - objA.position) * objA.invInertia * multi
            objB.velocity += frictionImpulse * objB.invMass
            objB.angularVelocity += -frictionImpulse.cross(contactPoints[i] - objB.position) * objB.invInertia * multi
        }

        // Remove small angular velocities (this improves stability)
        if (abs(objA.angularVelocity) < EPSILON) objA.angularVelocity = 0f
        if (abs(objB.angularVelocity) < EPSILON) objB.angularVelocity = 0f
    }

    /**
     * Separates the objects that are contained in the [collision info][info].
     */
    private fun separateOverlappingObjects(info: CollisionInfo) {
        val objA: FhysicsObject = info.objA!!
        val objB: FhysicsObject = info.objB!!

        val overlap: Vector2 = info.depth * info.normal

        if (!objA.static) objA.position -= if (!objB.static) 0.5f * overlap else overlap
        if (!objB.static) objB.position += if (!objA.static) 0.5f * overlap else overlap
    }
    /// endregion

    /// region =====Border Collision=====
    /**
     * Handles the possible collision between an [object][obj] and the border.
     */
    fun handleBorderCollisions(obj: FhysicsObject) {
        if (obj.static) return
        // Return if the object is fully inside the border
        if (BORDER.contains(obj.boundingBox)) return

        // This is a separate step because the object might be outside two edges at the same time
        val collidingBorders: MutableSet<BorderEdge> = moveInsideBorder(obj)

        // Find contact points and solve collisions
        for (border: BorderEdge in collidingBorders) {
            solveBorderCollision(obj, border)
        }
    }

    /**
     * Solves the collision between an [object][obj] and an [edge][edge] of the border.
     */
    private fun solveBorderCollision(
        obj: FhysicsObject,
        edge: BorderEdge,
    ) {
        // Find contact points
        val contactPoints: Array<Vector2> = obj.findContactPoints(edge)
        // Early out
        if (contactPoints.isEmpty()) return

        // Solve collision
        val normalForces: FloatArray =
            solveBorderImpulse(edge, obj, contactPoints)
        solveBorderFriction(edge, obj, contactPoints, normalForces)
    }

    /**
     * Solves the impulse for a collision between an [object][obj] and an [edge] of the border.
     * @param contactPoints The contact points of the collision
     * @return A list of normal forces for each contact point
     */
    private fun solveBorderImpulse(
        edge: BorderEdge,
        obj: FhysicsObject,
        contactPoints: Array<Vector2>,
    ): FloatArray {
//        val e: Float = (obj.restitution * borderRestitution) / 2 // Coefficient of restitution <-- bad approximation
        val e: Float = sqrt(obj.restitution * borderRestitution) // Coefficient of restitution <-- correct formula
        val impulseList: ArrayList<Vector2> = ArrayList(contactPoints.size) // Will have a max size of contactPoints
        val normalForces = FloatArray(contactPoints.size) // Used for friction calculation
        val normal: Vector2 = edge.normal

        // Calculate the impulses for each contact point
        for ((i: Int, contactPoint: Vector2) in contactPoints.withIndex()) {
            val r: Vector2 = contactPoint - obj.position

            val rPerp = Vector2(-r.y, r.x)

            val totalVelocity: Vector2 = obj.velocity + rPerp * obj.angularVelocity

            // Continue if the objects are already moving away from each other
            val velAlongNormal: Float = -totalVelocity.dot(normal)
            if (velAlongNormal > EPSILON) {
                normalForces[i] = 0f
                continue
            }

            // Calculate the impulse
            val rPerpDotNormal: Float = rPerp.dot(normal)

            var impulseMag: Float = -(1f + e) * velAlongNormal
            impulseMag /= obj.invMass +
                    (rPerpDotNormal * rPerpDotNormal) * obj.invInertia
            impulseMag /= contactPoints.size // Distribute the impulse over all contact points

            val impulse: Vector2 = impulseMag * normal

            impulseList.add(impulse)
            normalForces[i] = impulseMag
        }

        // Apply impulses in separate loop to avoid affecting the calculation of following contact points
        for ((i: Int, impulse: Vector2) in impulseList.withIndex()) {
            obj.velocity += -impulse * obj.invMass
            obj.angularVelocity += impulse.cross(contactPoints[i] - obj.position) * obj.invInertia
        }

        // Set angular velocity to 0 if it's very small (this improves stability)
        if (abs(obj.angularVelocity) < EPSILON) obj.angularVelocity = 0f

        return normalForces
    }

    /**
     * Solves the friction between an [object][obj] and an [edge] of the border.
     * @param contactPoints The contact points of the collision
     * @param normalForces A list of normal forces for each contact point
     */
    private fun solveBorderFriction(
        edge: BorderEdge,
        obj: FhysicsObject,
        contactPoints: Array<Vector2>,
        normalForces: FloatArray,
    ) {
        val sf: Float = (borderFrictionStatic + obj.frictionStatic) / 2 // Coefficient of static friction
        val df: Float = (borderFrictionDynamic + obj.frictionDynamic) / 2// Coefficient of dynamic friction

        val frictionList: ArrayList<Vector2> = ArrayList(contactPoints.size) // Will have a max size of contactPoints
        val normal: Vector2 = edge.normal

        // Calculate the friction for each contact point
        for ((i: Int, contactPoint: Vector2) in contactPoints.withIndex()) {
            if (normalForces[i] == 0f) continue

            val r: Vector2 = contactPoint - obj.position

            val rPerp = Vector2(-r.y, r.x)

            val totalVelocity: Vector2 = obj.velocity + rPerp * obj.angularVelocity
            val relativeVelocity: Vector2 = -totalVelocity

            // Get the tangent vector of the normal
            var tangent: Vector2 = relativeVelocity - relativeVelocity.dot(normal) * normal
            if (tangent.sqrMagnitude() < EPSILON) continue // Continue if there is no tangential velocity
            tangent = tangent.normalized()

            // Calculate the friction impulse
            val rPerpDotTangent: Float = rPerp.dot(tangent)

            var frictionMag: Float = -relativeVelocity.dot(tangent)
            frictionMag /= obj.invMass +
                    (rPerpDotTangent * rPerpDotTangent) * obj.invInertia
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

        // Apply impulses in separate loop to avoid affecting the calculation of following contact points
        for ((i: Int, frictionImpulse: Vector2) in frictionList.withIndex()) {
            obj.velocity += -frictionImpulse * obj.invMass
            obj.angularVelocity += frictionImpulse.cross(contactPoints[i] - obj.position) * obj.invInertia
        }
    }

    /**
     * Moves an [object][obj] inside the border and
     * returns a set of border edges the object is colliding with.
     */
    fun moveInsideBorder(obj: FhysicsObject): MutableSet<BorderEdge> {
        // Check for collision with the border
        val collidingBorders: MutableSet<BorderEdge> = mutableSetOf()
        for (border: BorderEdge in borderObjects) {
            val info: CollisionInfo = border.testCollision(obj)
            if (!info.hasCollision) continue

            obj.position += -info.normal * info.depth
            collidingBorders.add(border)
        }

        obj.updateBoundingBox()
        return collidingBorders
    }
    /// endregion
}
