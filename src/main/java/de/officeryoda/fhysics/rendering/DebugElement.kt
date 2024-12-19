package de.officeryoda.fhysics.rendering

import de.officeryoda.fhysics.engine.math.Vector2
import java.awt.Color

/**
 * A debug element that is drawn for a certain amount
 * of [frames][durationFrames] with a certain [color].
 */
abstract class DebugElement(val color: Color, var durationFrames: Int = 240)

/**
 * A point in world space that is drawn for a certain
 * amount of [frames][durationFrames] with a certain [color].
 */
class DebugPoint(
    val position: Vector2,
    color: Color,
    durationFrames: Int = 240,
) : DebugElement(color, durationFrames)

/**
 * A line in world space that is drawn for a certain
 * amount of [frames][durationFrames] with a certain [color].
 */
class DebugLine(
    val start: Vector2,
    val end: Vector2,
    color: Color,
    durationFrames: Int = 240,
) : DebugElement(color, durationFrames)

/**
 * A vector in world space from [support] to [support] + [direction]
 * that is drawn for a certain amount of [frames][durationFrames]
 * with a certain [color].
 */
class DebugVector(
    val support: Vector2,
    val direction: Vector2,
    color: Color,
    durationFrames: Int = 240,
) : DebugElement(color, durationFrames)