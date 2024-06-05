package de.officeryoda.fhysics.engine

import de.officeryoda.fhysics.engine.collision.CollisionInfo
import de.officeryoda.fhysics.engine.collision.CollisionSolver
import de.officeryoda.fhysics.engine.collision.ElasticCollision
import de.officeryoda.fhysics.engine.objects.Circle
import de.officeryoda.fhysics.engine.objects.FhysicsObject
import de.officeryoda.fhysics.engine.objects.FhysicsObjectFactory
import de.officeryoda.fhysics.engine.objects.Polygon
import de.officeryoda.fhysics.extensions.times
import de.officeryoda.fhysics.rendering.FhysicsObjectDrawer
import de.officeryoda.fhysics.rendering.GravityType
import de.officeryoda.fhysics.rendering.UIController
import java.util.*
import java.util.Timer
import java.util.concurrent.locks.ReentrantLock
import kotlin.math.max
import kotlin.math.sign

object FhysicsCore {

    /// =====constants=====
    val BORDER: BoundingBox = BoundingBox(0.0F, 0.0F, 100.0F, 100.0F) // x and y must be 0.0
    private val COLLISION_SOLVER: CollisionSolver = ElasticCollision
    const val UPDATES_PER_SECOND: Int = 120
    private const val MAX_FRAMES_AT_CAPACITY: Int = 100
    private const val QTC_START_STEP_SIZE = 10.0
    val RENDER_LOCK = ReentrantLock()

    /// =====variables=====
    private var quadTree: QuadTree = QuadTree(BORDER, null)

    private var objectCount: Int = 0

    var updateCount = 0

    var dt: Float = 1.0F / UPDATES_PER_SECOND
    var running: Boolean = true
    val updateTimer = Timer(50)

    // quad tree capacity optimization
    val qtCapacity: MutableMap<Int, Double> = mutableMapOf()
    private var framesAtCapacity: Int = 0
    private var stepSize: Double = QTC_START_STEP_SIZE
    private var lastSample: Double = 0.0
    private var lastCapacity: Int = QuadTree.capacity
    private var objectsAtStepSizeIncrease: Int = 0

    init {
//        for (i in 1..4000) {
//            val circle: Circle = FhysicsObjectFactory.randomCircle()
////            circle.velocity.set(Vector2.ZERO)
//            spawn(circle)
//        }
//
//        for (i in 1..20) {
//            val rect: Rectangle = FhysicsObjectFactory.randomRectangle()
//            spawn(rect)
//        }

        // spawn a rotated rectangle in the center
//        val rect =
//            Rectangle(Vector2((BORDER.width / 2), (BORDER.height / 2)), 30.0F, 10.0F, Math.toRadians(45.0).toFloat())
//        rect.velocity += Vector2(10f, 12f) * 2f
//        spawn(rect)

        // spawn a rectangle to the left and to the right
//        val rect2 = Rectangle(Vector2(20.0F, 50.0F), 10.0F, 10.0F)
//        rect2.velocity += Vector2(5f, 0f)
//        spawn(rect2)
//        val rect3 = Rectangle(Vector2(60.0F, 50.0F), 10.0F, 10.0F)
//        spawn(rect3)

        for (i in 1..10)
            spawn(FhysicsObjectFactory.randomConvexPolygon())

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
        updateTimer.start()

        quadTree.insertObjects()
        quadTree.updateObjectsAndRebuild()

        checkObjectCollision(quadTree)

        if (UIController.optimizeQTCapacity) optimizeQuadTreeCapacity()

        updateCount++
        updateTimer.stop()
    }

    fun spawn(obj: FhysicsObject) {
        QuadTree.toAdd.add(obj)
    }

    private fun checkObjectCollision(quadTree: QuadTree) {
        if (quadTree.divided) {
            checkObjectCollision(quadTree.topLeft!!)
            checkObjectCollision(quadTree.topRight!!)
            checkObjectCollision(quadTree.botLeft!!)
            checkObjectCollision(quadTree.botRight!!)
        } else {
            val objects: MutableList<FhysicsObject> = quadTree.objects
            val numObjects: Int = objects.size

            for (i: Int in 0 until numObjects - 1) {
                for (j: Int in i + 1 until numObjects) {
                    handleCollision(objects[i], objects[j])
                }
            }
        }
    }

    private fun handleCollision(objA: FhysicsObject, objB: FhysicsObject) {
        val points: CollisionInfo = objA.testCollision(objB)

        if (!points.hasCollision) return

        COLLISION_SOLVER.solveCollision(points)
    }

    fun checkBorderCollision(obj: FhysicsObject) {
        when (obj) {
            is Circle -> handleCircleBorderCollision(obj)
            is Polygon -> handlePolygonBorderCollision(obj)
        }
    }

    private fun handleCircleBorderCollision(obj: Circle) {
        when {
            obj.position.x - obj.radius < 0.0F -> {
                obj.velocity.x = -obj.velocity.x * UIController.wallElasticity
                obj.position.x = obj.radius
            }

            obj.position.x + obj.radius > BORDER.width -> {
                obj.velocity.x = -obj.velocity.x * UIController.wallElasticity
                obj.position.x = (BORDER.width - obj.radius)
            }
        }

        when {
            obj.position.y - obj.radius < 0.0F -> {
                obj.velocity.y = -obj.velocity.y * UIController.wallElasticity
                obj.position.y = obj.radius
            }

            obj.position.y + obj.radius > BORDER.height -> {
                obj.velocity.y = -obj.velocity.y * UIController.wallElasticity
                obj.position.y = (BORDER.height - obj.radius)
            }
        }
    }

    private fun handlePolygonBorderCollision(obj: Polygon) {
        val axesBorderProjection: List<Pair<Vector2, Projection>> = listOf(
            Pair(Vector2(-1f, 0f), Projection(Float.MIN_VALUE, BORDER.x)),
            Pair(Vector2(1f, 0f), Projection(BORDER.x + BORDER.width, Float.MAX_VALUE)),
            Pair(Vector2(0f, -1f), Projection(Float.MIN_VALUE, BORDER.y)),
            Pair(Vector2(0f, 1f), Projection(BORDER.y + BORDER.height, Float.MAX_VALUE))
        )

        axesBorderProjection.forEach { (axis: Vector2, borderProj: Projection) ->
            val projection: Projection = obj.project(axis)
            val projResult = ProjectionResult(projection, borderProj)

            if (projResult.hasOverlap) {
                val overlap: Float = projResult.getOverlap()

                obj.position -= axis * overlap
                when {
                    axis.x != 0f -> obj.velocity.x = -obj.velocity.x * UIController.wallElasticity
                    axis.y != 0f -> obj.velocity.y = -obj.velocity.y * UIController.wallElasticity
                }
            }
        }
    }

    private fun optimizeQuadTreeCapacity() {
        framesAtCapacity++
        if (framesAtCapacity > MAX_FRAMES_AT_CAPACITY) { // > and not >= to exclude the first frame where the rebuild takes place which takes longer
            val average: Double = updateTimer.average()

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
            val sqrDst: Float = UIController.gravityPoint.sqrDistance(pos)
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
