package de.officeryoda.fhysics.engine.objects

import de.officeryoda.fhysics.engine.FhysicsCore
import de.officeryoda.fhysics.engine.collision.CollisionInfo
import de.officeryoda.fhysics.engine.collision.CollisionSolver
import de.officeryoda.fhysics.engine.math.Vector2
import de.officeryoda.fhysics.rendering.GravityType
import de.officeryoda.fhysics.rendering.UIController.Companion.damping
import de.officeryoda.fhysics.rendering.UIController.Companion.gravityDirection
import de.officeryoda.fhysics.rendering.UIController.Companion.gravityPointStrength
import de.officeryoda.fhysics.rendering.UIController.Companion.gravityType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ElasticCollisionTest {


    @BeforeEach
    fun setup() {
        FhysicsCore.clear()
    }

    @Test
    fun testPerfectlyElasticCollisionCircles() {
        // Create two circles
        val circleA: Circle = Circle(Vector2(60.5f, 50f), 5f).apply {
            velocity.set(Vector2(10f, 0f))
            mass = 2f
        }

        val circleB: Circle = Circle(Vector2(70f, 50f), 5f).apply {
            velocity.set(Vector2(-10f, 0f))
            mass = 2f
        }

        // Adjust properties for perfectly elastic collision
        setRestitution(1f, circleA, circleB)
        setZeroFriction(circleA, circleB)

        // Simulate the collision
        handleCollision(circleA, circleB, "testPerfectlyElasticCollisionCircles")

        // Check the velocities after collision
        assertEquals(Vector2(-10f, 0f), circleA.velocity)
        assertEquals(Vector2(10f, 0f), circleB.velocity)
        assertEquals(0f, circleA.angularVelocity)
        assertEquals(0f, circleB.angularVelocity)
    }

    @Test
    fun testPerfectlyElasticCollisionRectangles() {
        // Create two rectangles
        val rectA: Rectangle = Rectangle(Vector2(60.5f, 50f), 10f, 5f).apply {
            velocity.set(Vector2(10f, 0f))
            mass = 2f
        }

        val rectB: Rectangle = Rectangle(Vector2(70f, 50f), 10f, 5f).apply {
            velocity.set(Vector2(-10f, 0f))
            mass = 2f
        }

        // Adjust properties for perfectly elastic collision
        setRestitution(1f, rectA, rectB)
        setZeroFriction(rectA, rectB)

        // Simulate the collision
        handleCollision(rectA, rectB, "testPerfectlyElasticCollisionRectangle")

        // Check the velocities after collision
        assertEquals(Vector2(-10f, 0f), rectA.velocity)
        assertEquals(Vector2(10f, 0f), rectB.velocity)
        assertEquals(0f, rectA.angularVelocity)
        assertEquals(0f, rectB.angularVelocity)
    }

    /**
     * Handles the collision between two objects and solves it.
     * Prints an error message if no collision is detected.
     *
     * @param objA The first object to test for collision.
     * @param objB The second object to test for collision.
     * @param testName The name of the test for error messages.
     */
    private fun handleCollision(objA: FhysicsObject, objB: FhysicsObject, testName: String) {
        val info: CollisionInfo = objA.testCollision(objB)
        if (!info.hasCollision) {
            System.err.println("[$testName] No collision detected!")
            return
        }

        CollisionSolver.solveCollision(info)
    }

    /**
     * Sets the restitution of the given objects.
     *
     * @param restitution The new restitution value.
     * @param objects The objects to set the restitution for.
     */
    private fun setRestitution(restitution: Float, vararg objects: FhysicsObject) {
        for (o: FhysicsObject in objects) {
            o.restitution = restitution
        }
    }

    /**
     * Sets the friction of the given objects.
     *
     * @param frictionStatic The new static friction value.
     * @param frictionDynamic The new dynamic friction value.
     * @param objects The objects to set the friction for.
     */
    private fun setFriction(frictionStatic: Float, frictionDynamic: Float, vararg objects: FhysicsObject) {
        for (o: FhysicsObject in objects) {
            o.frictionStatic = frictionStatic
            o.frictionDynamic = frictionDynamic
        }
    }

    /**
     * Sets the friction of the given objects to zero.
     *
     * @param objects The objects to set the friction for.
     */
    private fun setZeroFriction(vararg objects: FhysicsObject) {
        setFriction(0f, 0f, *objects)
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun masterSetup() {
            if (damping != 0f) {
                System.err.println("Damping is not 0! Tests may fail!")
            }
            when {
                gravityType == GravityType.DIRECTIONAL && gravityDirection.sqrMagnitude() != 0f -> System.err.println("Gravity is not 0! Tests may fail!")
                gravityType == GravityType.TOWARDS_POINT && gravityPointStrength != 0f -> System.err.println("Gravity is not 0! Tests may fail!")
            }
            FhysicsCore.clear()
        }
    }
}