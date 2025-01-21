package de.officeryoda.fhysics.engine

import de.officeryoda.fhysics.engine.math.Vector2
import de.officeryoda.fhysics.visual.UIController
import java.awt.Color

/**
 * The settings for the simulation.
 * Every variable in this class can be changed at runtime.
 */
data object Settings {

    private val defaultSettings = mapOf<String, Any>(
        "spawnObjectType" to SpawnObjectType.NOTHING,
        "spawnStatic" to false,
        "spawnRadius" to 1.0f,
        "spawnWidth" to 1.0f,
        "spawnHeight" to 1.0f,
        "useCustomColor" to false,
        "spawnColor" to Color(255, 255, 255),
        "gravityType" to GravityType.DIRECTIONAL,
        "gravityDirection" to Vector2(0.0f, -0.0f),
        "gravityPoint" to Vector2( // Default: The center of the world
            (FhysicsCore.border.width / 2.0).toFloat(),
            (FhysicsCore.border.height / 2.0).toFloat()
        ),
        "gravityPointStrength" to 100.0f,
        "damping" to 0.000f,
        "timeScale" to 1.0f,
        "borderRestitution" to 0.5f,
        "borderFrictionStatic" to 0.5f,
        "borderFrictionDynamic" to 0.45f,
        "drawQuadTree" to false,
        "drawQTNodeUtilization" to true,
        "showBoundingBoxes" to false,
        "showSubPolygons" to false,
        "showQTCapacity" to false,
        "showMSPU" to false,
        "showUPS" to false,
        "showSubSteps" to false,
        "showObjectCount" to false,
        "showRenderTime" to false
    )

    /// region =====General=====
    /** The amount of updates the simulation should perform per second */
    const val UPDATES_PER_SECOND: Int = 60

    /** The amount of sub steps the simulation performs per update */
    const val SUB_STEPS: Int = 4

    /** A small value used for floating point comparisons */
    const val EPSILON: Float = 1E-4f
    /// endregion

    // regions are based on the UI
    /// region =====Spawn Object=====
    var spawnObjectType: SpawnObjectType = SpawnObjectType.NOTHING
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
        (FhysicsCore.border.width / 2.0).toFloat(),
        (FhysicsCore.border.height / 2.0).toFloat()
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
    var drawQuadTree: Boolean = false
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

    fun load(settings: Map<String, Any>) {
        settings["spawnObjectType"]?.let { spawnObjectType = it as SpawnObjectType }
        settings["spawnStatic"]?.let { spawnStatic = it as Boolean }
        settings["spawnRadius"]?.let { spawnRadius = (it as Number).toFloat() }
        settings["spawnWidth"]?.let { spawnWidth = (it as Number).toFloat() }
        settings["spawnHeight"]?.let { spawnHeight = (it as Number).toFloat() }
        settings["useCustomColor"]?.let { useCustomColor = it as Boolean }
        settings["spawnColor"]?.let { spawnColor = it as Color }
        settings["gravityType"]?.let { gravityType = it as GravityType }
        settings["gravityDirection"]?.let { gravityDirection.set(it as Vector2) }
        settings["gravityPoint"]?.let { gravityPoint.set(it as Vector2) }
        settings["gravityPointStrength"]?.let { gravityPointStrength = (it as Number).toFloat() }
        settings["damping"]?.let { damping = (it as Number).toFloat() }
        settings["timeScale"]?.let { timeScale = (it as Number).toFloat() }
        settings["borderRestitution"]?.let { borderRestitution = (it as Number).toFloat() }
        settings["borderFrictionStatic"]?.let { borderFrictionStatic = (it as Number).toFloat() }
        settings["borderFrictionDynamic"]?.let { borderFrictionDynamic = (it as Number).toFloat() }
        settings["drawQuadTree"]?.let { drawQuadTree = it as Boolean }
        settings["drawQTNodeUtilization"]?.let { drawQTNodeUtilization = it as Boolean }
        settings["showBoundingBoxes"]?.let { showBoundingBoxes = it as Boolean }
        settings["showSubPolygons"]?.let { showSubPolygons = it as Boolean }
        settings["showQTCapacity"]?.let { showQTCapacity = it as Boolean }
        settings["showMSPU"]?.let { showMSPU = it as Boolean }
        settings["showUPS"]?.let { showUPS = it as Boolean }
        settings["showSubSteps"]?.let { showSubSteps = it as Boolean }
        settings["showObjectCount"]?.let { showObjectCount = it as Boolean }
        settings["showRenderTime"]?.let { showRenderTime = it as Boolean }

        UIController.updateUiFlag = true
    }

    /**
     * Loads the default settings.
     */
    fun loadDefault() {
        load(defaultSettings)
    }
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
