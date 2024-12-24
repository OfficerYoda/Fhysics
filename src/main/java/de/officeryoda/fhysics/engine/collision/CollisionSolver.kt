package de.officeryoda.fhysics.engine.collision

import de.officeryoda.fhysics.engine.FhysicsCore.BORDER
import de.officeryoda.fhysics.engine.FhysicsCore.EPSILON
import de.officeryoda.fhysics.engine.math.Vector2
import de.officeryoda.fhysics.engine.objects.FhysicsObject
import de.officeryoda.fhysics.engine.util.times
import de.officeryoda.fhysics.rendering.UIController.Companion.borderFrictionDynamic
import de.officeryoda.fhysics.rendering.UIController.Companion.borderFrictionStatic
import de.officeryoda.fhysics.rendering.UIController.Companion.borderRestitution
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
        solveFriction(objA, objB, contactPoints, normalForces, info.normal)

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
        val impulseList = ArrayList<Vector2>(contactPoints.size)

        val normalForces: FloatArray =
            processObjectContactPointsImpulse(contactPoints, objA, objB, info.normal, impulseList)

        applyImpulses(impulseList, contactPoints, objA, objB)

        clampSmallAngularVelocity(objA)
        clampSmallAngularVelocity(objB)

        return normalForces
    }

    private fun processObjectContactPointsImpulse(
        contactPoints: Array<Vector2>,
        objA: FhysicsObject,
        objB: FhysicsObject,
        normal: Vector2,
        impulseList: ArrayList<Vector2>,
    ): FloatArray {
        val e: Float = sqrt(objA.restitution * objB.restitution) // Coefficient of restitution
        val normalForces = FloatArray(contactPoints.size)

        for ((i: Int, contactPoint: Vector2) in contactPoints.withIndex()) {
            val raPerp: Vector2 = calculateContactPerpendicular(contactPoint, objA)
            val rbPerp: Vector2 = calculateContactPerpendicular(contactPoint, objB)
            val relativeVelocity: Vector2 = calculateRelativeVelocity(objA, objB, raPerp, rbPerp)

            // Continue if the objects are already moving away from each other
            val contactVelocityMag: Float = relativeVelocity.dot(normal)
            if (contactVelocityMag > EPSILON) {
                normalForces[i] = 0f
                continue
            }

            val impulse: Vector2 =
                calculateImpulse(objA, objB, normal, e, contactPoints.size, contactVelocityMag, raPerp, rbPerp)
            impulseList.add(impulse)
            normalForces[i] = impulse.magnitude()
        }

        return normalForces
    }

    private fun calculateRelativeVelocity(
        objA: FhysicsObject,
        objB: FhysicsObject,
        raPerp: Vector2,
        rbPerp: Vector2,
    ): Vector2 {
        val totalVelocityA: Vector2 = objA.velocity + raPerp * objA.angularVelocity
        val totalVelocityB: Vector2 = objB.velocity + rbPerp * objB.angularVelocity
        return totalVelocityB - totalVelocityA
    }

    private fun calculateImpulse(
        objA: FhysicsObject,
        objB: FhysicsObject,
        normal: Vector2,
        e: Float, // Coefficient of restitution
        contactPointsSize: Int,
        contactVelocityMag: Float,
        raPerp: Vector2,
        rbPerp: Vector2,
    ): Vector2 {
        val raPerpDotNormal: Float = raPerp.dot(normal)
        val rbPerpDotNormal: Float = rbPerp.dot(normal)

        // Calculate the impulse magnitude
        var impulseMag: Float = -(1f + e) * contactVelocityMag
        impulseMag /= objA.invMass + objB.invMass +
                (raPerpDotNormal * raPerpDotNormal) * objA.invInertia +
                (rbPerpDotNormal * rbPerpDotNormal) * objB.invInertia
        impulseMag /= contactPointsSize // Distribute the impulse over all contact points

        return impulseMag * normal
    }

    private fun applyImpulses(
        impulseList: ArrayList<Vector2>,
        contactPoints: Array<Vector2>,
        objA: FhysicsObject,
        objB: FhysicsObject,
    ) {
        for ((i: Int, impulse: Vector2) in impulseList.withIndex()) {
            objA.velocity += -impulse * objA.invMass
            objA.angularVelocity += impulse.cross(contactPoints[i] - objA.position) * objA.invInertia
            objB.velocity += impulse * objB.invMass
            objB.angularVelocity += -impulse.cross(contactPoints[i] - objB.position) * objB.invInertia
        }
    }

    /**
     * Solves the friction between [objA] and [objB].
     * @param contactPoints The contact points of the collision
     * @param normalForces A list of normal forces for each contact point
     */
    private fun solveFriction(
        objA: FhysicsObject,
        objB: FhysicsObject,
        contactPoints: Array<Vector2>,
        normalForces: FloatArray,
        normal: Vector2,
    ) {
        val frictionList: ArrayList<Vector2> = ArrayList(contactPoints.size) // Will have a max size of contactPoints

        // Calculate the friction for each contact point
        processObjectContactPointsFriction(contactPoints, objA, objB, normal, frictionList, normalForces)

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

        applyFriction(frictionList, contactPoints, objA, objB, multi)

        clampSmallAngularVelocity(objA)
        clampSmallAngularVelocity(objB)
    }

    private fun processObjectContactPointsFriction(
        contactPoints: Array<Vector2>,
        objA: FhysicsObject,
        objB: FhysicsObject,
        normal: Vector2,
        frictionList: ArrayList<Vector2>,
        normalForces: FloatArray,
    ) {
        val sf: Float = (objA.frictionStatic + objB.frictionStatic) / 2 // Coefficient of static friction
        val df: Float = (objA.frictionDynamic + objB.frictionDynamic) / 2 // Coefficient of dynamic friction

        for ((i: Int, contactPoint: Vector2) in contactPoints.withIndex()) {
            if (normalForces[i] == 0f) continue

            val raPerp: Vector2 = calculateContactPerpendicular(contactPoint, objA)
            val rbPerp: Vector2 = calculateContactPerpendicular(contactPoint, objB)
            val relativeVelocity: Vector2 = calculateRelativeVelocity(objA, objB, raPerp, rbPerp)

            // Get the tangent vector of the normal
            var tangent: Vector2 = relativeVelocity - relativeVelocity.dot(normal) * normal // Tangential velocity
            if (tangent.sqrMagnitude() < EPSILON) continue // Continue if there is no tangential velocity
            tangent = tangent.normalized()

            // Calculate the friction impulse
            val raPerpDotTangent: Float = raPerp.dot(tangent)
            val rbPerpDotTangent: Float = rbPerp.dot(tangent)

            var frictionMag: Float =
                calculateFrictionMagnitude(
                    relativeVelocity, tangent,
                    objA, objB,
                    raPerpDotTangent, rbPerpDotTangent,
                    contactPoints
                )

            // Apply Coulomb's law of friction
            val frictionImpulse: Vector2 = applyCoulombsLaw(normalForces[i], frictionMag, tangent, sf, df)
            frictionList.add(frictionImpulse)
        }
    }

    private fun calculateFrictionMagnitude(
        relativeVelocity: Vector2,
        tangent: Vector2,
        objA: FhysicsObject,
        objB: FhysicsObject,
        raPerpDotTangent: Float,
        rbPerpDotTangent: Float,
        contactPoints: Array<Vector2>,
    ): Float {
        var frictionMag: Float = -relativeVelocity.dot(tangent)
        frictionMag /= objA.invMass + objB.invMass +
                (raPerpDotTangent * raPerpDotTangent) * objA.invInertia +
                (rbPerpDotTangent * rbPerpDotTangent) * objB.invInertia
        frictionMag /= contactPoints.size // Distribute the impulse over all contact points

        return frictionMag
    }

    private fun applyFriction(
        frictionList: ArrayList<Vector2>,
        contactPoints: Array<Vector2>,
        objA: FhysicsObject,
        objB: FhysicsObject,
        multi: Float,
    ) {
        for ((i: Int, frictionImpulse: Vector2) in frictionList.withIndex()) {
            objA.velocity += -frictionImpulse * objA.invMass
            objA.angularVelocity += frictionImpulse.cross(contactPoints[i] - objA.position) * objA.invInertia * multi
            objB.velocity += frictionImpulse * objB.invMass
            objB.angularVelocity += -frictionImpulse.cross(contactPoints[i] - objB.position) * objB.invInertia * multi
        }
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

        // Solve collision with every colliding border
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

        // Solve collision
        val normalForces: FloatArray =
            solveImpulseBorder(edge, obj, contactPoints)
        solveFrictionBorder(edge, obj, contactPoints, normalForces)
    }

    /**
     * Solves the impulse for a collision between an [object][obj] and an [edge] of the border.
     * @param contactPoints The contact points of the collision
     * @return A list of normal forces for each contact point
     */
    private fun solveImpulseBorder(
        edge: BorderEdge,
        obj: FhysicsObject,
        contactPoints: Array<Vector2>,
    ): FloatArray {
        val impulseList: ArrayList<Vector2> = ArrayList(contactPoints.size) // Will have a max size of contactPoints

        val normalForces: FloatArray = processBorderContactPointsImpulse(contactPoints, obj, edge.normal, impulseList)

        applyImpulse(impulseList, contactPoints, obj)

        clampSmallAngularVelocity(obj)

        return normalForces
    }

    private fun processBorderContactPointsImpulse(
        contactPoints: Array<Vector2>,
        obj: FhysicsObject,
        normal: Vector2,
        impulseList: ArrayList<Vector2>,
    ): FloatArray {
        val normalForces = FloatArray(contactPoints.size)
        val e: Float = sqrt(obj.restitution * borderRestitution) // Coefficient of restitution

        // Calculate the impulses for each contact point
        for ((i: Int, contactPoint: Vector2) in contactPoints.withIndex()) {
            val rPerp: Vector2 = calculateContactPerpendicular(contactPoint, obj)

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

        return normalForces
    }

    private fun applyImpulse(
        impulseList: ArrayList<Vector2>,
        contactPoints: Array<Vector2>,
        obj: FhysicsObject,
    ) {
        for ((i: Int, impulse: Vector2) in impulseList.withIndex()) {
            obj.velocity += -impulse * obj.invMass
            obj.angularVelocity += impulse.cross(contactPoints[i] - obj.position) * obj.invInertia
        }
    }

    /**
     * Solves the friction between an [object][obj] and an [edge] of the border.
     * @param contactPoints The contact points of the collision
     * @param normalForces A list of normal forces for each contact point
     */
    private fun solveFrictionBorder(
        edge: BorderEdge,
        obj: FhysicsObject,
        contactPoints: Array<Vector2>,
        normalForces: FloatArray,
    ) {
        val frictionList: ArrayList<Vector2> = ArrayList(contactPoints.size) // Will have a max size of contactPoints
        val normal: Vector2 = edge.normal

        // Calculate the friction for each contact point
        processBorderContactPointsFriction(contactPoints, obj, normal, frictionList, normalForces)

        // Apply impulses in separate loop to avoid affecting the calculation of following contact points
        for ((i: Int, frictionImpulse: Vector2) in frictionList.withIndex()) {
            obj.velocity += -frictionImpulse * obj.invMass
            obj.angularVelocity += frictionImpulse.cross(contactPoints[i] - obj.position) * obj.invInertia
        }
    }

    private fun processBorderContactPointsFriction(
        contactPoints: Array<Vector2>,
        obj: FhysicsObject,
        normal: Vector2,
        frictionList: ArrayList<Vector2>,
        normalForces: FloatArray,
    ) {
        val sf: Float = (borderFrictionStatic + obj.frictionStatic) / 2 // Coefficient of static friction
        val df: Float = (borderFrictionDynamic + obj.frictionDynamic) / 2// Coefficient of dynamic friction

        for ((i: Int, contactPoint: Vector2) in contactPoints.withIndex()) {
            if (normalForces[i] == 0f) continue

            val rPerp: Vector2 = calculateContactPerpendicular(contactPoint, obj)

            val totalVelocity: Vector2 = obj.velocity + rPerp * obj.angularVelocity
            val relativeVelocity: Vector2 = -totalVelocity

            // Get the tangent vector of the normal
            var tangent: Vector2 = relativeVelocity - relativeVelocity.dot(normal) * normal
            if (tangent.sqrMagnitude() < EPSILON) continue // Continue if there is no tangential velocity
            tangent = tangent.normalized()

            // Calculate the friction impulse
            val rPerpDotTangent: Float = rPerp.dot(tangent)

            var frictionMag: Float =
                calculateFrictionMagnitude(relativeVelocity, tangent, obj, rPerpDotTangent, contactPoints)

            // Apply Coulomb's law
            val frictionImpulse: Vector2 = applyCoulombsLaw(normalForces[i], frictionMag, tangent, sf, df)
            frictionList.add(frictionImpulse)
        }
    }

    private fun calculateFrictionMagnitude(
        relativeVelocity: Vector2,
        tangent: Vector2,
        obj: FhysicsObject,
        rPerpDotTangent: Float,
        contactPoints: Array<Vector2>,
    ): Float {
        var frictionMag: Float = -relativeVelocity.dot(tangent)
        frictionMag /= obj.invMass +
                (rPerpDotTangent * rPerpDotTangent) * obj.invInertia
        frictionMag /= contactPoints.size // Distribute the impulse over all contact points

        return frictionMag
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

            // Move the object inside the border
            obj.position += -info.normal * info.depth
            collidingBorders.add(border)
        }

        obj.updateBoundingBox()
        return collidingBorders
    }
    /// endregion

    /**
     * Returns the vector perpendicular to the vector from the object's position to the contact point.
     */
    private fun calculateContactPerpendicular(
        contactPoint: Vector2,
        obj: FhysicsObject,
    ): Vector2 {
        val r: Vector2 = contactPoint - obj.position
        val rPerp = Vector2(-r.y, r.x)
        return rPerp
    }

    /**
     * Returns the friction impulse calculated by applying Coulomb's law.
     */
    private fun applyCoulombsLaw(
        normalForce: Float,
        frictionMag: Float,
        tangent: Vector2,
        sf: Float, // Coefficient of static friction
        df: Float, // Coefficient of dynamic friction
    ): Vector2 {
        return if (abs(frictionMag) >= normalForce * sf) {
            frictionMag * tangent // Static friction
        } else {
            -normalForce * df * tangent // Dynamic friction
        }
    }

    /**
     * Sets the angular velocity of an [object][obj] to 0 if it's very small.
     */
    private fun clampSmallAngularVelocity(obj: FhysicsObject) {
        // Set angular velocity to 0 if it's very small (this improves stability)
        if (abs(obj.angularVelocity) < EPSILON) obj.angularVelocity = 0f
    }
}
