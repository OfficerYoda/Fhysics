package de.officeryoda.fhysics.engine.datastructures


data class QTNodeElement(
    /** The index of the object in [QuadTree.objects] */
    val index: Int,
    /** The index of the next element in the node within [QuadTree.elements], or -1 if it is the last element */
    var next: Int,
)

