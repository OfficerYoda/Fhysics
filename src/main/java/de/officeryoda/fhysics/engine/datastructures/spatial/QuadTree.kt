package de.officeryoda.fhysics.engine.datastructures.spatial

import de.officeryoda.fhysics.engine.FhysicsCore
import de.officeryoda.fhysics.engine.collision.CollisionInfo
import de.officeryoda.fhysics.engine.collision.CollisionSolver
import de.officeryoda.fhysics.engine.datastructures.IndexedFreeList
import de.officeryoda.fhysics.engine.datastructures.Tuple6
import de.officeryoda.fhysics.engine.datastructures.spatial.QuadTree.processPendingOperations
import de.officeryoda.fhysics.engine.math.Vector2
import de.officeryoda.fhysics.engine.objects.FhysicsObject
import de.officeryoda.fhysics.engine.util.floorToInt
import de.officeryoda.fhysics.rendering.DebugDrawer
import de.officeryoda.fhysics.rendering.FhysicsObjectDrawer
import de.officeryoda.fhysics.rendering.UIController
import kotlin.math.log2
import kotlin.math.min

object QuadTree {
    // Capacity of the tree
    var capacity: Int = 8
        set(value) {
            field = value.coerceAtLeast(1)
        }

    // The max depth of the QuadTree
    // The formula calculates the amount of divisions before the size falls under 0
    private val MAX_DEPTH = log2(min(FhysicsCore.BORDER.width, FhysicsCore.BORDER.height)).floorToInt()

    // The root node of the tree
    private var root: QTNode = QTNode()
        set(value) {
            field = value
            rootData = QTNodeData(CenterRect.Companion.fromBoundingBox(FhysicsCore.BORDER), 0, 0)
        }

    private var rootData: QTNodeData = QTNodeData(CenterRect.Companion.fromBoundingBox(FhysicsCore.BORDER), 0, 0)

