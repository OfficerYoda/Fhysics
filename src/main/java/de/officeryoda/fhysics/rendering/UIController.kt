/**
 * Sample Skeleton for 'ui.fxml' Controller Class
 */

package de.officeryoda.fhysics.rendering

import de.officeryoda.fhysics.engine.FhysicsCore
import de.officeryoda.fhysics.engine.QuadTree
import de.officeryoda.fhysics.engine.Vector2
import de.officeryoda.fhysics.engine.objects.Circle
import de.officeryoda.fhysics.engine.objects.FhysicsObject
import de.officeryoda.fhysics.engine.objects.Rectangle
import javafx.fxml.FXML
import javafx.scene.control.*
import javafx.scene.layout.AnchorPane
import java.awt.Color
import java.util.*

class UIController {

    /// =====Spawn Object=====
    @FXML
    private lateinit var cbSpawnPreview: CheckBox

    @FXML
    private lateinit var txtSpawnRadius: TextField

    @FXML
    private lateinit var txtSpawnWidth: TextField

    @FXML
    private lateinit var txtSpawnHeight: TextField

    /// =====Object Properties=====
    @FXML
    private lateinit var tpProperties: TitledPane

    @FXML
    private lateinit var apProperties: AnchorPane

    @FXML
    private lateinit var cbPropertyStatic: CheckBox

    @FXML
    private lateinit var txtPropertyMass: TextField

    @FXML
    private lateinit var txtPropertyRotation: TextField

    @FXML
    private lateinit var clrPropertyColor: ColorPicker

    /// =====Gravity=====
    @FXML
    private lateinit var txtGravityDirectionX: TextField

    @FXML
    private lateinit var txtGravityDirectionY: TextField

    @FXML
    private lateinit var txtGravityPointX: TextField

    @FXML
    private lateinit var txtGravityPointY: TextField

    @FXML
    private lateinit var txtGravityPointStrength: TextField

    /// =====Time=====
    @FXML
    private lateinit var btnTimePause: ToggleButton

    @FXML
    private lateinit var btnTimeStep: Button

    @FXML
    private lateinit var txtTimeSpeed: TextField

    /// =====QuadTree=====
    @FXML
    private lateinit var cbQuadTree: CheckBox

    @FXML
    private lateinit var cbQTNodeUtilization: CheckBox

    @FXML
    private lateinit var cbOptimizeQTCapacity: CheckBox

    @FXML
    private lateinit var txtQuadTreeCapacity: TextField

    /// =====Debug=====
    @FXML
    private lateinit var cbBoundingBoxes: CheckBox

    @FXML
    private lateinit var cbQTCapacity: CheckBox

    @FXML
    private lateinit var cbMSPU: CheckBox

    @FXML
    private lateinit var cbUPS: CheckBox

    @FXML
    private lateinit var cbObjectCount: CheckBox

    @FXML
    private lateinit var sldWallElasticity: Slider

    @FXML
    private lateinit var lblWallElasticity: Label

    /// =====Spawn Object=====
    @FXML
    fun onSpawnNothingClicked() {
        spawnObjectType = SpawnObjectType.NOTHING
        updateSpawnPreview()
        setSpawnFieldAvailability(radius = false, width = false, height = false)
    }

    @FXML
    fun onSpawnCircleClicked() {
        spawnObjectType = SpawnObjectType.CIRCLE
        updateSpawnPreview()
        setSpawnFieldAvailability(radius = true, width = false, height = false)
    }

    @FXML
    fun onSpawnRectangleClicked() {
        spawnObjectType = SpawnObjectType.RECTANGLE
        updateSpawnPreview()
        setSpawnFieldAvailability(radius = false, width = true, height = true)
    }

    @FXML
    fun onSpawnPolygonClicked() {
        spawnObjectType = SpawnObjectType.POLYGON
        updateSpawnPreview()
        setSpawnFieldAvailability(radius = false, width = false, height = false)
        // Clear the polygon vertices list for a new polygon
        SceneListener.polyVertices.clear()
    }

