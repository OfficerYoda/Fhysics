package de.officeryoda.fhysics.rendering

import de.officeryoda.fhysics.engine.math.Vector2
import java.awt.Color

abstract class DebugElement(val color: Color, var durationFrames: Int = 240)

class DebugPoint(
    val position: Vector2,
    color: Color,
    durationFrames: Int = 240,
) : DebugElement(color, durationFrames)

class DebugLine(
    val start: Vector2,
    val end: Vector2,
    color: Color,
    durationFrames: Int = 240,
) :
    DebugElement(color, durationFrames)

class DebugVector(
    val support: Vector2,
    val direction: Vector2,
    color: Color,
    durationFrames: Int = 240,
) :
    DebugElement(color, durationFrames)