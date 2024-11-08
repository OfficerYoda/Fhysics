package de.officeryoda.fhysics.engine.datastructures

import de.officeryoda.fhysics.engine.FhysicsCore
import de.officeryoda.fhysics.engine.math.BoundingBox
import de.officeryoda.fhysics.engine.math.Vector2
import de.officeryoda.fhysics.engine.objects.FhysicsObject
import de.officeryoda.fhysics.rendering.FhysicsObjectDrawer
import java.util.concurrent.Executors

object QuadTree {

    // Capacity of the tree
    var capacity: Int = 4
        set(value) {
            field = value.coerceAtLeast(1)
        }

    // Pool of nodes to prevent excessive object creation
    private val nodePool: ObjectPool<QuadTreeNode> = ObjectPool()

    // Thread pool for parallelizing the tree processes
    private val threadPool = Executors.newFixedThreadPool(4)

    // List of objects to add, used to queue up insertions and prevent concurrent modification
    private val pendingAdditions: MutableList<FhysicsObject> = ArrayList()

    // Set of objects to remove, used to mark objects for deletion safely
    private val pendingRemovals: MutableSet<FhysicsObject> = HashSet()

    // Used to prevent concurrent modification exceptions
    var divideNextUpdate: Boolean = false

    // The root node of the tree
    private var root: QuadTreeNode = QuadTreeNode(FhysicsCore.BORDER, null)

    fun query(pos: Vector2): FhysicsObject? {
        return root.query(pos)
    }

    fun insert(obj: FhysicsObject) {
        pendingAdditions.add(obj)
    }

    fun remove(obj: FhysicsObject) {
        pendingRemovals.add(obj)
    }

    fun insertPendingAdditions() {
        pendingAdditions.forEach { root.insert(it) }
        pendingAdditions.clear()
    }

    fun rebuild() {
        // If the capacity was changed, divideNextUpdate will be true and the root should try to divide
        if (divideNextUpdate) {
            root.tryDivide()
            divideNextUpdate = false
        }

        root.rebuild()
    }

    fun clear() {
        root = QuadTreeNode(FhysicsCore.BORDER, null)
    }

    fun updateFhysicsObjects() {
        root.updateFhysicsObjects()
    }

    fun handleCollisions() {
        root.handleCollisions()
    }

    fun drawObjects(drawer: FhysicsObjectDrawer) {
        root.drawObjects(drawer)
    }

    fun drawNodes(drawer: FhysicsObjectDrawer) {
        root.drawNode(drawer)
    }

    fun updateNodeSizes() {
        root.updateNodeSizes(isTop = true, isLeft = true)
    }

    fun getNodeFromPool(): QuadTreeNode {
        // TODO: Check if the node pool is working correctly
        // Try to get a node from the pool, if it's null, create a new one
        return nodePool.borrowObject() ?: QuadTreeNode(BoundingBox(0f, 0f, 0f, 0f), null)
    }

    fun addNodeToPool(node: QuadTreeNode) {
        node.clear()
        nodePool.returnObject(node)
    }

    fun getObjectCount(): Int {
        return root.countUnique()
    }

    fun getPendingRemovals(): MutableSet<FhysicsObject> {
        return pendingRemovals
    }

    fun shutdownThreadPool() {
        println("Shutting down thread pool")
        threadPool.shutdownNow()
    }

    override fun toString(): String {
        return root.toString()
    }
}