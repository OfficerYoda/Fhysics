package de.officeryoda.fhysics.engine

import de.officeryoda.fhysics.engine.datastructures.spatial.BoundingBox
import de.officeryoda.fhysics.engine.datastructures.spatial.QuadTree
import de.officeryoda.fhysics.engine.math.Vector2
import de.officeryoda.fhysics.engine.objects.FhysicsObject
import de.officeryoda.fhysics.engine.util.Stopwatch
import de.officeryoda.fhysics.engine.util.times
import de.officeryoda.fhysics.rendering.FhysicsObjectDrawer
import de.officeryoda.fhysics.rendering.GravityType
import de.officeryoda.fhysics.rendering.SceneListener
import de.officeryoda.fhysics.rendering.UIController
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.math.max
import kotlin.math.sign

object FhysicsCore {

    // Constants
    val BORDER: BoundingBox = BoundingBox(0f, 0f, 100f, 100f) // x and y must be 0.0
    const val UPDATES_PER_SECOND: Int = 60 * 4
    const val SUB_STEPS: Int = 1
    private const val MAX_FRAMES_AT_CAPACITY: Int = 100
    private const val QTC_START_STEP_SIZE = 10.0
    val RENDER_LOCK = ReentrantLock()
    const val EPSILON: Float = 1E-4f

    // Variables
    private var objectCount: Int = 0
    var updateCount = 0 // Includes all sub steps

    var dt: Float = 1.0f / (UPDATES_PER_SECOND * SUB_STEPS)
    var running: Boolean = false
    val updateStopwatch = Stopwatch(50)

    // Quad tree capacity optimization
    val qtCapacity: MutableMap<Int, Double> = mutableMapOf()
    private var framesAtCapacity: Int = 0
    private var stepSize: Double = QTC_START_STEP_SIZE
    private var lastSample: Double = 0.0
    private var lastCapacity: Int = QuadTree.capacity
    private var objectsAtStepSizeIncrease: Int = 0

    init {
//        val objects: List<FhysicsObject> = List(7_000) { FhysicsObjectFactory.randomCircle() }
//        for (it: FhysicsObject in objects) {
//            it.restitution = 1f
//            it.frictionDynamic = 0f
//            it.frictionStatic = 0f
//        }
//        UIController.setBorderProperties(1f, 1f, 1f)
//        spawn(objects)

//        repeat(100) {
//            spawn(FhysicsObjectFactory.randomRectangle())
//        }
//
//        repeat(10) {
//            spawn(FhysicsObjectFactory.randomConcavePolygon())
//        }

        // Three rectangles that act as slides + ground rectangle
//        spawn(Rectangle(Vector2(75.0f, 75.0f), 45.0f, 5.0f, Math.toRadians(30.0).toFloat())).static = true
//        spawn(Rectangle(Vector2(30.0f, 50.0f), 45.0f, 5.0f, Math.toRadians(-30.0).toFloat())).static = true
//        spawn(Rectangle(Vector2(70.0f, 30.0f), 45.0f, 5.0f, Math.toRadians(30.0).toFloat())).static = true
//        spawn(Rectangle(Vector2(50.0f, 20.0f), 100.0f, 5.0f)).static = true

        // A Big rectangle in the center with an incline of 30 degrees and maximum friction values
//        spawn(Rectangle(Vector2(50.0f, 50.0f), 100.0f, 10.0f, Math.toRadians(30.0).toFloat())).first().apply {
//            static = true
//            frictionStatic = 1.0f
//            frictionDynamic = 1.0f
//            restitution = 0.0f
//        }
//        // A small rectangle on top of the big rectangle
//        spawn(Rectangle(Vector2(50.0f, 57.0f), 1.0f, 1.0f)).first().apply {
//            static = false
//            frictionStatic = 1.0f
//            frictionDynamic = 1.0f
//            restitution = 0.0f
//            angle = Math.toRadians(30.0).toFloat()
//        }

//        repeat(2000) {
//            val circle = FhysicsObjectFactory.randomCircle()
//            spawn(circle)
//        }

//        val rect1 = Rectangle(Vector2(50f, 0.5f), 1f, 1f)
//        spawn(rect1)
//        val rect2 = Rectangle(Vector2(55f, 4f), 12f, 5f)
//        spawn(rect1, rect2)

        // Create two circles
        // Create two rectangles
//        val rectA: Rectangle = Rectangle(Vector2(50f, 50f), 40f, 20f).apply {
//            velocity.set(Vector2(0f, 0f))
//            angle = 45f
//            angularVelocity = -1f
//            mass = 2f
//        }
//
//        val rectB: Rectangle = Rectangle(Vector2(65f, 35f), 10f, 10f).apply {
//            velocity.set(Vector2(0f, 0f))
//            mass = 2f
//        }

        // Adjust properties for perfectly elastic collision
//        setRestitution(1f, rectA, rectB)
//
//        spawn(rectA, rectB)

        objectsAtStepSizeIncrease = objectCount
    }