    // List of all nodes in the tree (root node is always at index 0)
    private val nodes = IndexedFreeList(root)

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
        val overlappingLeaves: MutableList<QTNodeData> = findOverlappingLeaves(BoundingBoxEdges(obj.boundingBox))
        for (leave: QTNodeData in overlappingLeaves) {
            insertIntoLeaf(objIdx, leave)
        }
    }

    private fun findOverlappingLeaves(bboxEdges: BoundingBoxEdges): MutableList<QTNodeData> {
        val leaves: MutableList<QTNodeData> = mutableListOf() // List to store leaf nodes
        val toProcess: ArrayDeque<QTNodeData> = ArrayDeque() // Queue to manage nodes to process
        toProcess.add(rootData) // Add the root node to the queue

        while (toProcess.isNotEmpty()) {
            val nodeData: QTNodeData = toProcess.removeFirst()

            // Add leaf nodes to the leaves list
            if (nodes[nodeData.index].isLeaf) {
                leaves.add(nodeData)
                continue
            }

            addOverlappingNodesToQueue(nodeData, bboxEdges, toProcess)
        }

        return leaves
    }

    private fun addOverlappingNodesToQueue(
        nodeData: QTNodeData,
        edges: BoundingBoxEdges,
        toProcess: ArrayDeque<QTNodeData>, // [left edge, right edge, top edge, bottom edge]
    ) {
        val (
            cx: Int, cy: Int, // Center X/Y
            hwl: Int, hwr: Int, // Half width left/right
            hhb: Int, hht: Int, // Half height bottom/top
        ) = calculateNodeDimensions(nodeData)

        // Get the index of the first child
        val childIndex: Int = nodes[nodeData.index].firstIdx

        // Check which children overlap the bounding box
        if (edges.top >= cy) { // Top edge intersects or is above the center
            if (edges.left <= cx) { // Left edge intersects or is left of the center
                toProcess.add(QTNodeData(cx - hwl, cy, hwl, hht, childIndex + 0, nodeData.depth + 1)) // Top-left
            }
            if (edges.right >= cx) { // Right edge intersects or is right of the center
                toProcess.add(QTNodeData(cx, cy, hwr, hht, childIndex + 1, nodeData.depth + 1)) // Top-right
            }
        }
        if (edges.bottom <= cy) { // Bottom edge intersects or is below the center
            if (edges.left <= cx) { // Left edge intersects or is left of the center
                toProcess.add(
                    QTNodeData(
                        cx - hwl,
                        cy - hhb,
                        hwl,
                        hhb,
                        childIndex + 2,
                        nodeData.depth + 1
                    )
                ) // Bottom-left
            }
            if (edges.right >= cx) { // Right edge intersects or is right of the center
                toProcess.add(QTNodeData(cx, cy - hhb, hwr, hhb, childIndex + 3, nodeData.depth + 1)) // Bottom-right
            }
        }
    }

    private fun insertIntoLeaf(objIdx: Int, nodeData: QTNodeData) {
        val node: QTNode = nodes[nodeData.index]
        val element = QTNodeElement(objIdx, -1)

        // If the node is empty, add the element as the first element
        if (node.count == 0) {
            node.firstIdx = elements.add(element)
            node.count = 1
            return
        }

        val (nodeElement: QTNodeElement, alreadyInNode: Boolean) = getLastElement(node, objIdx)
        if (alreadyInNode) return

        // Append the element to the end of the list
        nodeElement.next = elements.add(element)
        node.count++

        // Try splitting the node
        trySplitNode(nodeData)
    }

    private fun getLastElement(
        node: QTNode,
        objIdx: Int,
    ): Pair<QTNodeElement, Boolean> {
        // Traverse the element linked list until the last element
        var nodeElement: QTNodeElement = elements[node.firstIdx]
        while (nodeElement.next != -1) {
            // Check if the object is already in the node
            if (nodeElement.index == objIdx) return Pair(nodeElement, true)

            nodeElement = elements[nodeElement.next]
        }

        return Pair(nodeElement, false)
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

            removeIteratively(objIdx, BoundingBoxEdges(it.boundingBox))
        }

        pendingRemovals.clear()
    }

    private fun removeIteratively(
        objIdx: Int,
        bboxEdges: BoundingBoxEdges,
    ) {
        // Remove the element from the object list
        objects.free(objIdx)

        // A collection of nodes to process
        val queue: ArrayDeque<QTNodeData> = ArrayDeque()
        queue.add(rootData)

        // Remove all node elements pointing to the object
        while (queue.isNotEmpty()) {
            val nodeData: QTNodeData = queue.removeFirst()

            // If the node is a leaf, remove the object
            if (nodes[nodeData.index].isLeaf) {
                removeFromLeaf(nodes[nodeData.index], objIdx)
                continue
            }

            // Add nodes that overlap with the object to the processing queue
            addOverlappingNodesToQueue(nodeData, bboxEdges, queue)
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
            if (current.index == objIdx) {
                // If the object is the first element, update the first element
                if (previous.index == -1) {
                    elements.free(node.firstIdx)
                    node.firstIdx = current.next
                } else {
                    elements.free(previous.next)
                    previous.next = current.next
                }

                node.count--
                return
            }
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
        var nodeData: QTNodeData = rootData
        while (true) {
            val node: QTNode = nodes[nodeData.index]
            if (node.isLeaf) return node

            val (
                cx: Int, cy: Int, // Center X/Y
                hwl: Int, hwr: Int, // Half width left/right
                hhb: Int, hht: Int, // Half height bottom/top
            ) = calculateNodeDimensions(nodeData)

            // Check which child node contains the position (favouring the top-left node)
            nodeData =
                if (pos.y >= cy) { // Top side
                    if (pos.x <= cx) { // Left side
                        QTNodeData(cx - hwl, cy, hwl, hht, node.firstIdx + 0, nodeData.depth + 1) // Top-left
                    } else {
                        QTNodeData(cx, cy, hwr, hht, node.firstIdx + 1, nodeData.depth + 1) // Top-right
                    }
                } else { // Bottom side
                    if (pos.x <= cx) { // Left side
                        QTNodeData(cx - hwl, cy - hhb, hwl, hhb, node.firstIdx + 2, nodeData.depth + 1) // Bottom-left
                    } else {
                        QTNodeData(cx, cy - hhb, hwr, hhb, node.firstIdx + 3, nodeData.depth + 1) // Bottom-right
                    }
                }
        }
    }

    private fun queryLeafObjects(
        node: QTNode,
        pos: Vector2,
    ): FhysicsObject? {
        if (node.count <= 0) return null

        // Traverse the element linked list
        var nodeElement = QTNodeElement(-1, node.firstIdx) // Dummy element
        while (nodeElement.next != -1) {
            // Get the next element
            nodeElement = elements[nodeElement.next]

            // Get the object from the list
            val obj: FhysicsObject = objects[nodeElement.index]
            // Check if the object contains the position
            if (obj.contains(pos)) {
                return obj
            }
        }

        return null
    }
    /// endregion

    /// region =====Partitioning=====
    /// region =====Splitting=====
    private fun trySplitNode(nodeData: QTNodeData) {
        val parent: QTNode = nodes[nodeData.index]
        if (!shouldSplitNode(parent, nodeData)) return

        // Split the node
        val firstElementIndex: Int = parent.firstIdx
        val childNodes: Array<QTNodeData> = createChildNodes(nodeData)
        moveElementsToChildren(firstElementIndex, childNodes)
        convertToBranch(parent, childNodes.first())
    }

    private fun shouldSplitNode(node: QTNode, nodeData: QTNodeData): Boolean {
        return node.count > capacity && nodeData.depth < MAX_DEPTH
    }

    private fun createChildNodes(parent: QTNodeData): Array<QTNodeData> {
        // Add new child nodes to the list
        val firstNodeIndex: Int = nodes.add(QTNode()) // Store the index of the first child node
        repeat(3) { nodes.add(QTNode()) } // Add the other 3 child nodes

        val qtNodeData: MutableList<QTNodeData> = mutableListOf()
        addChildNodeDataToCollection(parent, qtNodeData, firstNodeIndex)

        return qtNodeData.toTypedArray()
    }

    private fun moveElementsToChildren(
        firstElementIndex: Int,
        children: Array<QTNodeData>,
    ) {
        for (child: QTNodeData in children) {
            insertOverlappingObjects(firstElementIndex, child)
        }
    }

    private fun insertOverlappingObjects(firstElementIndex: Int, node: QTNodeData) {
        var current = QTNodeElement(-1, firstElementIndex) // Dummy element
        while (current.next != -1) {
            current = elements[current.next]

            val obj: FhysicsObject = objects[current.index]
            if (obj.boundingBox.overlaps(node.cRect)) {
                insertIntoLeaf(current.index, node)
            }
        }
    }

    private fun convertToBranch(
        node: QTNode,
        firstChildData: QTNodeData,
    ) {
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
        node.firstIdx = firstChildData.index
    }
    /// endregion

    /// region =====Cleanup=====
    /**
     * Collapses nodes with only empty leaves as children into a single leaf node.
     */
    fun cleanup() {
        // Queue of node indices to process
        val toProcess: ArrayDeque<Int> = ArrayDeque()
        // Only process the root if it's not a leaf
        if (!nodes[0].isLeaf) toProcess.add(rootData.index)

        while (toProcess.isNotEmpty()) {
            val nodeIdx: Int = toProcess.removeFirst()
            val node: QTNode = nodes[nodeIdx]

            // Loop through the children
            var numEmptyLeaves = countEmptyLeaves(node, toProcess)

            // If all children are empty leaves, convert the node to a leaf
            if (numEmptyLeaves == 4) {
                convertToLeaf(node)
            }
        }
    }

    private fun countEmptyLeaves(node: QTNode, toProcess: ArrayDeque<Int>): Int {
        var numEmptyLeaves = 0
        for (i: Int in 0..3) {
            val childIdx: Int = node.firstIdx + i
            val child: QTNode = nodes[childIdx]

            when {
                // Is empty leaf --> increment the counter
                child.count == 0 -> numEmptyLeaves++
                // Is branch --> add to be processed
                !child.isLeaf -> toProcess.add(childIdx)
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

    /// region =====Utility=====
    private fun addChildNodeDataToCollection(
        parentData: QTNodeData,
        collection: MutableCollection<QTNodeData>,
        childIndex: Int = nodes[parentData.index].firstIdx, // I love this syntax
    ) {
        val (
            cx: Int, cy: Int, // Center X/Y
            hwl: Int, hwr: Int, // Half width left/right
            hhb: Int, hht: Int, // Half height bottom/top
        ) = calculateNodeDimensions(parentData)
        val _0_ = 0 // For better readability

        // Calculate the child nodes and add them to the collection
        // x (bottom left corner), y (bottom left corner), width, height, index, depth
        collection.add(QTNodeData(cx - hwl, cy + _0_, hwl, hht, childIndex + 0, parentData.depth + 1)) // Top-left
        collection.add(QTNodeData(cx + _0_, cy + _0_, hwr, hht, childIndex + 1, parentData.depth + 1)) // Top-right
        collection.add(QTNodeData(cx - hwl, cy - hhb, hwl, hhb, childIndex + 2, parentData.depth + 1)) // Bottom-left
        collection.add(QTNodeData(cx + _0_, cy - hhb, hwr, hhb, childIndex + 3, parentData.depth + 1)) // Bottom-right
    }

    /**
     * Returns a Tuple6 containing the center x, center y, half width left, half width right, half height bottom, and half height top of a [node][nodeData].
     */
    private fun calculateNodeDimensions(nodeData: QTNodeData): Tuple6<Int, Int, Int, Int, Int, Int> {
        val cRect: CenterRect = nodeData.cRect
        val cx: Int = cRect.centerX
        val cy: Int = cRect.centerY
        val tw: Int = cRect.width // Total width
        val th: Int = cRect.height // Total height
        // Odd sizes will be split, so that the right and top sides are larger
        val hwl: Int = tw / 2 // Half width left
        val hwr: Int = tw - hwl // Half width right
        val hhb: Int = th / 2 // Half height bottom
        val hht: Int = th - hhb // Half height top
        return Tuple6(cx, cy, hwl, hwr, hhb, hht)
    }

    fun getObjectCount(): Int {
        return objects.usedCount()
    }
    /// endregion
    /// endregion

    /// region =====Fhysics Operations=====
    fun processPendingOperations() {
        if (rebuild) rebuild()
        insertPending()
        removePending()
    }

    fun rebuild() {
        // Store all objects in a temporary list
        val tempObjects: List<FhysicsObject> = objects.toList()
        clear()
        pendingAdditions.addAll(tempObjects)
        insertPending()
    }

    fun clear() {
        root = QTNode()
        nodes.clear()
        nodes.add(root)
        elements.clear()
        objects.clear()
    }

    fun updateFhysicsObjects() {
        // Custom iterator to get the index of the object in the free list
        val iterator: IndexedFreeList.IndexedIterator<FhysicsObject> =
            IndexedFreeList.IndexedIterator(objects)

        // Update all objects and update their position in the tree if necessary
        while (iterator.hasNext()) {
            // Get the object and its index
            val obj: FhysicsObject = iterator.next()
            val index: Int = iterator.index()

            // Update the object
            val cRectBefore: CenterRect = CenterRect.fromBoundingBox(obj.boundingBox)
            obj.update()
            val cRectAfter: CenterRect = CenterRect.fromBoundingBox(obj.boundingBox)

            // Only update the object in the tree if the integer-based bounding box has changed
            if (!cRectBefore.contentEquals(cRectAfter)) {
                updateObjectInTree(index, obj, cRectBefore)
            }
        }
    }

    /**
     * Updates the object in the tree by removing it and reinserting it.
     */
    private fun updateObjectInTree(objIdx: Int, obj: FhysicsObject, cRectBefore: CenterRect) {
        removeIteratively(objIdx, BoundingBoxEdges.fromCenterRect(cRectBefore))
        insertIteratively(obj)
    }

    fun handleCollisions() {
        handleBorderCollision()
        handleObjectCollision()
    }

    private fun handleBorderCollision() {
        for (obj: FhysicsObject in objects) {
            CollisionSolver.handleBorderCollisions(obj)
        }
    }

    private fun handleObjectCollision() {
        // Get all leaves
        val leaves: List<QTNode> = nodes.filter { it.isLeaf }

        // Declare outside to only allocate once
        val collisions: MutableList<CollisionInfo> = ArrayList()
        // Handle collisions in each leaf
        for (leaf: QTNode in leaves) {
            handleCollisionsInLeaf(leaf, collisions)
            collisions.clear()
        }
    }

    private fun handleCollisionsInLeaf(node: QTNode, collisions: MutableList<CollisionInfo>) {
        // TODO check if querying the object in the QuadTree is faster than iterating over the elements
        // Traverse the element linked list
        var current = QTNodeElement(-1, node.firstIdx) // Dummy element
        while (current.next != -1) {
            current = elements[current.next]

            // Get the object from the list
            val obj: FhysicsObject = objects[current.index]

            // Check for collisions with other objects in the leaf
            var next = QTNodeElement(-1, current.next) // Dummy element
            while (next.next != -1) {
                next = elements[next.next]

                // Get the object from the list
                val other: FhysicsObject = objects[next.index]

                //  Check for collision
                val info: CollisionInfo = obj.testCollision(other)
                if (info.hasCollision) {
                    collisions.add(info)
                }
            }
        }

        // Solve the collisions
        for (collision: CollisionInfo in collisions) {
            CollisionSolver.solveCollision(collision)
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
        val queue = ArrayDeque<QTNodeData>()
        queue.add(rootData)

        while (queue.isNotEmpty()) {
            val nodeData: QTNodeData = queue.removeFirst()
            val node: QTNode = nodes[nodeData.index]

            // Only drawing leaf nodes is enough
            if (node.isLeaf) {
                DebugDrawer.drawQTNode(nodeData.cRect, node.count)
                continue
            }

            addChildNodeDataToCollection(nodeData, queue)
        }
    }
    /// endregion

    /// region =====Inner Classes=====
    /**
     * A class representing a node in the QuadTree.
     */
    data class QTNode(
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
     * A class holding additional data for a [QTNode] object.
     *
     * This data is not stored, but calculated when needed.
     */
    data class QTNodeData(
        /** IntArray representing the node's bounding box: [centerX, centerY, width, height]. */
        val cRect: CenterRect,
        /** Index of the node in the [QuadTree.nodes] list. */
        val index: Int,
        /** Depth of the node in the Quadtree. */
        val depth: Int,
    ) {

        /**
         * Constructs a new [QTNodeData] object.
         * @param x The x-coordinate of the node's bottom-left corner
         * @param y The y-coordinate of the node's bottom-left corner
         * @param width The width of the node
         * @param height The height of the node
         * @param index The index of the node in the [QuadTree.nodes] list
         * @param depth The depth of the node in the Quadtree
         */
        constructor(
            x: Int, y: Int,
            width: Int, height: Int,
            index: Int, depth: Int,
        ) : this(
            CenterRect(x + width / 2, y + height / 2, width, height),
            index, depth,
        )

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is QTNodeData) return false

            if (!cRect.contentEquals(other.cRect)) return false
            if (index != other.index) return false
            if (depth != other.depth) return false

            return true
        }

        override fun hashCode(): Int {
            var result: Int = index
            result = 31 * result + depth
            result = 31 * result + cRect.hashCode()
            return result
        }
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

        fun getCurrentDepth(): Int {
            var maxDepth = 0
            val toProcess: ArrayDeque<QTNodeData> = ArrayDeque()
            toProcess.add(rootData)

            while (toProcess.isNotEmpty()) {
                val nodeData: QTNodeData = toProcess.removeFirst()
                maxDepth = maxOf(maxDepth, nodeData.depth)

                if (!nodes[nodeData.index].isLeaf) {
                    addChildNodeDataToCollection(nodeData, toProcess)
                }
            }

            return maxDepth
        }

        fun printTree() {
            printlnNode(rootData)
            getChildNodeData(rootData).forEachIndexed { index, it ->
                printTree(it, "", index == 0)
            }
        }

        private fun printTree(nodeData: QTNodeData, indent: String, left: Boolean) {
            print(indent)
            print(if (left) "\u251C " else "\u2514 ")

            printlnNode(nodeData)
            getChildNodeData(nodeData).forEach {
                val newIndent = indent + (if (left) "\u2502 " else "  ")
                printTree(it, newIndent, true)
            }
        }

        private fun printlnNode(nodeData: QTNodeData) {
            val node: QTNode = nodes[nodeData.index]
            println("QTNode(count=${node.count}, depth=${nodeData.depth}, ${nodeData.cRect})")
        }

        private fun getChildNodeData(parentData: QTNodeData): MutableList<QTNodeData> {
            if (nodes[parentData.index].isLeaf) return mutableListOf()

            val childData: MutableList<QTNodeData> = mutableListOf()
            addChildNodeDataToCollection(parentData, childData)

            return childData
        }

        fun getChildren(parent: QTNode): Array<QTNode> {
            val children: Array<QTNode> = Array(4) { QTNode() }
            val firstChildIdx: Int = parent.firstIdx
            for (i: Int in 0..3) {
                val node: QTNode = nodes[firstChildIdx + i]
                children[i] = QTNode(node.firstIdx, node.count)
            }
            return children
        }
    }
    /// endregion
    /// endregion
}
