package de.officeryoda.fhysics.engine.datastructures.spatial

import de.officeryoda.fhysics.engine.FhysicsCore.BORDER
import de.officeryoda.fhysics.engine.collision.CollisionInfo
import de.officeryoda.fhysics.engine.collision.CollisionSolver
import de.officeryoda.fhysics.engine.datastructures.IndexedFreeList
import de.officeryoda.fhysics.engine.datastructures.spatial.QuadTree.processPendingOperations
import de.officeryoda.fhysics.engine.math.Vector2
import de.officeryoda.fhysics.engine.objects.FhysicsObject
import de.officeryoda.fhysics.rendering.DebugDrawer
import de.officeryoda.fhysics.rendering.FhysicsObjectDrawer
import de.officeryoda.fhysics.rendering.UIController
import kotlin.math.min

object QuadTree {
    // Capacity of the tree
    var capacity: Int = 8
        set(value) {
            field = value.coerceAtLeast(1)
        }

    private const val MIN_SIZE: Float = 1f  // Minimum size of a node

    // The root node of the tree
    private var root: QTNode = QTNode(BORDER)

    // List of all nodes in the tree (root node is always at index 0)
    private val nodes = IndexedFreeList<QTNode>(root)

    // List of all elements in the tree (elements store the index of the object in QuadTree.objects)
    // This is done, so that objects are only stored once in the tree and can be referenced multiple times by different elements
    private val elements = IndexedFreeList<QTNodeElement>()

    // List of all objects in the tree
    private val objects = IndexedFreeList<FhysicsObject>()

    /** A flag indicating whether the QuadTree should be rebuilt. */
    var rebuild: Boolean = false

    // List of objects to add, used to queue up insertions and prevent concurrent modification
    private val pendingAdditions: MutableList<FhysicsObject> = ArrayList()

    // Set of objects to remove, used to mark objects for deletion safely
    val pendingRemovals: MutableList<FhysicsObject> = ArrayList()

    /// region =====QuadTree Operations=====
    /// region =====Insertion=====
    /**
     * Inserts an [object][obj] into the QuadTree.
     *
     * Insertion will happen the next time [processPendingOperations] is called.
     */
    fun insert(obj: FhysicsObject) {
        pendingAdditions.add(obj)
    }

    private fun insertPending() {
        for (it: FhysicsObject in pendingAdditions) {
            insertIteratively(it)
        }

        pendingAdditions.clear()
    }

    private fun insertIteratively(obj: FhysicsObject) {
        val objIdx: Int = objects.add(obj)
        insertIteratively(objIdx, obj.boundingBox)
    }

    private fun insertIteratively(objIdx: Int, bbox: BoundingBox) {
        val overlappingLeaves: MutableList<QTNode> = findOverlappingLeaves(bbox)
        for (leave: QTNode in overlappingLeaves) {
            insertIntoLeaf(objIdx, leave)
        }
    }

    private fun findOverlappingLeaves(area: BoundingBox): MutableList<QTNode> {
        val leaves: MutableList<QTNode> = mutableListOf() // List to store leaf nodes
        val toProcess: ArrayDeque<QTNode> = ArrayDeque() // Queue to manage nodes to process
        toProcess.add(root) // Add the root node to the queue

        while (toProcess.isNotEmpty()) {
            val node: QTNode = toProcess.removeFirst()

            if (node.isLeaf) {
                leaves.add(node)
            } else {
                addOverlappingNodesToQueue(node, area, toProcess)
            }
        }

        return leaves
    }

    private fun addOverlappingNodesToQueue(
        node: QTNode,
        area: BoundingBox,
        toProcess: ArrayDeque<QTNode>,
    ) {
        val nodeBbox: BoundingBox = node.bbox
        val cx: Float = nodeBbox.x + nodeBbox.width / 2 // Center X
        val cy: Float = nodeBbox.y + nodeBbox.height / 2 // Center Y

        // Get the index of the first child
        val childIndex: Int = node.firstIdx

        // Check which children overlap the bounding box
        if (area.y + area.height > cy) { // Top edge intersects or is above the center
            if (area.x < cx) { // Left edge intersects or is left of the center
                toProcess.add(nodes[childIndex + 0]) // Top-left
            }
            if (area.x + area.width > cx) { // Right edge intersects or is right of the center
                toProcess.add(nodes[childIndex + 1]) // Top-right
            }
        }
        if (area.y < cy) { // Bottom edge intersects or is below the center
            if (area.x < cx) { // Left edge intersects or is left of the center
                toProcess.add(nodes[childIndex + 2]) // Bottom-left
            }
            if (area.x + area.width > cx) { // Right edge intersects or is right of the center
                toProcess.add(nodes[childIndex + 3]) // Bottom-right
            }
        }
    }