    fun updateSpawnPreview() {
        if (spawnObjectType == SpawnObjectType.NOTHING) {
            drawer.spawnPreview = null
            return
        }

        val obj: FhysicsObject = when (spawnObjectType) {
            SpawnObjectType.CIRCLE -> Circle(SceneListener.mouseWorldPos.copy(), spawnRadius)
            SpawnObjectType.RECTANGLE -> Rectangle(SceneListener.mouseWorldPos.copy(), spawnWidth, spawnHeight)
            SpawnObjectType.POLYGON -> {
                val circle = Circle(SceneListener.mouseWorldPos.copy(), spawnRadius)
                circle.color = Color.PINK
                circle
            }
            else -> throw IllegalArgumentException("Invalid spawn object type")
        }

        obj.color = Color(obj.color.red, obj.color.green, obj.color.blue, 128)
        drawer.spawnPreview = obj
    }

    private fun setSpawnFieldAvailability(radius: Boolean, width: Boolean, height: Boolean) {
        txtSpawnRadius.isDisable = !radius
        txtSpawnWidth.isDisable = !width
        txtSpawnHeight.isDisable = !height
    }

    @FXML
    fun onSpawnPreviewClicked() {
        drawSpawnPreview = cbSpawnPreview.isSelected
    }

    @FXML
    fun onSpawnRadiusTyped() {
        spawnRadius = parseTextField(txtSpawnRadius)
        updateSpawnPreview()
    }

    @FXML
    fun onSpawnWidthTyped() {
        spawnWidth = parseTextField(txtSpawnWidth)
        updateSpawnPreview()
    }

    @FXML
    fun onSpawnHeightTyped() {
        spawnHeight = parseTextField(txtSpawnHeight)
        updateSpawnPreview()
    }

    /// =====Object Properties=====
    @FXML
    fun onPropertyStaticClicked() {
        drawer.selectedObject!!.static = cbPropertyStatic.isSelected
    }

    @FXML
    fun onPropertyMassTyped() {
        drawer.selectedObject!!.mass = parseTextField(txtPropertyMass, 1.0f)
    }

    @FXML
    fun onPropertyRotationTyped() {
        drawer.selectedObject!!.rotation = parseTextField(txtPropertyRotation) * DEGREES_TO_RADIANS
    }

    @FXML
    fun onPropertyColorAction() {
        drawer.selectedObject!!.color = RenderUtil.paintToColor(clrPropertyColor.value)
    }

    @FXML
    fun onPropertyRemoveClicked() {
        drawer.selectedObject?.let {
            QuadTree.removeQueue.add(it)
            drawer.selectedObject = null
        }
    }

    fun expandObjectPropertiesPane() {
        tpProperties.isExpanded = true
    }

    fun updateObjectPropertiesValues() {
        apProperties.isDisable = drawer.selectedObject == null
        if (drawer.selectedObject == null) return

        val obj: FhysicsObject = drawer.selectedObject!!

        cbPropertyStatic.isSelected = obj.static
        txtPropertyMass.text = toStringWithTwoDecimalPlaces(obj.mass)
        txtPropertyRotation.text = toStringWithTwoDecimalPlaces(obj.rotation * RADIANS_TO_DEGREES)
        clrPropertyColor.value = RenderUtil.colorToPaint(obj.color) as javafx.scene.paint.Color
    }

    /**
     * Rounds a float value to two decimal places and converts it to a string.
     *
     * @param value The float value to convert.
     * @return The string representation of the float value with two decimal places.
     */
    private fun toStringWithTwoDecimalPlaces(value: Float): String {
        return ((value * 100).toInt() / 100.0f).toString()
    }

    /// =====Gravity=====
    @FXML
    fun onGravityDirectionClicked() {
        gravityType = GravityType.DIRECTIONAL
        setGravityFieldsAvailability(direction = true, point = false)
    }

    @FXML
    fun onGravityPointClicked() {
        gravityType = GravityType.TOWARDS_POINT
        setGravityFieldsAvailability(direction = false, point = true)
    }

    @FXML
    fun onGravityDirectionXTyped() {
        gravityDirection.x = parseTextField(txtGravityDirectionX)
    }

    @FXML
    fun onGravityDirectionYTyped() {
        gravityDirection.y = parseTextField(txtGravityDirectionY)
    }

    @FXML
    fun onGravityPointXTyped() {
        gravityPoint.x = parseTextField(txtGravityPointX)
    }

    @FXML
    fun onGravityPointYTyped() {
        gravityPoint.y = parseTextField(txtGravityPointY)
    }

    @FXML
    fun onGravityStrengthTyped() {
        gravityPointStrength = parseTextField(txtGravityPointStrength)
    }

