package de.officeryoda.fhysics.engine.datastructures

import de.officeryoda.fhysics.engine.FhysicsCore.border
import de.officeryoda.fhysics.engine.Settings
import de.officeryoda.fhysics.engine.collision.CollisionInfo
import de.officeryoda.fhysics.engine.collision.CollisionSolver
import de.officeryoda.fhysics.engine.datastructures.QuadTree.processPendingOperations
import de.officeryoda.fhysics.engine.math.Vector2
import de.officeryoda.fhysics.engine.objects.FhysicsObject
import de.officeryoda.fhysics.visual.DebugRenderer
import de.officeryoda.fhysics.visual.Renderer
import de.officeryoda.fhysics.visual.SceneListener
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import kotlin.collections.ArrayDeque
import kotlin.math.min

/**
 * A QuadTree data structure for efficient spatial partitioning and collision detection.
 *
 * The QuadTree is a tree data structure in which a node is split into four children
 * whenever it contains more objects than a given capacity.
 */
object QuadTree {
    /** The capacity of a node in the QuadTree */
    var capacity: Int = 16
        set(value) {
            field = value.coerceAtLeast(2)
        }

    /** The minimum size of a node */
    private const val MIN_SIZE: Float = 1f

    /** A list of lists of objects in the leaf nodes */
    private val objectLists = IndexedFreeList<MutableList<FhysicsObject>>()

    /** The root node of the QuadTree */
    private var root: QTNode = QTNode(border)

    /** List of all nodes in the tree (root node is always at index 0) */
    private val nodes = IndexedFreeList<QTNode>(root)

    /** A flag indicating whether the QuadTree should be cleared. */
    var clearFlag: Boolean = false

    /** A flag indicating whether the QuadTree should be rebuilt. */
    var rebuildFlag: Boolean = false

    /** List of objects to add, used to queue up insertions and prevent concurrent modification */
    private val pendingAdditions: MutableList<FhysicsObject> = ArrayList()

    /** List of objects to remove, used to mark objects for deletion safely */
    val pendingRemovals: MutableList<FhysicsObject> = ArrayList()

    private val threadPool = Executors.newFixedThreadPool(4) // 4 Threads showed best performance

    /// region =====QuadTree Operations=====
    /// region =====Insertion=====
    /**
     * Inserts an [object][obj] into the QuadTree.
     *
     * Insertion will happen the next time [processPendingOperations] is called.
     */
    fun insert(obj: FhysicsObject) {
        obj.updateBoundingBox()
        pendingAdditions.add(obj)
    }

    private fun insertPending() {
        for (it: FhysicsObject in pendingAdditions) {
            insertIteratively(it)
        }

        pendingAdditions.clear()
    }

    private fun insertIteratively(obj: FhysicsObject, startNode: QTNode = root) {
        val overlappingLeaves: MutableList<QTNode> = findOverlappingLeaves(obj.boundingBox, startNode)

        for (leave: QTNode in overlappingLeaves) {
            insertIntoLeaf(obj, leave)
        }
    }

    /**
     * Finds all leaf nodes in the QuadTree that overlap with a given [area]
     * and are descendants of the [startNode] (default is the root).
     */
    private fun findOverlappingLeaves(area: BoundingBox, startNode: QTNode = root): MutableList<QTNode> {
        val leaves: MutableList<QTNode> = mutableListOf() // List to store leaf nodes
        val toProcess: ArrayDeque<QTNode> = ArrayDeque() // Queue to manage nodes to process
        toProcess.add(startNode)

        while (toProcess.isNotEmpty()) {
            val node: QTNode = toProcess.removeFirst()

            if (node.isLeaf) {
                leaves.add(node)
            } else {
                addOverlappingChildrenToQueue(node, area, toProcess)
            }
        }

        return leaves
    }

    /**
     * Adds the children of a [node] that overlap with a given [area] to a [queue] of nodes.
     */
    private fun addOverlappingChildrenToQueue(node: QTNode, area: BoundingBox, queue: ArrayDeque<QTNode>) {
        val nodeBbox: BoundingBox = node.bbox
        val cx: Float = nodeBbox.x + nodeBbox.width / 2 // Center X
        val cy: Float = nodeBbox.y + nodeBbox.height / 2 // Center Y

        // Get the index of the first child
        val childIndex: Int = node.index

        // Check which children overlap the bounding box
        if (area.y + area.height > cy) { // Top edge intersects or is above the center
            if (area.x < cx) { // Left edge intersects or is left of the center
                queue.add(nodes[childIndex + 0]) // Top-left
            }
            if (area.x + area.width > cx) { // Right edge intersects or is right of the center
                queue.add(nodes[childIndex + 1]) // Top-right
            }
        }
        if (area.y < cy) { // Bottom edge intersects or is below the center
            if (area.x < cx) { // Left edge intersects or is left of the center
                queue.add(nodes[childIndex + 2]) // Bottom-left
            }
            if (area.x + area.width > cx) { // Right edge intersects or is right of the center
                queue.add(nodes[childIndex + 3]) // Bottom-right
            }
        }
    }

