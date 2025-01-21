@file:Suppress("SameParameterValue", "SameParameterValue", "SameParameterValue")

package de.officeryoda.fhysics.engine.objects

import de.officeryoda.fhysics.engine.GravityType
import de.officeryoda.fhysics.engine.Settings
import de.officeryoda.fhysics.engine.Settings.EPSILON
import de.officeryoda.fhysics.engine.collision.CollisionInfo
import de.officeryoda.fhysics.engine.collision.CollisionSolver
import de.officeryoda.fhysics.engine.datastructures.QuadTree
import de.officeryoda.fhysics.engine.math.Vector2
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Some tests are taken from wikipedia for elastic collisions.
 * https://en.wikipedia.org/wiki/Elastic_collision
 */
@Suppress("SameParameterValue", "SameParameterValue", "SameParameterValue")
class ElasticCollisionTest {


    @BeforeEach
    fun setup() {
        QuadTree.clear()
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
        val rectA: Rectangle = Rectangle(Vector2(60.25f, 50f), 10f, 5f).apply {
            velocity.set(Vector2(10f, 0f))
            mass = 2f
        }

        val rectB: Rectangle = Rectangle(Vector2(69.75f, 50f), 10f, 5f).apply {
            velocity.set(Vector2(-10f, 0f))
            mass = 2f
        }
        // Adjust properties for perfectly elastic collision
        setRestitution(1f, rectA, rectB)
        setZeroFriction(rectA, rectB)

        println("Before collision energies: ${calculateEnergy(rectA)} ${calculateEnergy(rectB)}")
        // Simulate the collision
        handleCollision(rectA, rectB, "testPerfectlyElasticCollisionRectangle")
        println("After collision energies: ${calculateEnergy(rectA)} ${calculateEnergy(rectB)}")

        // Check the velocities after collision
        assertAlmostEquals(Vector2(-10f, 0f), rectA.velocity)
        assertAlmostEquals(Vector2(10f, 0f), rectB.velocity)
        assertEquals(0f, rectA.angularVelocity)
        assertEquals(0f, rectB.angularVelocity)
    }

    private fun calculateEnergy(obj: FhysicsObject): Float {
        val kineticEnergy: Float = 0.5f * obj.mass * obj.velocity.sqrMagnitude()
        val rotationalEnergy: Float = 0.5f * obj.inertia * obj.angularVelocity * obj.angularVelocity
        return kineticEnergy + rotationalEnergy
    }

    @Test
    fun testPerfectlyElasticCollisionWithDifferentMasses() {
        // Create two circles
        val circleA: Circle = Circle(Vector2(60.5f, 50f), 5f).apply {
            velocity.set(Vector2(10f, 0f))
            mass = 4f
        }

        val circleB: Circle = Circle(Vector2(70f, 50f), 5f).apply {
            velocity.set(Vector2(-10f, 0f))
            mass = 2f
        }

        // Adjust properties for perfectly elastic collision
        setRestitution(1f, circleA, circleB)
        setZeroFriction(circleA, circleB)

        // Simulate the collision
        handleCollision(circleA, circleB, "testPerfectlyElasticCollisionWithDifferentMasses")

        // Check the velocities after collision
        assertAlmostEquals(Vector2(-10f, 0f) * 1f / 3f, circleA.velocity)
        assertAlmostEquals(Vector2(10f, 0f) * 5f / 3f, circleB.velocity)
        assertEquals(0f, circleA.angularVelocity)
        assertEquals(0f, circleB.angularVelocity)
    }

    @Test
    fun testPerfectlyElasticCollisionWithStationaryObject() {
        // Create a circle and a stationary rectangle
        val circle: Circle = Circle(Vector2(60.5f, 50f), 5f).apply {
            velocity.set(Vector2(10f, 0f))
            mass = 2f
        }

        val rect: Rectangle = Rectangle(Vector2(70f, 50f), 10f, 5f).apply {
            velocity.set(Vector2(0f, 0f))
            mass = 2f
        }

        // Adjust properties for perfectly elastic collision
        setRestitution(1f, circle, rect)
        setZeroFriction(circle, rect)

        // Simulate the collision
        handleCollision(circle, rect, "testPerfectlyElasticCollisionWithStationaryObject")

        // Check the velocities after collision
        assertAlmostEquals(Vector2(0f, 0f), circle.velocity)
        assertAlmostEquals(Vector2(10f, 0f), rect.velocity)
        assertEquals(0f, circle.angularVelocity)
        assertEquals(0f, rect.angularVelocity)
    }

