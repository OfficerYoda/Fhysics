package de.officeryoda.fhysics.engine

import de.officeryoda.fhysics.engine.datastructures.QuadTree
import de.officeryoda.fhysics.engine.math.Vector2
import de.officeryoda.fhysics.engine.objects.Circle
import de.officeryoda.fhysics.engine.objects.Polygon
import de.officeryoda.fhysics.engine.objects.Rectangle
import de.officeryoda.fhysics.engine.objects.factories.FhysicsObjectFactory

/**
 * The scene manager is responsible for loading custom scenes.
 */
object SceneManager {

    /** A list of all scenes that can be loaded */
    val scenes: List<CustomScene> = createScenes()

    /** The name of the scene to load */
    private var loadSceneName: String? = null

    /**
     * Loads a scene by its name.
     */
    fun loadScene(sceneName: String?) {
        loadSceneName = sceneName?.split("_")?.first()
    }

    /**
     * Loads the scene that is queued to be loaded.
     *
     * This method exists to prevent concurrent modification exceptions.
     */
    fun loadPendingScene() {
        if (loadSceneName == null) return

        scenes.find { it.name == loadSceneName }?.load()
        loadSceneName = ""
    }

    fun clearScene() {
        QuadTree.clearFlag = true
    }

    /**
     * Creates the custom scenes to be loaded through the UI.
     */
    private fun createScenes(): List<CustomScene> {
        val scenes: MutableList<CustomScene> = mutableListOf<CustomScene>()
        scenes.add(
            CustomScene(
                "1000 Circles (perfect)",
                mapOf(
                    "borderRestitution" to 1f,
                    "borderFrictionStatic" to 0f,
                    "borderFrictionDynamic" to 0f,
                    "showMSPU" to true,
                    "showObjectCount" to true,
                )
            ) {
                repeat(1000) {
                    val circle: Circle = FhysicsObjectFactory.randomCircle().apply {
                        restitution = 1f
                        frictionStatic = 0f
                        frictionDynamic = 0f
                    }
                    FhysicsCore.spawn(circle)
                }
            }
        )

        scenes.add(
            CustomScene(
                "100 Rectangles (perfect)",
                mapOf(
                    "borderRestitution" to 1f,
                    "borderFrictionStatic" to 0f,
                    "borderFrictionDynamic" to 0f,
                    "showMSPU" to true,
                    "showObjectCount" to true,
                )
            ) {
                repeat(100) {
                    val rectangle: Rectangle = FhysicsObjectFactory.randomRectangle().apply {
                        restitution = 1f
                        frictionStatic = 0f
                        frictionDynamic = 0f
                    }
                    FhysicsCore.spawn(rectangle)
                }
            }
        )

        scenes.add(
            CustomScene(
                "100 Polygons (perfect)",
                mapOf(
                    "borderRestitution" to 1f,
                    "borderFrictionStatic" to 0f,
                    "borderFrictionDynamic" to 0f,
                    "showMSPU" to true,
                    "showObjectCount" to true,
                )
            ) {
                repeat(100) {
                    val polygon: Polygon = FhysicsObjectFactory.randomPolygon().apply {
                        restitution = 1f
                        frictionStatic = 0f
                        frictionDynamic = 0f
                    }
                    FhysicsCore.spawn(polygon)
                }
            }
        )

        scenes.add(
            CustomScene(
                "Coefficient Demonstration",
                mapOf(
                    "gravityType" to GravityType.DIRECTIONAL,
                    "gravityDirection" to Vector2(0f, -10f),
                )
            ) {
                val rect1: Rectangle = Rectangle(Vector2(25f, 65f), 30f, 10f).apply {
                    static = true
                    restitution = 0f
                    frictionStatic = 0f
                    frictionDynamic = 0f
                }

                val rect2: Rectangle = Rectangle(Vector2(75f, 65f), 30f, 10f).apply {
                    static = true
                    restitution = 1f
                    frictionStatic = 0f
                    frictionDynamic = 0f
                }

                val rect3: Rectangle = Rectangle(Vector2(25f, 20f), 30f, 10f, Math.toRadians(-30.0).toFloat()).apply {
                    static = true
                    restitution = 0f
                    frictionStatic = 0f
                    frictionDynamic = 0f
                }

                val rect4: Rectangle = Rectangle(Vector2(75f, 20f), 30f, 10f, Math.toRadians(-30.0).toFloat()).apply {
                    static = true
                    restitution = 0f
                    frictionStatic = 1f
                    frictionDynamic = 1f
                }

                val circle1: Circle = Circle(Vector2(25f, 85f), 1f).apply {
                    restitution = 0f
                    frictionStatic = 0f
                    frictionDynamic = 0f
                }

                val circle2: Circle = Circle(Vector2(75f, 85f), 1f).apply {
                    restitution = 1f
                    frictionStatic = 0f
                    frictionDynamic = 0f
                }

                val circle3: Circle = Circle(Vector2(20f, 35f), 1f).apply {
                    restitution = 0f
                    frictionStatic = 0f
                    frictionDynamic = 0f
                }

                val circle4: Circle = Circle(Vector2(70f, 35f), 1f).apply {
                    restitution = 0f
                    frictionStatic = 1f
                    frictionDynamic = 1f
                }

                FhysicsCore.running = false
                FhysicsCore.spawn(rect1, rect2, rect3, rect4, circle1, circle2, circle3, circle4)
            }
        )

        return scenes
    }
}

data class CustomScene(
    /** The name shown in the UI */
    val name: String,
    /** The settings that differ from the default settings */
    private val settings: Map<String, Any>,
    /** The object creation function */
    private val objectCreation: () -> Unit,
) {
    fun load() {
        Settings.load(settings)

        objectCreation()
    }
}