    private fun insertIntoLeaf(obj: FhysicsObject, node: QTNode) {
        // Check if the object is already in the node (id is unique)
        if (node.objects.any { it.id == obj.id }) return

        node.objects.add(obj)

        trySplitNode(node)
    }
    /// endregion

    /// region =====Removal=====
    /**
     * Removes an [object][obj] from the QuadTree.
     *
     * Removal will happen the next time [processPendingOperations] is called.
     */
    fun remove(obj: FhysicsObject) {
        pendingRemovals.add(obj)
    }

    private fun removePending() {
        for (obj: FhysicsObject in pendingRemovals) {
            removeIteratively(obj)
        }

        pendingRemovals.clear()
    }

    private fun removeIteratively(obj: FhysicsObject) {
        val overlappingLeaves: MutableList<QTNode> = findOverlappingLeaves(obj.boundingBox)

        for (leaf: QTNode in overlappingLeaves) {
            leaf.objects.remove(obj)
        }
    }
    /// endregion

    /// region =====Query=====
    /**
     * Queries the QuadTree for an object at a given [position][pos].
     *
     * Returns null if no object is found.
     */
    fun query(pos: Vector2): FhysicsObject? {
        val leaf: QTNode = getLeafNode(pos)
        val objectHit: FhysicsObject? = queryLeafObjects(leaf, pos)
        return objectHit
    }

    /**
     * Returns the leaf node containing the given [position][pos].
     */
    private fun getLeafNode(pos: Vector2): QTNode {
        // Traverse the tree until the leaf node containing the position is found
        var node: QTNode = root
        while (true) {
            if (node.isLeaf) return node

            val nodeBbox: BoundingBox = node.bbox
            val cx: Float = nodeBbox.x + nodeBbox.width / 2 // Center X
            val cy: Float = nodeBbox.y + nodeBbox.height / 2 // Center Y

            // Get the index of the first child
            val childIndex: Int = node.index

            // Check which child node contains the position
            node = if (pos.y >= cy) {
                if (pos.x <= cx) nodes[childIndex] // Top-left
                else nodes[childIndex + 1] // Top-right
            } else {
                if (pos.x <= cx) nodes[childIndex + 2] // Bottom-left
                else nodes[childIndex + 3] // Bottom-right
            }
        }
    }

    /**
     * Queries the objects in a leaf node for an object at a given [position][pos].
     *
     * Returns null if no object is found.
     */
    private fun queryLeafObjects(node: QTNode, pos: Vector2): FhysicsObject? {
        for (obj: FhysicsObject in node.objects) {
            if (!obj.boundingBox.contains(pos)) continue
            if (obj.contains(pos)) {
                return obj
            }
        }

        // No object found
        return null
    }
    /// endregion

    /// region =====Splitting=====
    private fun trySplitNode(parent: QTNode) {
        if (shouldSplitNode(parent)) {
            val firstChildIdx: Int = createChildNodes(parent.bbox)
            moveObjectsToChildren(parent, firstChildIdx)
            convertToBranch(parent, firstChildIdx)
        }
    }

    private fun shouldSplitNode(node: QTNode): Boolean {
        // Objects in node has exceeded capacity and the node is larger than the minimum size
        return node.objects.count() > capacity && min(node.bbox.width, node.bbox.height) > MIN_SIZE
    }

    /**
     * Creates child nodes for a given [parentBbox] and returns the index of the first child node.
     */
    private fun createChildNodes(parentBbox: BoundingBox): Int {
        val cx: Float = parentBbox.x + parentBbox.width / 2 // Center X
        val cy: Float = parentBbox.y + parentBbox.height / 2 // Center Y
        val hw: Float = parentBbox.width / 2 // Half width
        val hh: Float = parentBbox.height / 2 // Half height

        // Create child nodes
        val tl = QTNode(BoundingBox(parentBbox.x, cy, hw, hh))
        val tr = QTNode(BoundingBox(cx, cy, hw, hh))
        val bl = QTNode(BoundingBox(parentBbox.x, parentBbox.y, hw, hh))
        val br = QTNode(BoundingBox(cx, parentBbox.y, hw, hh))

        // Insert in the correct order
        val firstNodeIndex: Int = nodes.add(tl)
        nodes.add(tr)
        nodes.add(bl)
        nodes.add(br)

        return firstNodeIndex
    }

