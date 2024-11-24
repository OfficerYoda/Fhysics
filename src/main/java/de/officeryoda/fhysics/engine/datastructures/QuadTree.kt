package de.officeryoda.fhysics.engine.datastructures

import de.officeryoda.fhysics.engine.FhysicsCore
import de.officeryoda.fhysics.engine.math.Vector2
import de.officeryoda.fhysics.engine.objects.FhysicsObject
import de.officeryoda.fhysics.extensions.floorToInt
import de.officeryoda.fhysics.rendering.FhysicsObjectDrawer
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
    var root: QTNode = QTNode()
        private set

    private var rootData: QTNodeData = QTNodeData(0, CenterRect(FhysicsCore.BORDER), 0)

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
    fun insert(obj: FhysicsObject) {
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
        val hw: Int = nodeData.cRect[2] / 2 // Half width
        val hh: Int = nodeData.cRect[3] / 2 // Half height

        // Get the index of the first child
        val childIndex: Int = nodes[nodeData.index].firstIdx

        // Check which children overlap the bounding box
        if (edges[0] <= cx) { // Left edge intersects or is left of the center
            if (edges[2] >= cy) { // Top edge intersects or is above the center
                toProcess.add(QTNodeData(cx - hw, cy + 0, hw, hh, childIndex + 0, nodeData.depth + 1)) // Top-left
            }
            if (edges[3] <= cy) { // Bottom edge intersects or is below the center
                toProcess.add(QTNodeData(cx - hw, cy - hh, hw, hh, childIndex + 2, nodeData.depth + 1)) // Bottom-left
            }
        }
        if (edges[1] >= cx) { // Right edge intersects or is right of the center
            if (edges[2] >= cy) { // Top edge intersects or is above the center
                toProcess.add(QTNodeData(cx + 0, cy + 0, hw, hh, childIndex + 1, nodeData.depth + 1)) // Top-right
            }
            if (edges[3] <= cy) { // Bottom edge intersects or is below the center
                toProcess.add(QTNodeData(cx + 0, cy - hh, hw, hh, childIndex + 4, nodeData.depth + 1)) // Bottom-right
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
        if (node.count > capacity && nodeData.depth <= MAX_DEPTH) {
            splitNode(nodeData)
        }
    }

    private fun getLastElement(
        node: QTNode,
        objIdx: Int,
    ): Pair<QTNodeElement, Boolean> {
        // Traverse the element linked list until the last element
        var nodeElement: QTNodeElement = elements[node.firstIdx]
        var objContained = false
        while (nodeElement.next != -1) {
            // Check if the object is already in the node
            if (nodeElement.index == objIdx) return Pair(nodeElement, true)

            nodeElement = elements[nodeElement.next]
        }
        return Pair(nodeElement, false)
    }

    private fun getLastElement(node: QTNode): QTNodeElement {
        // Traverse the element linked list until the last element
        var nodeElement: QTNodeElement = elements[node.firstIdx]
        while (nodeElement.next != -1) {
            nodeElement = elements[nodeElement.next]
        }

        return nodeElement
    }
    /// endregion

    /// region =====Removal=====
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
        objects.remove(objIdx)

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
                    elements.remove(node.firstIdx)
                    node.firstIdx = current.next
                } else {
                    elements.remove(previous.next)
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
            val hw: Int = nodeData.cRect[2] / 2 // Half width
            val hh: Int = nodeData.cRect[3] / 2 // Half height

            // Check which child node contains the position (favouring the top-left node)
            nodeData =
                if (pos.x <= cx) { // Left side
                    if (pos.y >= cy) { // Top side
                        QTNodeData(cx - hw, cy + 0, hw, hh, node.firstIdx + 0, nodeData.depth + 1) // Top-left
                    } else {
                        QTNodeData(cx - hw, cy - hh, hw, hh, node.firstIdx + 2, nodeData.depth + 1) // Bottom-left
                    }
                } else { // Right side
                    if (pos.y >= cy) { // Top side
                        QTNodeData(cx + 0, cy + 0, hw, hh, node.firstIdx + 1, nodeData.depth + 1) // Top-right
                    } else {
                        QTNodeData(cx + 0, cy - hh, hw, hh, node.firstIdx + 3, nodeData.depth + 1) // Bottom-right
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
        val cx: Int = parent.cRect[0] // Center X
        val cy: Int = parent.cRect[1] // Center Y
        val hw: Int = parent.cRect[2] / 2 // Half width
        val hh: Int = parent.cRect[3] / 2 // Half height

        // Calculate the edges of the parents' bounds TODO remove
//        val pl: Int = cx - hw // Parent's left edge
//        val pb: Int = cy - hh // Parent's bottom edge

        // Add new child nodes to the list
        val firstNodeIndex: Int = nodes.add(QTNode()) // Store the index of the first child node
        repeat(3) { nodes.add(QTNode()) } // Add the other 3 child nodes

        // Order of insertion matters
        val qtNodeData: MutableList<QTNodeData> = mutableListOf()
        qtNodeData.add(QTNodeData(cx - hw, cy + 0, hw, hh, firstNodeIndex + 0, parent.depth + 1)) // Top-left child
        qtNodeData.add(QTNodeData(cx + 0, cy + 0, hw, hh, firstNodeIndex + 1, parent.depth + 1)) // Top-right child
        qtNodeData.add(QTNodeData(cx - hw, cy - hh, hw, hh, firstNodeIndex + 2, parent.depth + 1)) // Bottom-left child
        qtNodeData.add(QTNodeData(cx + 0, cy - hh, hw, hh, firstNodeIndex + 3, parent.depth + 1)) // Bottom-right child

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
        // Set the count to -1 to indicate that the node is a branch
        node.count = -1
        // Set the first index to the first child node
        node.firstIdx = firstChildData.index

        // Remove the elements from the parent node
        for (i: Int in 3 downTo 0) {
            // Remove elements in reverse order to maintain correct indices
            elements.remove(node.firstIdx + i)
        }
    }
    /// endregion
    /// endregion

    /// region =====Debugging=====
    fun getCurrentDepth(): Int {
        var maxDepth = 0
        val toProcess: ArrayDeque<QTNodeData> = ArrayDeque()
        toProcess.add(rootData)

        while (toProcess.isNotEmpty()) {
            val nodeData: QTNodeData = toProcess.removeFirst()
            maxDepth = maxOf(maxDepth, nodeData.depth)

            if (!nodes[nodeData.index].isLeaf) {
                val childIdx: Int = nodes[nodeData.index].firstIdx
                val cx: Int = nodeData.cRect[0]
                val cy: Int = nodeData.cRect[1]
                val hw: Int = nodeData.cRect[2] / 2
                val hh: Int = nodeData.cRect[3] / 2

                toProcess.add(QTNodeData(cx - hw, cy + 0, hw, hh, childIdx + 0, nodeData.depth + 1)) // Top-left
                toProcess.add(QTNodeData(cx + 0, cy + 0, hw, hh, childIdx + 1, nodeData.depth + 1)) // Top-right
                toProcess.add(QTNodeData(cx - hw, cy - hh, hw, hh, childIdx + 2, nodeData.depth + 1)) // Bottom-left
                toProcess.add(QTNodeData(cx + 0, cy - hh, hw, hh, childIdx + 3, nodeData.depth + 1)) // Bottom-right
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
        val childIdx: Int = nodes[parentData.index].firstIdx
        val cx: Int = parentData.cRect[0]
        val cy: Int = parentData.cRect[1]
        val hw: Int = parentData.cRect[2] / 2
        val hh: Int = parentData.cRect[3] / 2

        childData.add(QTNodeData(cx - hw, cy + 0, hw, hh, childIdx + 0, parentData.depth + 1)) // Top-left
        childData.add(QTNodeData(cx + 0, cy + 0, hw, hh, childIdx + 1, parentData.depth + 1)) // Top-right
        childData.add(QTNodeData(cx - hw, cy - hh, hw, hh, childIdx + 2, parentData.depth + 1)) // Bottom-left
        childData.add(QTNodeData(cx + 0, cy - hh, hw, hh, childIdx + 3, parentData.depth + 1)) // Bottom-right

        return childData
    }
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
        // TODO
    }

    fun handleCollisions() {
        // TODO
    }

    fun drawObjects(drawer: FhysicsObjectDrawer) {
        // TODO
    }

    fun drawNodes() {
        // TODO
    }

    fun updateNodeSizes() {
        // TODO
    }

    fun getObjectCount(): Int {
        return objects.size()
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
}
