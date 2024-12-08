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
        set(value) {
            field = value
            rootData = QTNodeData(BORDER, 0, 0)
        }

    private var rootData: QTNodeData = QTNodeData(BORDER, 0, 0)

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
        val overlappingLeaves: MutableList<QTNodeData> = findOverlappingLeaves(obj.boundingBox)
        for (leave: QTNodeData in overlappingLeaves) {
            insertIntoLeaf(objIdx, leave)
        }
    }

    private fun findOverlappingLeaves(area: BoundingBox): MutableList<QTNodeData> {
        val leaves: MutableList<QTNodeData> = mutableListOf() // List to store leaf nodes
        val toProcess: ArrayDeque<QTNodeData> = ArrayDeque() // Queue to manage nodes to process
        toProcess.add(rootData) // Add the root node to the queue

        while (toProcess.isNotEmpty()) {
            val nodeData: QTNodeData = toProcess.removeFirst()

            // Add leaf nodes to the leaves list
            if (nodes[nodeData.index].isLeaf) { // TODO check if overlap check is necessary here (add better explanation)
                leaves.add(nodeData)
                continue
            }

            addOverlappingNodesToQueue(nodeData, area, toProcess)
        }

        return leaves
    }

    private fun addOverlappingNodesToQueue(
        nodeData: QTNodeData,
        area: BoundingBox,
        toProcess: ArrayDeque<QTNodeData>, // [left edge, right edge, top edge, bottom edge]
    ) {
        val nodeBbox: BoundingBox = nodeData.bbox
        val cx: Float = nodeBbox.x + nodeBbox.width / 2 // Center X
        val cy: Float = nodeBbox.y + nodeBbox.height / 2 // Center Y
        val hw: Float = nodeBbox.width / 2 // Half width
        val hh: Float = nodeBbox.height / 2 // Half height

        // Get the index of the first child
        val childIndex: Int = nodes[nodeData.index].firstIdx

        // Check which children overlap the bounding box
        if (area.y + area.height > cy) { // Top edge intersects or is above the center
            if (area.x < cx) { // Left edge intersects or is left of the center
                toProcess.add(
                    QTNodeData(
                        BoundingBox(nodeBbox.x, cy, hw, hh),
                        childIndex + 0, nodeData.depth + 1
                    )
                ) // Top-left
            }
            if (area.x + area.width > cx) { // Right edge intersects or is right of the center
                toProcess.add(
                    QTNodeData(
                        BoundingBox(cx, cy, hw, hh),
                        childIndex + 1, nodeData.depth + 1
                    )
                ) // Top-right
            }
        }
        if (area.y < cy) { // Bottom edge intersects or is below the center
            if (area.x < cx) { // Left edge intersects or is left of the center
                toProcess.add(
                    QTNodeData(
                        BoundingBox(nodeBbox.x, nodeBbox.y, hw, hh),
                        childIndex + 2, nodeData.depth + 1
                    )
                ) // Bottom-left
            }
            if (area.x + area.width > cx) { // Right edge intersects or is right of the center
                toProcess.add(
                    QTNodeData(
                        BoundingBox(cx, nodeBbox.y, hw, hh),
                        childIndex + 3, nodeData.depth + 1
                    )
                ) // Bottom-right
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

        val nodeElement: QTNodeElement? = getLastElement(node, objIdx)
        if (nodeElement == null) return

        // Append the element to the end of the list
        nodeElement.next = elements.add(element)
        node.count++

        // Try splitting the node
        trySplitNode(nodeData)
    }

    /**
     * Returns the last element in the linked list of elements in a [node].
     * If the object is already in the node, the function returns mull.
     */
    private fun getLastElement(node: QTNode, objIdx: Int): QTNodeElement? {
        // Traverse the element linked list until the last element
        var nodeElement: QTNodeElement = elements[node.firstIdx]
        while (nodeElement.next != -1) {
            // Check if the object is already in the node
            if (nodeElement.index == objIdx) return null

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
            addOverlappingNodesToQueue(nodeData, bbox, queue)
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

            val nodeBbox: BoundingBox = nodeData.bbox
            val cx: Float = nodeBbox.x + nodeBbox.width / 2 // Center X
            val cy: Float = nodeBbox.y + nodeBbox.height / 2 // Center Y
            val hw: Float = nodeBbox.width / 2 // Half width
            val hh: Float = nodeBbox.height / 2 // Half height

            // Get the index of the first child
            val childIndex: Int = node.firstIdx

            // Check which child node contains the position (favouring the top-left node)
            nodeData =
                if (pos.y >= cy) { // Top side
                    if (pos.x <= cx) { // Left side
                        QTNodeData(
                            BoundingBox(nodeBbox.x, cy, hw, hh),
                            childIndex + 0, nodeData.depth + 1
                        ) // Top-left
                    } else {
                        QTNodeData(
                            BoundingBox(cx, cy, hw, hh),
                            childIndex + 1, nodeData.depth + 1
                        ) // Top-right
                    }
                } else { // Bottom side
                    if (pos.x <= cx) { // Left side
                        QTNodeData(
                            BoundingBox(nodeBbox.x, nodeBbox.y, hw, hh),
                            childIndex + 2, nodeData.depth + 1
                        ) // Bottom-left
                    } else {
                        QTNodeData(
                            BoundingBox(cx, nodeBbox.y, hw, hh),
                            childIndex + 3, nodeData.depth + 1
                        ) // Bottom-right
                    }
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
        return node.count > capacity && min(nodeData.bbox.width, nodeData.bbox.height) > MIN_SIZE
    }

    private fun createChildNodes(parent: QTNodeData): Array<QTNodeData> {
        // Add new child nodes to the list
        val firstNodeIndex: Int = createNodes(parent.bbox) // Store the index of the first child node

        val qtNodeData: MutableList<QTNodeData> = mutableListOf()
        addChildNodeDataToCollection(parent, qtNodeData, firstNodeIndex)

        return qtNodeData.toTypedArray()
    }

    private fun createNodes(parentBbox: BoundingBox): Int {
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

    private fun moveElementsToChildren(firstElementIndex: Int, children: Array<QTNodeData>) {
        for (child: QTNodeData in children) {
            insertOverlappingObjects(firstElementIndex, child)
        }
    }

    private fun insertOverlappingObjects(firstElementIndex: Int, node: QTNodeData) {
        var current = QTNodeElement(-1, firstElementIndex) // Dummy element
        while (current.next != -1) {
            current = elements[current.next]

            val obj: FhysicsObject = objects[current.index]
            if (node.bbox.overlaps(obj.boundingBox)) {
                insertIntoLeaf(current.index, node)
            }
        }
    }

    private fun convertToBranch(node: QTNode, firstChildData: QTNodeData) {
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
            var numEmptyLeaves: Int = countEmptyLeaves(node, toProcess)

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
    /// endregion

    /// region =====Utility=====
    private fun addChildNodeDataToCollection(
        parentData: QTNodeData,
        collection: MutableCollection<QTNodeData>,
        childIndex: Int = nodes[parentData.index].firstIdx, // I love this syntax
    ) {
//        val (
//            cx: Int, cy: Int, // Center X/Y
//            hwl: Int, hwr: Int, // Half width left/right
//            hhb: Int, hht: Int, // Half height bottom/top
//        ) = calculateNodeDimensions(parentData)
//        val _0_ = 0 // For better readability
        val bbox: BoundingBox = parentData.bbox
        val blx: Float = bbox.x
        val bly: Float = bbox.y
        val cx: Float = blx + bbox.width / 2
        val cy: Float = bly + bbox.height / 2
        val hw: Float = bbox.width / 2
        val hh: Float = bbox.height / 2

        // Calculate the child nodes and add them to the collection
        // x (bottom left corner), y (bottom left corner), width, height, index, depth
        collection.add(QTNodeData(BoundingBox(blx, cy, hw, hh), childIndex + 0, parentData.depth + 1)) // Top-left
        collection.add(QTNodeData(BoundingBox(cx, cy, hw, hh), childIndex + 1, parentData.depth + 1)) // Top-right
        collection.add(QTNodeData(BoundingBox(blx, bly, hw, hh), childIndex + 2, parentData.depth + 1)) // Bottom-left
        collection.add(QTNodeData(BoundingBox(cx, bly, hw, hh), childIndex + 3, parentData.depth + 1)) // Bottom-right
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
        root = QTNode(BORDER)
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
            obj.update()

            // Only update the object in the tree if the integer-based bounding box has changed
            updateObjectInTree(index, obj)
        }
    }

    /**
     * Updates the object in the tree by removing it and reinserting it.
     */
    private fun updateObjectInTree(objIdx: Int, obj: FhysicsObject) {
        removeIteratively(objIdx, obj.boundingBox)
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
            handleCollisionsInLeaf(leaf)
            collisions.clear()
        }
    }

    private fun handleCollisionsInLeaf(node: QTNode) {
        var current = QTNodeElement(-1, node.firstIdx) // Dummy element
        while (current.next != -1) {
            current = elements[current.next]

            // Check for collisions between current and the remaining elements in the node
            checkAndSolveCollisions(current)
        }
    }

    private fun checkAndSolveCollisions(current: QTNodeElement) {
        // Get the object from the list
        val obj: FhysicsObject = objects[current.index]

        // Iterate over the remaining elements in the node
        var next = QTNodeElement(-1, current.next) // Dummy element
        while (next.next != -1) {
            next = elements[next.next]

            // Get the object from the list
            val other: FhysicsObject = objects[next.index]

            // Check for collision
            val info: CollisionInfo = obj.testCollision(other)
            if (info.hasCollision) {
                CollisionSolver.solveCollision(info)
            }
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
                DebugDrawer.drawQTNode(nodeData.bbox, node.count)
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
     * A class holding additional data for a [QTNode] object.
     *
     * This data is not stored, but calculated when needed.
     */
    data class QTNodeData(
//        /** IntArray representing the node's bounding box: [centerX, centerY, width, height]. */
//        val cRect: CenterRect,
        /** The bounding box of the node. */
        val bbox: BoundingBox,
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
//        constructor(
//            x: Int, y: Int,
//            width: Int, height: Int,
//            index: Int, depth: Int,
//        ) : this(
//            CenterRect(x + width / 2, y + height / 2, width, height),
//            index, depth,
//        )
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
            println("QTNode(count=${node.count}, depth=${nodeData.depth})")
        }

        private fun getChildNodeData(parentData: QTNodeData): MutableList<QTNodeData> {
            if (nodes[parentData.index].isLeaf) return mutableListOf()

            val childData: MutableList<QTNodeData> = mutableListOf()
            addChildNodeDataToCollection(parentData, childData)

            return childData
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
