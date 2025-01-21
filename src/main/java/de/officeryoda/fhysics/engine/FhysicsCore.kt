package de.officeryoda.fhysics.engine

import de.officeryoda.fhysics.engine.Settings.SUB_STEPS
import de.officeryoda.fhysics.engine.Settings.UPDATES_PER_SECOND
import de.officeryoda.fhysics.engine.datastructures.BoundingBox
import de.officeryoda.fhysics.engine.datastructures.QuadTree
import de.officeryoda.fhysics.engine.math.Vector2
import de.officeryoda.fhysics.engine.objects.FhysicsObject
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

    /** A Thread lock to prevent simulating and rendering at the same time */
    val RENDER_LOCK = ReentrantLock()

    /** The Bounds of the simulation */
    var border: BoundingBox = BoundingBox(0f, 0f, 100f, 100f) // x and y must be 0.0

    /** The time step used for the simulation */
    var dt: Float = 1.0f / (UPDATES_PER_SECOND * SUB_STEPS) * Settings.timeScale

    /** The amount of updates that have been performed since the start of the simulation */
    var updateCount = 0 // Includes all sub steps

    /** Whether the simulation is running */
    var running: Boolean = true

    /** A stopwatch to measure the time per update */
    val updateStopwatch = Stopwatch()

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
