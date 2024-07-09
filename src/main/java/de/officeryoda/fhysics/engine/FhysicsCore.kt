package de.officeryoda.fhysics.engine

import de.officeryoda.fhysics.engine.objects.Circle
import de.officeryoda.fhysics.engine.objects.FhysicsObject
import de.officeryoda.fhysics.engine.objects.PolygonCreator
import de.officeryoda.fhysics.engine.objects.Rectangle
import de.officeryoda.fhysics.extensions.times
import de.officeryoda.fhysics.rendering.FhysicsObjectDrawer
import de.officeryoda.fhysics.rendering.GravityType
import de.officeryoda.fhysics.rendering.UIController
import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.math.max
import kotlin.math.sign

object FhysicsCore {

    // Constants
    val BORDER: BoundingBox = BoundingBox(0.0f, 0.0f, 100.0f, 100.0f) // x and y must be 0.0
    const val UPDATES_PER_SECOND: Int = 120
    private const val MAX_FRAMES_AT_CAPACITY: Int = 100
    private const val QTC_START_STEP_SIZE = 10.0
    val RENDER_LOCK = ReentrantLock()

    // Variables
    private var quadTree: QuadTree = QuadTree(BORDER, null)

    private var objectCount: Int = 0
    var updateCount = 0

    var dt: Float = 1.0f / UPDATES_PER_SECOND
    var running: Boolean = true
    val updateStopwatch = Stopwatch(50)

    // Quad tree capacity optimization
    val qtCapacity: MutableMap<Int, Double> = mutableMapOf()
    private var framesAtCapacity: Int = 0
    private var stepSize: Double = QTC_START_STEP_SIZE
    private var lastSample: Double = 0.0
    private var lastCapacity: Int = QuadTree.capacity
    private var objectsAtStepSizeIncrease: Int = 0

    init {
//        for (i in 1..30) {
//            spawn(FhysicsObjectFactory.randomCircle())
//        }
//
//        for (i in 1..20) {
//            spawn(FhysicsObjectFactory.randomRectangle())
//        }
//
//        for (i in 1..10) {
//            spawn(FhysicsObjectFactory.randomPolygon())
//        }

        // Two rectangles that act as slides
//        spawn(Rectangle(Vector2(75.0f, 75.0f), 45.0f, 5.0f, Math.toRadians(30.0).toFloat())).static = true
//        spawn(Rectangle(Vector2(30.0f, 50.0f), 45.0f, 5.0f, Math.toRadians(-30.0).toFloat())).static = true
        spawn(Rectangle(Vector2(50.0f, 20.0f), 100.0f, 5.0f)).static = true

        // Concave poly-circle fail case
//        val vertices = arrayOf(
//            Vector2(x = 50.0f, y = 50.0f),
//            Vector2(x = 55.0f, y = 70.0f),
//            Vector2(x = 50.0f, y = 60.0f),
//            Vector2(x = 45.0f, y = 70.0f),
//        )
//        spawn(PolygonCreator.createPolygon(vertices)).static = true

        // Spawn five circles in the top right
//        for (i: Int in 1..5) {
//            spawn(Circle(Vector2(90f - i * 5, 90f), 1f))
//        }

        // Just to add one to the color cycle
        QuadTree.removeQueue.add(spawn(Circle(Vector2(50f, 50f), 1f)))

        val vertices: Array<Vector2> = arrayOf(
            Vector2(0f, 0f),
            Vector2(5f, 0f),
            Vector2(5f, 3f),
            Vector2(0f, 2f),
            Vector2(-2f, 3.5f)
        )
        vertices.forEach { it += Vector2(50f, 22.5f) }
        spawn(PolygonCreator.createPolygon(vertices))

        objectsAtStepSizeIncrease = objectCount
    }

    fun startEverything() {
        Thread { FhysicsObjectDrawer().launch() }.start()
        startUpdateLoop()
    }

    private fun startUpdateLoop() {
        val updateIntervalMillis: Int = (1f / UPDATES_PER_SECOND * 1000).toInt()
        Timer(true).scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                if (running) {
                    RENDER_LOCK.lock()
                    update()
                    RENDER_LOCK.unlock()
                }
            }
        }, 0, updateIntervalMillis.toLong())
    }

    fun update() {
        updateStopwatch.start()

        quadTree.insertObjects()
        quadTree.updateObjectsAndRebuild()

//        checkObjectCollision(quadTree)
        quadTree.handleCollisions()

        if (UIController.optimizeQTCapacity) optimizeQuadTreeCapacity()

        updateCount++
        updateStopwatch.stop()
    }

    fun spawn(obj: FhysicsObject): FhysicsObject {
        QuadTree.toAdd.add(obj)
        return obj
    }

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
            val minDst = 0.05F
            val sqrDst: Float = UIController.gravityPoint.sqrDistanceTo(pos)
            if (sqrDst < minDst * minDst) {
                // to not fling objects away and to avoid objects getting stuck in the
                // gravity point causing it to vibrate and hitting other objects away
                return Vector2.ZERO
            }

            val direction: Vector2 = UIController.gravityPoint - pos
            return UIController.gravityPointStrength / sqrDst * direction.normalized()
        }
    }

    fun nextId(): Int {
        return objectCount++
    }
}