    fun startEverything() {
        Thread { FhysicsObjectDrawer().launch() }.start()
        startUpdateLoop()
    }

    private fun startUpdateLoop() {
        val updateIntervalMillis: Long = (1f / UPDATES_PER_SECOND * 1000).toLong()
        Timer("Fhysics-Core", true).scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                if (running) {
                    RENDER_LOCK.lock()
                    update()
                    RENDER_LOCK.unlock()
                }
            }
        }, 0, updateIntervalMillis)
    }

    fun update() {
        updateStopwatch.start()

        QuadTree.processPendingOperations()

        for (i: Int in 0 until SUB_STEPS) {
            QuadTree.updateFhysicsObjects()
            QuadTree.handleCollisions()

            SceneListener.pullObject()

            updateCount++
        }

        QuadTree.cleanup()

        if (UIController.optimizeQTCapacity) optimizeQuadTreeCapacity()

        updateStopwatch.stop()
    }

    fun spawn(vararg objects: FhysicsObject) {
        spawn(objects.toList())
    }

    private fun spawn(objects: List<FhysicsObject>) {
        for (obj: FhysicsObject in objects) {
            obj.updateBoundingBox()
            QuadTree.insert(obj)
        }
    }

    fun clear() {
        objectCount = 0
        QuadTree.clear()
    }

    // TODO remove all this optimization stuff and just use a fixed capacity
    private fun optimizeQuadTreeCapacity() {
        framesAtCapacity++
        if (framesAtCapacity > MAX_FRAMES_AT_CAPACITY) { // > and not >= to exclude the first frame where the rebuild takes place which takes longer
            val average: Double = updateStopwatch.average()

            qtCapacity[QuadTree.capacity] = average
            val newCapacity: Int = calculateNextQTCapacity()

            lastCapacity = QuadTree.capacity
            QuadTree.capacity = newCapacity
            lastSample = average
            framesAtCapacity = 0
        }
    }

    private fun calculateNextQTCapacity(): Int {
        // two samples are needed to calculate the next capacity
        if (qtCapacity.size < 2)
            return QuadTree.capacity + 5

        val crntSample: Double = qtCapacity[QuadTree.capacity]!!
        // if the capacity is increasing or decreasing
        val valueDir: Int = -(crntSample - lastSample).sign.toInt()
        // if the mspu is increasing or decreasing
        val capacityDir: Int = (QuadTree.capacity - lastCapacity).sign
        // the direction the capacity should change to get closer to the optimal capacity
        val totalDir: Int = valueDir * capacityDir

        // calculate the new capacity
        val newCapacity: Int = QuadTree.capacity + stepSize.toInt() * totalDir

        // if a lot of objects got spawned since the last time the stepSize was increased then increase the stepSize again
        if (objectCount - objectsAtStepSizeIncrease > 100) {
            stepSize = max(
                stepSize,
                QTC_START_STEP_SIZE * 0.75
            ) // increase stepSize to increase the speed of the capacity optimization (max to not decrease when it's higher at the start of the program)
            objectsAtStepSizeIncrease = objectCount
            qtCapacity.clear()
        } else {
            stepSize =
                max(1.0, stepSize - 0.5) // reduces stepSize by one every 2 updates
        }

        return newCapacity
    }

    fun gravityAt(pos: Vector2): Vector2 {
        if (UIController.gravityType == GravityType.DIRECTIONAL) {
            return UIController.gravityDirection
        } else {
            val direction: Vector2 = UIController.gravityPoint - pos
            val sqrDistance: Float =
                max(1f, direction.sqrMagnitude()) // Prevent high forces when the object is close to the gravity point
            return UIController.gravityPointStrength / sqrDistance * direction.normalized()
        }
    }

    fun nextId(): Int {
        return objectCount++
    }
}