    /**
     * Moves objects from the [parent] node to its children.
     */
    private fun moveObjectsToChildren(parent: QTNode, firstChildIdx: Int) {
        for (i: Int in 0..3) {
            val child: QTNode = nodes[firstChildIdx + i]
            insertOverlappingObjects(parent, child)
        }
    }

    /**
     * Inserts objects from the [parent] node that overlap with the [child] node into the child.
     */
    private fun insertOverlappingObjects(parent: QTNode, child: QTNode) {
        for (obj: FhysicsObject in parent.objects) {
            if (child.bbox.overlaps(obj.boundingBox)) {
                insertIntoLeaf(obj, child)
            }
        }
    }

    /**
     * Converts a [node] to a branch node and sets the [firstChildIdx] as the first child node.
     */
    private fun convertToBranch(node: QTNode, firstChildIdx: Int) {
        node.convertToBranch()
        node.index = firstChildIdx
    }
    /// endregion
    /// endregion

    /// region =====Fhysics Operations=====
    /**
     * Processes all pending operations in the QuadTree.
     * This includes inserting and removing objects and doing a total rebuild if necessary.
     */
    fun processPendingOperations() {
        if (rebuildFlag) {
            rebuildFlag = false
            totalRebuild()
        }
        if (clearFlag) {
            clearFlag = false
            clear()
        }
        insertPending()
        removePending()
    }

    /**
     * Rebuilds the QuadTree from scratch.
     */
    private fun totalRebuild() {
        // Store all objects in a temporary list
        val tempObjects: List<FhysicsObject> = nodes.filter { it.isLeaf }.flatMap { it.objects }
        clear()
        pendingAdditions.addAll(tempObjects)
        insertPending()
    }

    fun clear() {
        root = QTNode(border)
        nodes.clear()
        nodes.add(root)
        SceneListener.clearSelection()
    }

    /**
     * Updates the objects, checks for collisions and updates the QuadTree structure.
     */
    fun update() {
        val leaves: List<QTNode> = nodes.filter { it.isLeaf }

        // Update all leaves in parallel
        threadPool.invokeAll(leaves.map { node ->
            Callable {
                updateLeaf(node)
            }
        })
    }

    private fun updateLeaf(node: QTNode) {
        val objectsInLeaf: MutableList<FhysicsObject> = node.objects
        updateFhysicsObjects(objectsInLeaf)
        handleCollisions(objectsInLeaf)
    }

    /**
     * Updates the [objects] in the QuadTree.
     */
    private fun updateFhysicsObjects(objects: MutableList<FhysicsObject>) {
        for (obj: FhysicsObject in objects) {
            obj.update()
        }
    }

    /**
     * Efficiently rebuilds the QuadTree.
     *
     * This method traverses the QuadTree using a stack to avoid recursion.
     * It collects objects that are not fully contained in their leaf nodes into a rebuild list.
     * As soon as the objects in the rebuild list are fully contained in a node, they are reinserted
     * starting from that node.
     *
     * A recursive pseudocode implementation of the algorithm would look like this:
     * ```
     * function rebuildRecursive(node):
     *     // A list of objects that need to be reinserted
     *     rebuildList = empty list
     *
     *     if node is leaf:
     *         for each object in node:
     *             if node does not completely contain object:
     *                 remove object from node
     *                 add object to rebuildList
     *     else:
     *         // Process children recursively
     *         for each child in child-nodes:
     *             childRebuildList = rebuildRecursive(child)
     *             add childRebuildList to rebuildList
     *
     *         // Try to reinsert the objects in the rebuild list
     *         for each object in rebuildList:
     *             if node completely contains object:
     *                 insert object into node
     *                 remove object from rebuildList
     *
     *     return rebuildList
     * ```
     */
    fun rebuild() {
        val stack = ArrayDeque<QTNode>()
        val visited: MutableSet<QTNode> = mutableSetOf<QTNode>()
        // A list of object indices that aren't fully contained in their leaf nodes
        val rebuildList: MutableList<FhysicsObject> = LinkedList<FhysicsObject>()

        stack.add(root)

        while (stack.isNotEmpty()) {
            val current: QTNode = stack.last()

            // If it's a leaf node, add objects that aren't fully contained in the node to the rebuild list
            if (current.isLeaf) {
                addNotContainedToList(current, rebuildList)
                stack.removeLast()
                continue
            }

            // If the node hasn't been visited yet, mark it as visited and add its children
            if (!visited.contains(current)) {
                visited.add(current)
                val children: Array<QTNode> = getChildren(current)
                stack.addAll(children)
            } else {
                // After visiting children, process and pop the branch
                tryInsertRebuildList(rebuildList, current)
                tryCollapseBranch(current)
                stack.removeLast()
            }
        }

        // The remaining objects are slightly out of bounds and need to be reinserted
        for (obj: FhysicsObject in rebuildList) {
            // Move them in bounds and reinsert
            CollisionSolver.moveInsideBorder(obj)
            insertIteratively(obj)
        }
    }