    @Test
    fun testPerfectlyElasticCollisionWithDifferentVelocity() {
        // Create two circles
        val circleA: Circle = Circle(Vector2(60.5f, 50f), 5f).apply {
            velocity.set(Vector2(10f, 0f))
            mass = 2f
        }

        val circleB: Circle = Circle(Vector2(70f, 50f), 5f).apply {
            velocity.set(Vector2(-5f, 0f))
            mass = 2f
        }

        // Adjust properties for perfectly elastic collision
        setRestitution(1f, circleA, circleB)
        setZeroFriction(circleA, circleB)

        // Simulate the collision
        handleCollision(circleA, circleB, "testPerfectlyElasticCollisionWithDifferentVelocity")

        // Check the velocities after collision
        assertAlmostEquals(Vector2(-5f, 0f), circleA.velocity)
        assertAlmostEquals(Vector2(10f, 0f), circleB.velocity)
        assertEquals(0f, circleA.angularVelocity)
        assertEquals(0f, circleB.angularVelocity)
    }

    @Test
    fun testPerfectlyElasticCollisionMovingInTheSameDirection() {
        // Create two circles
        val circleA: Circle = Circle(Vector2(60.5f, 50f), 5f).apply {
            velocity.set(Vector2(10f, 0f))
            mass = 2f
        }

        val circleB: Circle = Circle(Vector2(70f, 50f), 5f).apply {
            velocity.set(Vector2(5f, 0f))
            mass = 2f
        }

        // Adjust properties for perfectly elastic collision
        setRestitution(1f, circleA, circleB)
        setZeroFriction(circleA, circleB)

        // Simulate the collision
        handleCollision(circleA, circleB, "testPerfectlyElasticCollisionMovingInTheSameDirection")

        // Check the velocities after collision
        assertAlmostEquals(Vector2(5f, 0f), circleA.velocity)
        assertAlmostEquals(Vector2(10f, 0f), circleB.velocity)
        assertEquals(0f, circleA.angularVelocity)
        assertEquals(0f, circleB.angularVelocity)
    }

    @Test
    fun testPerfectlyElasticCollisionWithStaticObject() {
        // Create a circle and a stationary rectangle
        val circle: Circle = Circle(Vector2(60.5f, 50f), 5f).apply {
            velocity.set(Vector2(10f, 0f))
            mass = 2f
        }

        val rect: Rectangle = Rectangle(Vector2(70f, 50f), 10f, 5f).apply {
            velocity.set(Vector2(0f, 0f))
            mass = 2f
        }

        // Adjust properties for perfectly elastic collision
        setRestitution(1f, circle, rect)
        setZeroFriction(circle, rect)

        // Simulate the collision
        handleCollision(circle, rect, "testPerfectlyElasticCollisionWithStaticObject")

        // Check the velocities after collision
        assertAlmostEquals(Vector2(0f, 0f), circle.velocity)
        assertAlmostEquals(Vector2(10f, 0f), rect.velocity)
        assertEquals(0f, circle.angularVelocity)
        assertEquals(0f, rect.angularVelocity)
    }

    /**
     * Asserts that the components of the two vectors are almost equal.
     * The difference between the components must be less than [EPSILON].
     *
     * @param expected The expected vector.
     * @param actual The actual vector.
     */
    private fun assertAlmostEquals(expected: Vector2, actual: Vector2) {
        assertEquals(expected.x, actual.x, EPSILON, "X components are not equal")
        assertEquals(expected.y, actual.y, EPSILON, "Y components are not equal")
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
            if (Settings.damping != 0f) {
                System.err.println("Damping is not 0! Tests may fail!")
            }
            when {
                Settings.gravityType == GravityType.DIRECTIONAL && Settings.gravityDirection.sqrMagnitude() != 0f
                    -> System.err.println("Gravity is not 0! Tests may fail!")

                Settings.gravityType == GravityType.TOWARDS_POINT && Settings.gravityPointStrength != 0f
                    -> System.err.println("Gravity is not 0! Tests may fail!")
            }
            QuadTree.clear()
        }
    }
}