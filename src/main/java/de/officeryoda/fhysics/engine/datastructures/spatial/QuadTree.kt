package de.officeryoda.fhysics.engine.datastructures.spatial

import de.officeryoda.fhysics.engine.FhysicsCore
import de.officeryoda.fhysics.engine.datastructures.IndexedFreeList
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

    private var rootData: QTNodeData = QTNodeData(CenterRect.Companion.fromBoundingBox(FhysicsCore.BORDER), 0, 0)

    // List of all nodes in the tree (root node is always at index 0)
    private val nodes = IndexedFreeList(root)

    // List of all elements in the tree (elements store the index of the object in QuadTree.objects)
    // This is done, so that objects are only stored once in the tree and can be referenced multiple times by different elements
    private val elements = IndexedFreeList<QTNodeElement>()

    // List of all objects in the tree
    private val objects = IndexedFreeList<FhysicsObject>()

    // List of objects to add, used to queue up insertions and prevent concurrent modification
    private val pendingAdditions: MutableList<FhysicsObject> = ArrayList()

    // Set of objects to remove, used to mark objects for deletion safely
    private val pendingRemovals: MutableList<FhysicsObject> = ArrayList()

    /// region =====Basic QuadTree Operations=====
    /// region =====Insertion=====
    /**
     * Queues an object for insertion into the QuadTree.
     */
    fun queueInsertion(obj: FhysicsObject) {
        pendingAdditions.add(obj)
    }

    private fun insert(obj: FhysicsObject) {
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
        // Extract the current node's center and half-size
        val cx: Int = nodeData.cRect[0] // Center X
        val cy: Int = nodeData.cRect[1] // Center Y
        val tw: Int = nodeData.cRect[2] // Total width
        val th: Int = nodeData.cRect[3] // Total height
        val hw: Int = tw / 2 // Half width
        val hh: Int = th / 2 // Half height

        // Get the index of the first child
        val childIndex: Int = nodes[nodeData.index].firstIdx

        // Check which children overlap the bounding box
        if (edges.top >= cy) { // Top edge intersects or is above the center
            if (edges.left <= cx) { // Left edge intersects or is left of the center
                toProcess.add(QTNodeData(cx - hw, cy, hw, th - hh, childIndex + 0, nodeData.depth + 1)) // Top-left
            }
            if (edges.right >= cx) { // Right edge intersects or is right of the center
                toProcess.add(QTNodeData(cx, cy, tw - hw, th - hh, childIndex + 1, nodeData.depth + 1)) // Top-right
            }
        }
        if (edges.bottom <= cy) { // Bottom edge intersects or is below the center
            if (edges.left <= cx) { // Left edge intersects or is left of the center
                toProcess.add(QTNodeData(cx - hw, cy - hh, hw, hh, childIndex + 2, nodeData.depth + 1)) // Bottom-left
            }
            if (edges.right >= cx) { // Right edge intersects or is right of the center
                toProcess.add(QTNodeData(cx, cy - hh, tw - hw, hh, childIndex + 3, nodeData.depth + 1)) // Bottom-right
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

        // Split the node if it's full
        if (node.count > capacity && nodeData.depth <= MAX_DEPTH) { // Check for size <= 1 to prevent splitting into zero size
            splitNode(nodeData)
        }
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
    // TODO add thread safe removal and traverse only once for all objects
    fun remove(obj: FhysicsObject) {
        // Get the index of the object
        val objIdx: Int = objects.indexOf(obj)

        removeIterative(objIdx, BoundingBoxEdges(obj.boundingBox))
    }

    private fun removeIterative(
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

            val cx: Int = nodeData.cRect[0] // Center X
            val cy: Int = nodeData.cRect[1] // Center Y
            val tw: Int = nodeData.cRect[2] // Total width
            val th: Int = nodeData.cRect[3] // Total height
            val hw: Int = tw / 2 // Half width
            val hh: Int = th / 2 // Half height
            // Odd sizes will be split, so that the right and top sides are larger

            // Check which child node contains the position (favouring the top-left node)
            nodeData =
                if (pos.y >= cy) { // Top side
                    if (pos.x <= cx) { // Left side
                        QTNodeData(cx - hw, cy, hw, th - hh, node.firstIdx + 0, nodeData.depth + 1) // Top-left
                    } else {
                        QTNodeData(cx, cy, tw - hw, th - hh, node.firstIdx + 1, nodeData.depth + 1) // Top-right
                    }
                } else { // Bottom side
                    if (pos.x <= cx) { // Left side
                        QTNodeData(cx - hw, cy - hh, hw, hh, node.firstIdx + 2, nodeData.depth + 1) // Bottom-left
                    } else {
                        QTNodeData(cx, cy - hh, tw - hw, hh, node.firstIdx + 3, nodeData.depth + 1) // Bottom-right
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

    /// region =====Splitting=====
    private fun splitNode(nodeData: QTNodeData) {
        val parent: QTNode = nodes[nodeData.index]
        val firstElementIndex: Int = parent.firstIdx
        val childNodes: Array<QTNodeData> = createChildNodes(nodeData)
        moveElementsToChildren(firstElementIndex, childNodes)
        convertToBranch(parent, childNodes.first())
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

    fun addChildNodeDataToCollection(
        parentData: QTNodeData,
        collection: MutableCollection<QTNodeData>,
        childIndex: Int = nodes[parentData.index].firstIdx, // I love this syntax
    ) {
        val cx: Int = parentData.cRect[0] // Center X
        val cy: Int = parentData.cRect[1] // Center Y
        val tw: Int = parentData.cRect[2] // Total width
        val th: Int = parentData.cRect[3] // Total height
        // Odd sizes will be split, so that the right and top sides are larger
        val hwl: Int = tw / 2 // Half width left
        val hwr: Int = tw - hwl // Half width right
        val hhb: Int = th / 2 // Half height bottom
        val hht: Int = th - hhb // Half height top
        val _0_ = 0 // For better readability

        // Calculate the child nodes and add them to the collection
        // x (bottom left corner), y (bottom left corner), width, height, index, depth
        collection.add(QTNodeData(cx - hwl, cy + _0_, hwl, hht, childIndex + 0, parentData.depth + 1)) // Top-left
        collection.add(QTNodeData(cx + _0_, cy + _0_, hwr, hht, childIndex + 1, parentData.depth + 1)) // Top-right
        collection.add(QTNodeData(cx - hwl, cy - hhb, hwl, hhb, childIndex + 2, parentData.depth + 1)) // Bottom-left
        collection.add(QTNodeData(cx + _0_, cy - hhb, hwr, hhb, childIndex + 3, parentData.depth + 1)) // Bottom-right
    }
    /// endregion
    /// endregion

    /// region
    fun insertPendingAdditions() {
        for (it: FhysicsObject in pendingAdditions) {
            insert(it)
        }
        pendingAdditions.clear()
    }

    fun rebuild() {
        // TODO
    }

    fun clear() {
        root = QTNode()
        nodes.clear()
        nodes.add(root)
        elements.clear()
        objects.clear()
    }

    fun updateFhysicsObjects() {
        for (obj: FhysicsObject in objects) {
            obj.update()
        }
    }

    fun handleCollisions() {
        // TODO
    }

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

    fun updateNodeSizes() {
        // TODO
    }

    fun getObjectCount(): Int {
        return objects.usedCount()
    }

    fun getPendingRemovals(): MutableList<FhysicsObject> {
        return pendingRemovals
    }

    fun shutdownThreadPool() {
        println("Shutting down thread pool")
//        threadPool.shutdownNow() // TODO
    }

    override fun toString(): String {
        return root.toString()
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

        override fun toString(): String {
            return "QuadTreeNode(firstIdx=$firstIdx, count=$count, isLeaf=$isLeaf)"

        }
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

        override fun toString(): String {
            return "QTNodeData(${cRect}, index=$index, depth=$depth)"
        }

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
            println("QTNode(${node.count}, ${nodeData.depth}, ${nodeData.cRect})")
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
