package de.officeryoda.fhysics.engine

import de.officeryoda.fhysics.engine.collision.CollisionInfo
import de.officeryoda.fhysics.engine.collision.CollisionSolver
import de.officeryoda.fhysics.engine.collision.ElasticCollision
import de.officeryoda.fhysics.objects.FhysicsObject
import de.officeryoda.fhysics.objects.FhysicsObjectFactory
import de.officeryoda.fhysics.objects.Rectangle
import java.awt.geom.Rectangle2D
import java.util.*
import java.util.Timer
import kotlin.math.max
import kotlin.math.sign

object FhysicsCore {

    /// =====constants=====
    // x and y must be 0.0
    val BORDER: Rectangle2D = Rectangle2D.Float(0.0F, 0.0F, 250.0F, 250.0F)
    private val COLLISION_SOLVER: CollisionSolver = ElasticCollision
    const val UPDATES_PER_SECOND: Int = 200
    private const val MAX_FRAMES_AT_CAPACITY: Int = 100

    /// =====variables=====
    var quadTree: QuadTree = QuadTree(BORDER, null)

    var objectCount: Int = 0
    val fhysicsObjects: MutableList<FhysicsObject> = ArrayList()
    private val borderRects: List<Rectangle>

    var updateCount = 0

    var dt: Float = 1.0F / UPDATES_PER_SECOND
    var running: Boolean = true
    val updateTimer = Timer(50)

    // quad tree capacity optimization
    val qtCapacity: MutableMap<Int, Double> = mutableMapOf()
    private var framesAtCapacity: Int = 0
    private val quadTreeTimer = Timer(MAX_FRAMES_AT_CAPACITY)

    init {
        borderRects = createBorderBoxes()
        quadTree.subdivide()

        for (i in 1..60000) {
            val circle = FhysicsObjectFactory.randomCircle()
//            circle.radius *= 2
            spawn(circle)
        }

        startUpdateLoop()
    }

    private fun startUpdateLoop() {
        val updateIntervalMillis: Int = (1f / UPDATES_PER_SECOND * 1000).toInt()
        Timer(true).scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                if (running) {
                    update()
                }
            }
        }, 0, updateIntervalMillis.toLong())
    }

    fun update() {
        updateTimer.start()

        spawnObject()

        quadTreeTimer.start()
        quadTree.updateObjectsAndRebuild()
        quadTreeTimer.stop()

        checkObjectCollision(quadTree)

        optimizeQuadTreeCapacity()

        updateCount++
        updateTimer.stop()
    }

    // This method must be called when trying to spawn an object
    fun spawn(obj: FhysicsObject) {
        fhysicsObjects.add(obj)
        quadTree.insert(obj)
    }

    private fun spawnObject() {
        for (i in 1..100) {
            if (objectCount < 60000) {
                spawn(FhysicsObjectFactory.randomCircle())
            }
        }
    }

    private fun checkObjectCollision(quadTree: QuadTree) {
        if (!quadTree.divided) {
            val objects = quadTree.objects
            val numObjects = objects.size

            for (i in 0 until numObjects - 1) {
                val objA = objects[i]

                for (j in i + 1 until numObjects) {
                    val objB = objects[j]
                    handleCollision(objA, objB)
                }
            }
        } else {
            checkObjectCollision(quadTree.topLeft!!)
            checkObjectCollision(quadTree.topRight!!)
            checkObjectCollision(quadTree.botLeft!!)
            checkObjectCollision(quadTree.botRight!!)
        }
    }

    private fun handleCollision(objA: FhysicsObject, objB: FhysicsObject) {
        val points: CollisionInfo = objA.testCollision(objB)

        if (points.overlap == -1.0F) return

        COLLISION_SOLVER.solveCollision(points)
    }

    fun checkBorderCollision(obj: FhysicsObject) {
        borderRects.forEach { handleCollision(obj, it) }
    }

    private fun createBorderBoxes(): List<Rectangle> {
        val width: Float = BORDER.width.toFloat()
        val height: Float = BORDER.height.toFloat()

        val left: Rectangle = FhysicsObjectFactory.customRectangle(Vector2(-width, 0.0F), width, height, Vector2.ZERO)
        val right: Rectangle = FhysicsObjectFactory.customRectangle(Vector2(width, 0.0F), width, height, Vector2.ZERO)
        val top: Rectangle =
            FhysicsObjectFactory.customRectangle(Vector2(-width, height), 3 * width, height, Vector2.ZERO)
        val bottom: Rectangle =
            FhysicsObjectFactory.customRectangle(Vector2(-width, -height), 3 * width, height, Vector2.ZERO)

        return listOf(left, right, top, bottom)
    }

    private var stepSize: Double = 10.0
    private var lastSample: Double = 0.0
    private var lastCapacity: Int = QuadTree.capacity

    private fun optimizeQuadTreeCapacity() {
        framesAtCapacity++
        if (framesAtCapacity > MAX_FRAMES_AT_CAPACITY) { // > and not >= to exclude the first frame which where the rebuild will take the longest
            val average: Double = updateTimer.average()
            quadTreeTimer.reset()

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

        val valueDir: Int = -(crntSample - lastSample).sign.toInt()
        val capacityDir: Int = (QuadTree.capacity - lastCapacity).sign
        val totalDir: Int = valueDir * capacityDir

        val newCapacity: Int = QuadTree.capacity + stepSize.toInt() * totalDir

        stepSize = max(1.0, stepSize - 0.5) // reduces stepSize by one every 2 updates because it's rounded to an int

        return newCapacity
    }

    fun nextId(): Int {
        return objectCount++
    }
}