    private fun setGravityFieldsAvailability(direction: Boolean, point: Boolean) {
        txtGravityDirectionX.isDisable = !direction
        txtGravityDirectionY.isDisable = !direction

        txtGravityPointX.isDisable = !point
        txtGravityPointY.isDisable = !point
        txtGravityPointStrength.isDisable = !point
    }

    /// =====Time=====
    @FXML
    fun onTimePauseClicked() {
        FhysicsCore.running = !btnTimePause.isSelected
        btnTimeStep.isDisable = FhysicsCore.running
    }

    @FXML
    fun onTimeStepClicked() {
        FhysicsCore.update()
    }

    @FXML
    fun onTimeSpeedTyped() {
        timeSpeed = parseTextField(txtTimeSpeed)
        FhysicsCore.dt = 1.0f / FhysicsCore.UPDATES_PER_SECOND * timeSpeed
    }

    /// =====QuadTree=====
    @FXML
    fun onQuadTreeClicked() {
        drawQuadTree = cbQuadTree.isSelected

        // Node utilization is only drawn if the quad tree is drawn
        cbQTNodeUtilization.isDisable = !drawQuadTree
    }

    @FXML
    fun onQTNodeUtilizationClicked() {
        drawQTNodeUtilization = cbQTNodeUtilization.isSelected
    }

    @FXML
    fun onOptimizeQTCapacityClicked() {
        optimizeQTCapacity = cbOptimizeQTCapacity.isSelected

        // Disable manual capacity input if the capacity is being optimized
        txtQuadTreeCapacity.isDisable = optimizeQTCapacity
        txtQuadTreeCapacity.text = QuadTree.capacity.toString()
    }

    @FXML
    fun onQuadTreeCapacityTyped() {
        val capacity: Int = txtQuadTreeCapacity.text.toIntOrNull() ?: 0
        if (capacity > 0) {
            QuadTree.capacity = capacity
            QuadTree.root.tryDivide()
        }
    }

    /// =====Debug=====
    @FXML
    fun onBoundingBoxesClicked() {
        drawBoundingBoxes = !drawBoundingBoxes
    }

    @FXML
    fun onQTCapacityClicked() {
        drawQTCapacity = cbQTCapacity.isSelected
    }

    @FXML
    fun onMSPUClicked() {
        drawMSPU = cbMSPU.isSelected
    }

    @FXML
    fun onUPSClicked() {
        drawUPS = cbUPS.isSelected
    }

    @FXML
    fun onObjectCountClicked() {
        drawObjectCount = cbObjectCount.isSelected
    }

    @FXML
    fun onWallElasticityChanged() {
        wallElasticity = sldWallElasticity.value.toFloat()
        lblWallElasticity.text = String.format(Locale.US, "%.2f", wallElasticity)
    }

    /// =====Initialization and helper=====
    @FXML // This method is called by the FXMLLoader when initialization is complete
    fun initialize() {
        /// =====Singleton=====
        instance = this
        drawer = RenderUtil.drawer

        /// =====Object Properties=====
        restrictToNumericInput(txtPropertyMass, false)
        restrictToNumericInput(txtPropertyRotation)

        /// =====Spawn Object=====
        cbSpawnPreview.isSelected = drawSpawnPreview
        txtSpawnRadius.text = spawnRadius.toString()
        txtSpawnWidth.text = spawnWidth.toString()
        txtSpawnHeight.text = spawnHeight.toString()
        txtSpawnWidth.isDisable = true
        txtSpawnHeight.isDisable = true

        restrictToNumericInput(txtSpawnRadius, false)
        restrictToNumericInput(txtSpawnWidth, false)
        restrictToNumericInput(txtSpawnHeight, false)

        when (spawnObjectType) {
            SpawnObjectType.NOTHING -> onSpawnNothingClicked()
            SpawnObjectType.CIRCLE -> onSpawnCircleClicked()
            SpawnObjectType.RECTANGLE -> onSpawnRectangleClicked()
            SpawnObjectType.POLYGON -> onSpawnPolygonClicked()
        }

        /// =====Gravity=====
        txtGravityDirectionX.text = gravityDirection.x.toString()
        txtGravityDirectionY.text = gravityDirection.y.toString()
        txtGravityPointX.text = gravityPoint.x.toString()
        txtGravityPointY.text = gravityPoint.y.toString()
        txtGravityPointStrength.text = gravityPointStrength.toString()
        txtGravityDirectionX.isDisable = gravityType != GravityType.DIRECTIONAL
        txtGravityDirectionY.isDisable = gravityType != GravityType.DIRECTIONAL
        txtGravityPointX.isDisable = gravityType != GravityType.TOWARDS_POINT
        txtGravityPointY.isDisable = gravityType != GravityType.TOWARDS_POINT
        txtGravityPointStrength.isDisable = gravityType != GravityType.TOWARDS_POINT

        restrictToNumericInput(txtGravityDirectionX)
        restrictToNumericInput(txtGravityDirectionY)
        restrictToNumericInput(txtGravityPointX)
        restrictToNumericInput(txtGravityPointY)
        restrictToNumericInput(txtGravityPointStrength)

        /// =====Time=====
        btnTimePause.isSelected = !FhysicsCore.running
        btnTimeStep.isDisable = FhysicsCore.running
        txtTimeSpeed.text = timeSpeed.toString()

        restrictToNumericInput(txtTimeSpeed, false)

        /// =====QuadTree=====
        cbQuadTree.isSelected = drawQuadTree
        cbQTNodeUtilization.isDisable = !drawQuadTree
        cbQTNodeUtilization.isSelected = drawQTNodeUtilization
        cbOptimizeQTCapacity.isSelected = optimizeQTCapacity
        txtQuadTreeCapacity.text = QuadTree.capacity.toString()

        restrictToNumericInput(txtQuadTreeCapacity, false)

        /// =====Debug=====
        cbBoundingBoxes.isSelected = drawBoundingBoxes
        cbObjectCount.isSelected = drawObjectCount
        cbMSPU.isSelected = drawMSPU
        cbUPS.isSelected = drawUPS

        sldWallElasticity.value = wallElasticity.toDouble()
        lblWallElasticity.text = String.format(Locale.US, "%.2f", wallElasticity)
    }