    private fun insertIntoLeaf(objIdx: Int, node: QTNode) {
        // Create a new element holding a reference to the object
        val element = QTNodeElement(objIdx, -1)

        // If the node is empty, add the element as the first element
        if (node.count == 0) {
            node.firstIdx = elements.add(element)
            node.count = 1
            return
        }

        val nodeElement: QTNodeElement? = getLastElement(node, objIdx)
        if (nodeElement == null) return

        // Append the element to the end of the list
        nodeElement.next = elements.add(element)
        node.count++

        // Try splitting the node
        trySplitNode(node)
    }

    /**
     * Returns the last element in the linked list of elements in a [node].
     * If the object is already in the node, the function returns mull.
     */
    private fun getLastElement(node: QTNode, objIdx: Int): QTNodeElement? {
        // Traverse the element linked list until the last element
        var nodeElement: QTNodeElement = elements[node.firstIdx]
        while (true) {
            // Check if the object is already in the node
            if (nodeElement.index == objIdx) return null
            if (nodeElement.next == -1) break
            nodeElement = elements[nodeElement.next]
        }

        return nodeElement
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
        for (it: FhysicsObject in pendingRemovals) {
            val objIdx: Int = objects.indexOf(it)
            if (objIdx == -1) continue

            removeIteratively(objIdx, it.boundingBox)
        }

        pendingRemovals.clear()
    }

    private fun removeIteratively(objIdx: Int, bbox: BoundingBox) {
        // Remove the element from the object list
        objects.free(objIdx)

        // A collection of nodes to process
        val queue: ArrayDeque<QTNode> = ArrayDeque()
        queue.add(root)

        // Remove all node elements pointing to the object
        while (queue.isNotEmpty()) {
            val node: QTNode = queue.removeFirst()

            if (node.isLeaf) {
                removeFromLeaf(node, objIdx)
            } else {
                addOverlappingNodesToQueue(node, bbox, queue)
            }
        }
    }

    private fun removeFromLeaf(node: QTNode, objIdx: Int) {
        // Traverse the element linked list
        var current = QTNodeElement(-1, node.firstIdx) // Dummy element
        var previous: QTNodeElement?
        while (current.next != -1) {
            // Get the next element
            previous = current
            current = elements[current.next]

            // Check if the object is the one to remove
            if (current.index != objIdx) continue

            // If the object is the first element, update the first index
            if (previous.index == -1) {
                elements.free(node.firstIdx)
                node.firstIdx = current.next
            } else {
                elements.free(previous.next)
                previous.next = current.next
            }

            // Object removed successfully
            node.count--
            return
        }
    }
    /// endregion

    /// region =====Query=====
    fun query(pos: Vector2): FhysicsObject? {
        val leaf: QTNode = getLeafNode(pos)
        val objectHit: FhysicsObject? = queryLeafObjects(leaf, pos)
        return objectHit
    }

    private fun getLeafNode(pos: Vector2): QTNode {
        // Traverse the tree until the leaf node containing the position is found
        var node: QTNode = root
        while (true) {
            if (node.isLeaf) return node

            val nodeBbox: BoundingBox = node.bbox
            val cx: Float = nodeBbox.x + nodeBbox.width / 2 // Center X
            val cy: Float = nodeBbox.y + nodeBbox.height / 2 // Center Y

            // Get the index of the first child
            val childIndex: Int = node.firstIdx

            // Check which child node contains the position
            node = when {
                pos.y >= cy && pos.x <= cx -> nodes[childIndex + 0] // Top-left
                pos.y >= cy && pos.x > cx -> nodes[childIndex + 1] // Top-right
                pos.y < cy && pos.x <= cx -> nodes[childIndex + 2] // Bottom-left
                else -> nodes[childIndex + 3] // Bottom-right
            }
        }
    }

    private fun queryLeafObjects(node: QTNode, pos: Vector2): FhysicsObject? {
        if (node.count <= 0) return null

        // Traverse the element linked list
        var nodeElement = QTNodeElement(-1, node.firstIdx) // Dummy element
        while (nodeElement.next != -1) {
            // Get the next element
            nodeElement = elements[nodeElement.next]

            // Get the object from the list
            val obj: FhysicsObject = objects[nodeElement.index]
            // Check if the object contains the position
            if (obj.contains(pos)) return obj
        }

        return null
    }
    /// endregion

    /// region =====Partitioning=====
    /// region =====Splitting=====
    private fun trySplitNode(parent: QTNode) {
        if (!shouldSplitNode(parent)) return

        // Split the node
        val firstElementIndex: Int = parent.firstIdx
        val firstChildIdx: Int = createChildNodes(parent.bbox)
        moveElementsToChildren(firstElementIndex, firstChildIdx)
        convertToBranch(parent, firstChildIdx)
    }