    /**
     * Checks for and solves collisions between the [objects].
     * Also handles collisions with the border.
     */
    private fun handleCollisions(objects: MutableList<FhysicsObject>) {
        for (i: Int in 0 until objects.size) {
            // Check for collisions with other objects
            val obj: FhysicsObject = objects[i]
            for (j: Int in i + 1 until objects.size) {
                val other: FhysicsObject = objects[j]

                val info: CollisionInfo = obj.testCollision(other)
                if (info.hasCollision) {
                    CollisionSolver.solveCollision(info)
                }
            }

            // Can collide with border because all possible object collisions have been checked
            CollisionSolver.handleBorderCollisions(obj)
        }
    }

    /**
     * Adds objects that aren't fully contained in the [leaf node][node] to the [list].
     */
    private fun addNotContainedToList(node: QTNode, list: MutableList<FhysicsObject>) {
        val iterator: MutableIterator<FhysicsObject> = node.objects.iterator()
        while (iterator.hasNext()) {
            val obj: FhysicsObject = iterator.next()
            if (node.bbox.contains(obj.boundingBox)) continue
            iterator.remove()
            list.add(obj)
        }
    }

    /**
     * Inserts objects from the [rebuildList] into the [node] if they are contained in the node's bounding box.
     */
    private fun tryInsertRebuildList(rebuildList: MutableList<FhysicsObject>, node: QTNode) {
        if (rebuildList.isEmpty()) return

        val iterator: MutableIterator<FhysicsObject> = rebuildList.iterator()
        while (iterator.hasNext()) {
            val obj: FhysicsObject = iterator.next()
            val objBbox: BoundingBox = obj.boundingBox

            if (node.bbox.contains(objBbox)) {
                insertIteratively(obj, node)
                iterator.remove()
            }
        }
    }

    /**
     * Tries to collapse the children of the [node] into the node if the
     * children contain fewer objects than the capacity.
     */
    private fun tryCollapseBranch(node: QTNode) {
        if (node.isLeaf) return

        var objectsInChildren: Int = getObjectCountInChildren(node)
        if (objectsInChildren == -1) return // Node has branch children, don't collapse

        if (objectsInChildren <= capacity) {
            collapseBranch(node)
        }
    }

    /**
     * Collapses the leaf children of a [node] into the node.
     */
    private fun collapseBranch(node: QTNode) {
        val firstChildIdx: Int = node.index
        node.convertToLeaf()

        // Free in reverse so that the first child is freed last and free indices are reused in the correct order
        for (i: Int in 3 downTo 0) {
            val child: QTNode = nodes[firstChildIdx + i]
            moveObjects(child, node)

            // Free the child and its objects
            nodes.free(firstChildIdx + i)
            objectLists.free(child.index)
        }
    }

    /**
     * Adds all objects from [fromNode] to [toNode].
     */
    private fun moveObjects(fromNode: QTNode, toNode: QTNode) {
        for (obj: FhysicsObject in fromNode.objects) {
            insertIntoLeaf(obj, toNode)
        }

        fromNode.objects.clear()
    }

    /**
     * Returns the number of objects in the children of the [node]
     * or -1 if the node has at least one branch child.
     */
    fun getObjectCountInChildren(node: QTNode): Int {
        val children: Array<QTNode> = getChildren(node)

        var objectCount = 0
        for (child: QTNode in children) {
            if (!child.isLeaf) {
                // Node has branch children, don't collapse
                return -1
            }
            objectCount += child.objects.count()
        }

        return objectCount
    }

    private fun getChildren(node: QTNode): Array<QTNode> {
        val firstChildIdx: Int = node.index
        return Array(4) { i -> nodes[firstChildIdx + i] }
    }

