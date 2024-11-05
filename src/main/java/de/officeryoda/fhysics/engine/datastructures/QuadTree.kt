package de.officeryoda.fhysics.engine.datastructures

import de.officeryoda.fhysics.engine.FhysicsCore
import de.officeryoda.fhysics.engine.objects.FhysicsObject
import java.util.concurrent.Executors

class QuadTree {

    private val root: QuadTreeNode = QuadTreeNode(FhysicsCore.BORDER, null)

    fun insertPendingAdditions() {
        pendingAdditions.forEach { root.insert(it) }
        pendingAdditions.clear()
    }

    companion object {
        val executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())

        // Pool of nodes to prevent excessive object creation
        val nodePool: ObjectPool<QuadTreeNode> = ObjectPool()

        // List of objects to add, used to queue up insertions and prevent concurrent modification
        val pendingAdditions: MutableList<FhysicsObject> = ArrayList()

        // Set of objects to remove, used to mark objects for deletion safely
        val pendingRemovals: MutableSet<FhysicsObject> = HashSet()
    }
}