    private fun shouldSplitNode(node: QTNode): Boolean {
        return node.count > capacity && min(node.bbox.width, node.bbox.height) > MIN_SIZE
    }

    private fun createChildNodes(parentBbox: BoundingBox): Int {
        val cx: Float = parentBbox.x + parentBbox.width / 2 // Center X
        val cy: Float = parentBbox.y + parentBbox.height / 2 // Center Y
        val hw: Float = parentBbox.width / 2 // Half width
        val hh: Float = parentBbox.height / 2 // Half height

        val tl = QTNode(BoundingBox(parentBbox.x, cy, hw, hh))
        val tr = QTNode(BoundingBox(cx, cy, hw, hh))
        val bl = QTNode(BoundingBox(parentBbox.x, parentBbox.y, hw, hh))
        val br = QTNode(BoundingBox(cx, parentBbox.y, hw, hh))

        val firstNodeIndex: Int = nodes.add(tl)
        nodes.add(tr)
        nodes.add(bl)
        nodes.add(br)
        return firstNodeIndex
    }

    private fun moveElementsToChildren(firstElementIndex: Int, firstChildIdx: Int) {
        for (i: Int in 0..3) {
            val child: QTNode = nodes[firstChildIdx + i]
            insertOverlappingObjects(firstElementIndex, child)
        }
    }

    private fun insertOverlappingObjects(firstElementIndex: Int, node: QTNode) {
        var current = QTNodeElement(-1, firstElementIndex) // Dummy element
        while (current.next != -1) {
            current = elements[current.next]

            val obj: FhysicsObject = objects[current.index]
            if (node.bbox.overlaps(obj.boundingBox)) {
                insertIntoLeaf(current.index, node)
            }
        }
    }

    private fun convertToBranch(node: QTNode, firstChildIdx: Int) {
        // Remove the elements from the parent node
        var current = QTNodeElement(-1, node.firstIdx) // Dummy element
        while (current.next != -1) {
            val removeIdx: Int = current.next
            current = elements[current.next]
            elements.free(removeIdx)
        }

        // Set the count to -1 to indicate that the node is a branch
        node.count = -1
        // Set the first index to the first child node
        node.firstIdx = firstChildIdx
    }
    /// endregion

    /// region =====Cleanup=====
    /**
     * Collapses nodes with only empty leaves as children into a single leaf node.
     */
    fun cleanup() {
        if (root.isLeaf) return

        // Queue of node indices to process
        val toProcess: ArrayDeque<QTNode> = ArrayDeque()
        toProcess.add(root)

        while (toProcess.isNotEmpty()) {
            val node: QTNode = toProcess.removeFirst()

            // Loop through the children
            var numEmptyLeaves: Int = countEmptyLeaves(node, toProcess)

            // If all children are empty leaves, convert the node to a leaf
            if (numEmptyLeaves == 4) {
                convertToLeaf(node)
            }
        }
    }

    private fun countEmptyLeaves(node: QTNode, toProcess: ArrayDeque<QTNode>): Int {
        var numEmptyLeaves = 0
        for (i: Int in 0..3) {
            val childIdx: Int = node.firstIdx + i
            val child: QTNode = nodes[childIdx]

            when {
                // Is empty leaf --> increment the counter
                child.count == 0 -> numEmptyLeaves++
                // Is branch --> add to be processed
                !child.isLeaf -> toProcess.add(child)
            }
        }

        return numEmptyLeaves
    }

    private fun convertToLeaf(node: QTNode) {
        // Free children in reverse order to keep order (tl, tr...) during insertion
        for (i: Int in 3 downTo 0) {
            nodes.free(node.firstIdx + i)
        }

        // Convert the node to a leaf
        node.count = 0
        node.firstIdx = -1
    }
    /// endregion
    /// endregion
    /// endregion

    /// region =====Fhysics Operations=====
    fun processPendingOperations() {
        if (rebuild) {
            rebuild = false
            rebuild()
        }
        insertPending()
        removePending()
    }

    private fun rebuild() {
        // Store all objects in a temporary list
        val tempObjects: List<FhysicsObject> = objects.toList()
        clear()
        pendingAdditions.addAll(tempObjects)
        insertPending()
    }

    fun clear() {
        root = QTNode(BORDER)
        nodes.clear()
        nodes.add(root)
        elements.clear()
        objects.clear()
    }