    /**
     * Restricts the input of a text field to numeric values.
     *
     * @param textField The text field to restrict.
     * @param allowNegatives Whether negative values are allowed.
     */
    private fun restrictToNumericInput(textField: TextField, allowNegatives: Boolean = true) {
        textField.textProperty().addListener { _, oldValue, newValue ->
            val regexPattern: String = if (allowNegatives) "-?\\d*\\.?\\d*" else "\\d*\\.?\\d*"
            if (!newValue.matches(regexPattern.toRegex())) {
                textField.text = oldValue
            }
        }
    }

    /**
     * Parses the text of a text field to a float.
     * If the text cannot be parsed, the default value is returned.
     *
     * @param textField The text field to parse.
     * @param default The default value to return if the text cannot be parsed.
     * @return The parsed float value or the default value if the text cannot be parsed.
     */
    private fun parseTextField(textField: TextField, default: Float = 0.0f): Float {
        return textField.text.toFloatOrNull() ?: default
    }

    companion object {
        /// =====Singleton=====
        lateinit var instance: UIController
        lateinit var drawer: FhysicsObjectDrawer

        /// =====Spawn Object=====
        var spawnObjectType: SpawnObjectType = SpawnObjectType.RECTANGLE
            private set
        var drawSpawnPreview: Boolean = true
            private set
        var spawnRadius: Float = 1.0f
            private set
        var spawnWidth: Float = 1.0f
            private set
        var spawnHeight: Float = 1.0f
            private set

        /// =====Object Properties=====
        private const val DEGREES_TO_RADIANS: Float = 0.017453292f
        private const val RADIANS_TO_DEGREES: Float = 57.29578f

        /// =====Gravity=====
        var gravityType: GravityType = GravityType.DIRECTIONAL
            private set
        val gravityDirection: Vector2 = Vector2(0.0f, -10.0f)
        val gravityPoint: Vector2 = Vector2( // The center of the world
            (FhysicsCore.BORDER.width / 2.0).toFloat(),
            (FhysicsCore.BORDER.height / 2.0).toFloat()
        )
        var gravityPointStrength: Float = 100.0f
            private set

        /// =====Time=====
        var timeSpeed: Float = 1.0f
            private set

        /// =====QuadTree=====
        var drawQuadTree: Boolean = false
            private set
        var drawQTNodeUtilization: Boolean = true
            private set
        var optimizeQTCapacity: Boolean = false
            private set

        /// =====Debug=====
        var drawBoundingBoxes: Boolean = false
            private set
        var drawQTCapacity: Boolean = false
            private set
        var drawMSPU: Boolean = true
            private set
        var drawUPS: Boolean = false
            private set
        var drawObjectCount: Boolean = false
            private set

        var wallElasticity: Float = 1.0f
            private set
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