    fun shutdownThreadPool() {
        println("Shutting down thread pool")
        threadPool.shutdownNow()
    }
    /// endregion

    /// region =====Rendering=====
    fun drawObjects(renderer: Renderer) {
        // Get all objects
        val objects: Sequence<FhysicsObject> = nodes.asSequence()
            .filter { it.isLeaf }
            .flatMap { it.objects }

        for (obj: FhysicsObject in objects) {
            // Only draw objects that are visible
            if (obj.boundingBox.overlaps(renderer.viewingFrustum)) {
                obj.draw(renderer)
            }
        }

        if (Settings.showBoundingBoxes) {
            for (obj: FhysicsObject in objects) {
                // Only draw bounding boxes that are visible
                if (obj.boundingBox.overlaps(renderer.viewingFrustum)) {
                    DebugRenderer.drawBoundingBox(obj.boundingBox)
                }
            }
        }
    }

    fun drawNodes(viewingFrustum: BoundingBox) {
        // Only drawing leaf nodes is enough
        val leaves: List<QTNode> = nodes.filter { it.isLeaf }
        for (leaf: QTNode in leaves) {
            // Only draw nodes that are visible
            if (leaf.bbox.overlaps(viewingFrustum)) {
                DebugRenderer.drawQTNode(leaf.bbox, leaf.objects.count())
            }
        }
    }
    /// endregion

    /// region =====Other=====
    /**
     * Returns the number of objects in the QuadTree.
     */
    fun getObjectCount(): Int {
        return nodes.filter { it.isLeaf }.flatMap { it.objects }.toSet().count()
    }
    /// endregion

    /// region =====Inner Classes=====
    /**
     * A class representing a node in the QuadTree.
     */
    data class QTNode(
        /**
         * The bounding box of the node.
         */
        val bbox: BoundingBox,
    ) {
        /**
         * Points to the first child node in [QuadTree.nodes] when the node is a branch
         * or to the list in [QuadTree.objectLists] containing the objects when the node is a leaf.
         *
         * Child nodes are stored in blocks of 4 in the order: top left, top right, bottom left, bottom right.
         */
        var index: Int = -1

        /** The list of objects in the node */
        val objects: MutableList<FhysicsObject> get() = objectLists[index]

        /** Whether the node is a leaf node */
        var isLeaf: Boolean = true
            private set

        /**
         * Converts the node to a leaf node.
         */
        fun convertToLeaf() {
            if (!isLeaf) {
                isLeaf = true
                index = objectLists.add(mutableListOf())
            }
        }

        /**
         * Converts the node to a branch node.
         */
        fun convertToBranch() {
            if (isLeaf) {
                isLeaf = false
                objectLists.free(index)
            }
        }

        init {
            // New QTNode is always a leaf
            index = objectLists.add(mutableListOf())
        }
    }

    /// region =====Debug=====
    object QTDebugHelper {
        val root: QTNode get() = QuadTree.root

        fun printTree() {
            printlnNode(root)
            getChildNodes(root).forEachIndexed { index, it ->
                printTree(it, "", index == 0)
            }
        }

        private fun printTree(node: QTNode, indent: String, left: Boolean) {
            print(indent)
            print(if (left) "\u251C " else "\u2514 ")

            printlnNode(node)
            getChildNodes(node).forEach {
                val newIndent: String = indent + (if (left) "\u2502 " else "  ")
                printTree(it, newIndent, true)
            }
        }

        private fun printlnNode(node: QTNode) {
            println("QTNode(isLeaf=${node.isLeaf}, index=${node.index}, bbox=${node.bbox})")
        }

        private fun getChildNodes(parent: QTNode): MutableList<QTNode> {
            if (parent.isLeaf) return mutableListOf()

            val children: MutableList<QTNode> = mutableListOf()
            addChildNodesToCollection(parent.index, children)

            return children
        }

        private fun addChildNodesToCollection(
            firstChildIdx: Int,
            collection: MutableCollection<QTNode>,
        ) {
            collection.add(nodes[firstChildIdx + 0]) // Top-left
            collection.add(nodes[firstChildIdx + 1]) // Top-right
            collection.add(nodes[firstChildIdx + 2]) // Bottom-left
            collection.add(nodes[firstChildIdx + 3]) // Bottom-right
        }

        fun getChildren(node: QTNode): Array<QTNode> {
            return QuadTree.getChildren(node)
        }
    }
    /// endregion
    /// endregion
}
