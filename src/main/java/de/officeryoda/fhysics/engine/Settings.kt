package de.officeryoda.fhysics.engine

import de.officeryoda.fhysics.engine.math.Vector2
import java.awt.Color

/**
 * The settings for the simulation.
 * Every variable in this class can be changed at runtime.
 */
object Settings {
    // regions are based on the UI
    /// region =====Spawn Object=====
    var spawnObjectType: SpawnObjectType = SpawnObjectType.CIRCLE
    var spawnStatic: Boolean = false
    var spawnRadius: Float = 1.0f
    var spawnWidth: Float = 1.0f
    var spawnHeight: Float = 1.0f
    var useCustomColor: Boolean = false
    var spawnColor: Color = Color(255, 255, 255)
    /// endregion

    /// region =====Forces=====
    var gravityType: GravityType = GravityType.DIRECTIONAL
    val gravityDirection: Vector2 = Vector2(0.0f, -0.0f)

    /** The point towards which the gravity pulls when set to [GravityType.TOWARDS_POINT] */
    val gravityPoint: Vector2 = Vector2( // Default: The center of the world
        (FhysicsCore.BORDER.width / 2.0).toFloat(),
        (FhysicsCore.BORDER.height / 2.0).toFloat()
    )
    var gravityPointStrength: Float = 100.0f
    var damping: Float = 0.000f
    /// endregion

    /// region =====Miscellaneous=====
    /** A Factor to scale the time step */
    var timeScale: Float = 1.0f
    var borderRestitution: Float = 0.5f
    var borderFrictionStatic: Float = 0f
    var borderFrictionDynamic: Float = 0f

    fun setBorderProperties(restitution: Float, frictionStatic: Float, frictionDynamic: Float) {
        borderRestitution = restitution
        borderFrictionStatic = frictionStatic
        borderFrictionDynamic = frictionDynamic
    }
    /// endregion

    /// region =====QuadTree=====
    var drawQuadTree: Boolean = true
    var drawQTNodeUtilization: Boolean = true
    /// endregion

    /// region =====Debug=====
    var showBoundingBoxes: Boolean = false
    var showSubPolygons: Boolean = false
    var showQTCapacity: Boolean = false

    /** Milliseconds per update */
    var showMSPU: Boolean = true

    /** Updates per second (based on MSPU) */
    var showUPS: Boolean = false
    var showSubSteps: Boolean = false
    var showObjectCount: Boolean = true
    var showRenderTime: Boolean = false
    /// endregion
}

enum class SpawnObjectType {
    NOTHING,
    CIRCLE,
    RECTANGLE,
    POLYGON
}

enum class GravityType {
    DIRECTIONAL,
    TOWARDS_POINT
}
