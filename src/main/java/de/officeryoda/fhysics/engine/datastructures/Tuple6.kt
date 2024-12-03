package de.officeryoda.fhysics.engine.datastructures

/**
 * Represents a sextet of values.
 *
 * There is no meaning attached to values in this class, it can be used for any purpose.
 * Tuple6 exhibits value semantics, i.e. two Tuple6 are equal if all six components are equal.
 *
 * @param A type of the first value.
 * @param B type of the second value.
 * @param C type of the third value.
 * @param D type of the fourth value.
 * @param E type of the fifth value.
 * @param F type of the sixth value.
 * @property first First value.
 * @property second Second value.
 * @property third Third value.
 * @property fourth Fourth value.
 * @property fifth Fifth value.
 * @property sixth Sixth value.
 */
data class Tuple6<A, B, C, D, E, F>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E,
    val sixth: F,
) {
    /**
     * Returns string representation of the [Tuple6] including its [first], [second], [third], [fourth], [fifth] and [sixth] values.
     */
    override fun toString(): String = "($first, $second, $third, $fourth, $fifth, $sixth)"
}