    fun update() {
        val queue: ArrayDeque<QTNode> = ArrayDeque()
        queue.add(root)

        while (queue.isNotEmpty()) {
            val node: QTNode = queue.removeFirst()

            if (node.isLeaf) {
                updateLeaf(node)
            } else {
                for (i: Int in 0..3) {
                    queue.add(nodes[node.firstIdx + i])
                }
            }
        }

//        val leaves: List<QTNode> = nodes.filter { it.isLeaf }
//        for (leaf: QTNode in leaves) {
//            handlePhysicsInLeaf(leaf)
//        }
    }

    private fun updateLeaf(node: QTNode) {
        val objectsInLeaf: MutableList<FhysicsObject> = getObjectsInLeaf(node)
        updateFhysicsObjects(objectsInLeaf)
        handleCollisions(objectsInLeaf)
        rebuild(objectsInLeaf, node)
    }

    private fun getObjectsInLeaf(node: QTNode): MutableList<FhysicsObject> {
        val objectsInLeaf: MutableList<FhysicsObject> = ArrayList(node.count)
        var current = QTNodeElement(-1, node.firstIdx) // Dummy element
        while (current.next != -1) {
            current = elements[current.next]
            objectsInLeaf.add(objects[current.index])
        }
        return objectsInLeaf
    }

    private fun updateFhysicsObjects(objectsInLeaf: MutableList<FhysicsObject>) {
        for (obj: FhysicsObject in objectsInLeaf) {
            obj.update()
        }
    }

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

    private fun rebuild(objectsInLeaf: MutableList<FhysicsObject>, node: QTNode) {
        for (obj: FhysicsObject in objectsInLeaf) {
            // Check if the object has moved out of the leaf
            if (node.bbox.contains(obj.boundingBox)) continue

            val objIdx: Int = objects.indexOf(obj)
            if (!node.bbox.overlaps(obj.boundingBox)) {
                removeFromLeaf(node, objIdx)
            }
            insertIteratively(objIdx, obj.boundingBox)
        }
    }

    fun shutdownThreadPool() {
        println("Shutting down thread pool")
//        threadPool.shutdownNow() // TODO
    }
    /// endregion

    /// region =====Rendering=====
    fun drawObjects(drawer: FhysicsObjectDrawer) {
        for (obj: FhysicsObject in objects) {
            obj.draw(drawer)
        }

        if (UIController.drawBoundingBoxes) {
            for (obj: FhysicsObject in objects) {
                DebugDrawer.drawBoundingBox(obj.boundingBox)
            }
        }
    }

    fun drawNodes() {
        // Only drawing leaf nodes is enough
        val leaves: List<QTNode> = nodes.filter { it.isLeaf }
        for (leaf: QTNode in leaves) {
            DebugDrawer.drawQTNode(leaf.bbox, leaf.count)
        }
    }
    /// endregion

    /// region =====Other=====
    fun getObjectCount(): Int {
        return objects.usedCount()
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
        /**
         * Points to the first child node in [QuadTree.nodes] when the node is a branch
         * or to the first element in [QuadTree.elements] when the node is a leaf.
         *
         * Nodes are stored in blocks of 4 in order: top left, top right, bottom left, bottom right.
         *
         * Elements are stored as a linked list.
         */
        var firstIdx: Int = -1,
        /**
         * The number of elements in the node or -1 if it's a branch.
         */
        var count: Int = 0,

        ) {

        /**
         * Whether the node is a leaf or a branch.
         */
        val isLeaf: Boolean get() = count != -1
    }

    /**
     * A class representing an element in a [QTNode].
     *
     * An Element is a reference to an object in the [QuadTree.objects] list.
     * Multiple elements can reference the same object. This is done to prevent storing the same object multiple times.
     */
    private data class QTNodeElement(
        /** The index of the object in [QuadTree.objects]. */
        val index: Int,
        /** The index of the next element in the node within [QuadTree.elements], or -1 if it is the last element. */
        var next: Int,
    )

    /// region =====Debugging=====
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
            println("QTNode(count=${node.count}, firstIdx=${node.firstIdx}, bbox=${node.bbox})")
        }

        private fun getChildNodes(parent: QTNode): MutableList<QTNode> {
            if (parent.isLeaf) return mutableListOf()

            val children: MutableList<QTNode> = mutableListOf()
            addChildNodesToCollection(parent.firstIdx, children)

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

        /** Returns the children of the given [parent]. */
        fun getChildren(parent: QTNode): Array<QTNode> {
            val children: Array<QTNode> = Array(4) { QTNode(BoundingBox()) }
            val firstChildIdx: Int = parent.firstIdx
            for (i: Int in 0..3) {
                val node: QTNode = nodes[firstChildIdx + i]
                children[i] = QTNode(node.bbox, node.firstIdx, node.count)
            }
            return children
        }
    }
    /// endregion
    /// endregion
}
