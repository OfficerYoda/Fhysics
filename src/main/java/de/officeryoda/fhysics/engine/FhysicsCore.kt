package de.officeryoda.fhysics.engine

import de.officeryoda.fhysics.engine.datastructures.BoundingBox
import de.officeryoda.fhysics.engine.datastructures.QuadTree
import de.officeryoda.fhysics.engine.math.Vector2
import de.officeryoda.fhysics.engine.objects.FhysicsObject
import de.officeryoda.fhysics.engine.objects.Rectangle
import de.officeryoda.fhysics.engine.util.Stopwatch
import de.officeryoda.fhysics.engine.util.times
import de.officeryoda.fhysics.visual.Renderer
import de.officeryoda.fhysics.visual.SceneListener
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.math.max

/**
 * The core of the Fhysics engine.
 * This class contains all the important variables and functions for the simulation.
 */
object FhysicsCore {

    /** The Bounds of the simulation */
    var BORDER: BoundingBox = BoundingBox(0f, 0f, 100f, 100f) // x and y must be 0.0

    /** The amount of updates the simulation should perform per second */
    const val UPDATES_PER_SECOND: Int = 60

    /** The amount of sub steps the simulation performs per update */
    const val SUB_STEPS: Int = 4

    /** A Thread lock to prevent simulating and rendering at the same time */
    val RENDER_LOCK = ReentrantLock()

    /** A small value used for floating point comparisons */
    const val EPSILON: Float = 1E-4f

    /**
     * The amount of updates that have been performed since the start of the simulation.
     */
    var updateCount = 0 // Includes all sub steps

    /** The time step used for the simulation */
    var dt: Float = 1.0f / (UPDATES_PER_SECOND * SUB_STEPS) * Settings.timeScale

    /** Whether the simulation is running */
    var running: Boolean = true

    /** A stopwatch to measure the time per update */
    val updateStopwatch = Stopwatch()

    init {
//        val objects: List<FhysicsObject> = List(50) { FhysicsObjectFactory.randomCircle() }
//        for (it: FhysicsObject in objects) {
//            it.restitution = 1f
//            it.frictionStatic = 0f
//            it.frictionDynamic = 0f
//            it.velocity *= 0.45f
//        }
//        spawn(objects)
        Settings.setBorderProperties(1f, 1f, 1f)

//        repeat(10) {
//            spawn(FhysicsObjectFactory.randomRectangle())
//        }
//
//        repeat(10) {
//            spawn(FhysicsObjectFactory.randomPolygon())
//        }

        // Three rectangles that act as slides + ground rectangle
//        spawn(Rectangle(Vector2(75.0f, 75.0f), 45.0f, 5.0f, Math.toRadians(30.0).toFloat())).first().static = true
//        spawn(Rectangle(Vector2(30.0f, 50.0f), 45.0f, 5.0f, Math.toRadians(-30.0).toFloat())).first().static = true
//        spawn(Rectangle(Vector2(70.0f, 30.0f), 45.0f, 5.0f, Math.toRadians(30.0).toFloat())).first().static = true
//        spawn(Rectangle(Vector2(50.0f, 20.0f), 100.0f, 5.0f)).first().static = true

        // A Big rectangle in the center with an incline of 30 degrees and maximum friction values
//        spawn(Rectangle(Vector2(50.0f, 50.0f), 100.0f, 10.0f, Math.toRadians(30.0).toFloat())).first().apply {
//            static = true
//            frictionStatic = 1.0f
//            frictionDynamic = 1.0f
//            restitution = 0.0f
//        }

        val rectA: Rectangle = Rectangle(Vector2(55.5f, 48.75f), 10f, 5f).apply {
            velocity.set(Vector2(10f, 0f))
            restitution = 1f
            frictionStatic = 0f
            frictionDynamic = 0f
            mass = 2f
        }

        val rectB: Rectangle = Rectangle(Vector2(75f, 51.25f), 10f, 5f).apply {
            velocity.set(Vector2(-10f, 0f))
            restitution = 1f
            frictionStatic = 0f
            frictionDynamic = 0f
            mass = 2f
        }
        spawn(rectA, rectB)

        // So objects get rendered even when the simulation starts paused
        QuadTree.processPendingOperations()
    }

    fun startEverything() {
        // Start the rendering thread
        Thread { Renderer().launch() }.start()
        // Start the simulation
        startUpdateLoop()
    }

    private fun startUpdateLoop() {
        val updateIntervalMillis: Long = (1f / UPDATES_PER_SECOND * 1000).toLong()

        Timer("Fhysics-Core", true).scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                if (running) {
                    // Don't render while updating to prevent concurrentModificationExceptions
                    RENDER_LOCK.lock()
                    update()
                    RENDER_LOCK.unlock()
                }
            }
        }, 0, updateIntervalMillis)
    }

    /**
     * Updates the simulation by one step.
     */
    fun update() {
        updateStopwatch.start()

        // Load a new scene if requested
        SceneManager.loadPendingScene()

        // Rebuild must happen before processing pending operations
        QuadTree.rebuild()
        QuadTree.processPendingOperations()

        // Update objects multiple times per update
        repeat(SUB_STEPS) {
            QuadTree.update()

            SceneListener.pullObject()

            updateCount++
        }

        updateStopwatch.stop()
    }

    fun spawn(vararg objects: FhysicsObject): List<FhysicsObject> {
        return spawn(objects.toList())
    }

    private fun spawn(objects: List<FhysicsObject>): List<FhysicsObject> {
        for (obj: FhysicsObject in objects) {
            QuadTree.insert(obj)
        }

        return objects
    }

    /**
     * Calculates the gravity at the given [position][pos].
     */
    fun gravityAt(pos: Vector2): Vector2 {
        if (Settings.gravityType == GravityType.DIRECTIONAL) {
            return Settings.gravityDirection
        } else {
            val direction: Vector2 = Settings.gravityPoint - pos
            val sqrDistance: Float =
                max(1f, direction.sqrMagnitude()) // Prevent high forces when the object is close to the gravity point
            return Settings.gravityPointStrength / sqrDistance * direction.normalized()
        }
    }
}
