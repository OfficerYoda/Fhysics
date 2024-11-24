package de.officeryoda.fhysics.engine.datastructures


data class QTNodeElement(
    /** The index of the object in [QuadTree.objects] */
    val index: Int,
    /** The index of the element in the node or -1 if it's the last element */
    var next: Int,
)

