/**
 * Sample Skeleton for 'ui.fxml' Controller Class
 */

package de.officeryoda.fhysics.rendering

import de.officeryoda.fhysics.engine.FhysicsCore
import de.officeryoda.fhysics.engine.QuadTree
import de.officeryoda.fhysics.engine.Vector2
import de.officeryoda.fhysics.engine.collision.CollisionSolver
import de.officeryoda.fhysics.engine.objects.FhysicsObject
import de.officeryoda.fhysics.rendering.BetterSceneListener.polyVertices
import de.officeryoda.fhysics.rendering.BetterSceneListener.selectedObject
import de.officeryoda.fhysics.rendering.BetterSceneListener.spawnPreview
import de.officeryoda.fhysics.rendering.BetterSceneListener.updateSpawnPreview
import javafx.fxml.FXML
import javafx.scene.control.*
import javafx.scene.layout.AnchorPane
import java.awt.Color
import java.util.*
import kotlin.math.max

class UIController {

    /// region ========Fields========

    /// region =====Fields: Spawn Object=====
    @FXML
    private lateinit var cbSpawnPreview: CheckBox

    @FXML
    private lateinit var cbSpawnStatic: CheckBox

    @FXML
    private lateinit var txtSpawnRadius: TextField

    @FXML
    private lateinit var txtSpawnWidth: TextField

    @FXML
    private lateinit var txtSpawnHeight: TextField

    @FXML
    private lateinit var cbCustomColor: CheckBox

    @FXML
    private lateinit var clrSpawnColor: ColorPicker
    /// endregion

    /// region =====Fields: Object Properties=====
    @FXML
    private lateinit var tpProperties: TitledPane

    @FXML
    private lateinit var apProperties: AnchorPane

    @FXML
    private lateinit var cbPropertyStatic: CheckBox

    @FXML
    private lateinit var clrPropertyColor: ColorPicker

    @FXML
    private lateinit var txtPropertyMass: TextField

    @FXML
    private lateinit var txtPropertyRotation: TextField

    @FXML
    private lateinit var sldPropertyRestitution: Slider

    @FXML
    private lateinit var lblPropertyRestitution: Label

    @FXML
    lateinit var sldPropertyFrictionStatic: Slider

    @FXML
    lateinit var lblPropertyFrictionStatic: Label

    @FXML
    lateinit var sldPropertyFrictionDynamic: Slider

    @FXML
    lateinit var lblPropertyFrictionDynamic: Label
    /// endregion

    /// region =====Fields: Gravity=====
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
    /// endregion

    /// region =====Fields: Miscellaneous=====
    @FXML
    private lateinit var btnTimePause: ToggleButton

    @FXML
    private lateinit var btnTimeStep: Button

    @FXML
    private lateinit var txtTimeSpeed: TextField

    @FXML
    private lateinit var txtBorderWidth: TextField

    @FXML
    private lateinit var txtBorderHeight: TextField

    @FXML
    private lateinit var sldBorderRestitution: Slider

    @FXML
    private lateinit var lblBorderRestitution: Label

    @FXML
    private lateinit var sldBorderFrictionStatic: Slider

    @FXML
    private lateinit var lblBorderFrictionStatic: Label

    @FXML
    private lateinit var sldBorderFrictionDynamic: Slider

    @FXML
    private lateinit var lblBorderFrictionDynamic: Label
    /// endregion

    /// region =====Fields: QuadTree=====
    @FXML
    private lateinit var cbQuadTree: CheckBox

    @FXML
    private lateinit var cbQTNodeUtilization: CheckBox

    @FXML
    private lateinit var cbOptimizeQTCapacity: CheckBox

    @FXML
    private lateinit var txtQuadTreeCapacity: TextField
    /// endregion

    /// region =====Fields: Debug=====
    @FXML
    private lateinit var cbBoundingBoxes: CheckBox

    @FXML
    private lateinit var cbSubPolygons: CheckBox

    @FXML
    private lateinit var cbQTCapacity: CheckBox

    @FXML
    private lateinit var cbMSPU: CheckBox

    @FXML
    private lateinit var cbUPS: CheckBox

    @FXML
    private lateinit var cbObjectCount: CheckBox

    @FXML
    private lateinit var cbRenderTime: CheckBox
    /// endregion

    /// endregion

    /// region ========Methods========

    /// region =====Methods: Spawn Object=====
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
        polyVertices.clear()
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
    fun onSpawnStaticClicked() {
        spawnStatic = cbSpawnStatic.isSelected
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

    @FXML
    fun onCustomColorClicked() {
        customColor = cbCustomColor.isSelected
        clrSpawnColor.isDisable = !cbCustomColor.isSelected

        if (cbCustomColor.isSelected) {
            spawnPreview?.color = spawnColor
        }
    }

    @FXML
    fun onSpawnColorAction() {
        spawnColor = RenderUtil.paintToColor(clrSpawnColor.value)
        spawnPreview?.color = spawnColor
    }
    /// endregion

    /// region =====Methods: Object Properties=====
    @FXML
    fun onPropertyStaticClicked() {
        selectedObject!!.static = cbPropertyStatic.isSelected

        // Update the bounding box
        selectedObject!!.updateBoundingBox()
    }

    @FXML
    fun onPropertyColorAction() {
        selectedObject!!.color = RenderUtil.paintToColor(clrPropertyColor.value)
    }

    @FXML
    fun onPropertyMassTyped() {
        selectedObject!!.mass = parseTextField(txtPropertyMass, 1.0f).coerceAtLeast(0.01f)
    }

    @FXML
    fun onPropertyRotationTyped() {
        selectedObject!!.angle = parseTextField(txtPropertyRotation) * DEGREES_TO_RADIANS

        // Update the bounding box
        selectedObject!!.updateBoundingBox()
    }

    @FXML
    fun onPropertyRestitutionChanged() {
        selectedObject!!.restitution = sldPropertyRestitution.value.toFloat()
        lblPropertyRestitution.text = toRoundedString(selectedObject!!.restitution)
    }

    @FXML
    fun onPropertyFrictionStaticChanged() {
        selectedObject!!.frictionStatic = sldPropertyFrictionStatic.value.toFloat()
        lblPropertyFrictionStatic.text = toRoundedString(selectedObject!!.frictionStatic)
    }

    @FXML
    fun onPropertyFrictionDynamicChanged() {
        selectedObject!!.frictionDynamic = sldPropertyFrictionDynamic.value.toFloat()
        lblPropertyFrictionDynamic.text = toRoundedString(selectedObject!!.frictionDynamic)
    }

    @FXML
    fun onPropertyRemoveClicked() {
        selectedObject?.let {
            QuadTree.removeQueue.add(it)
            selectedObject = null
        }

        updateObjectPropertiesValues()
    }

    /**
     * Expands the object properties pane.
     */
    fun expandObjectPropertiesPane() {
        tpProperties.isExpanded = true
        updateObjectPropertiesValues()
    }

    /**
     * Updates the values of the object properties fields.
     *
     * If no object is selected, the fields are disabled.
     * If an object is selected, the fields are enabled and filled with the object's values.
     */
    private fun updateObjectPropertiesValues() {
        apProperties.isDisable = selectedObject == null
        if (selectedObject == null) return

        val obj: FhysicsObject = selectedObject!!

        cbPropertyStatic.isSelected = obj.static
        clrPropertyColor.value = RenderUtil.colorToPaint(obj.color) as javafx.scene.paint.Color
        txtPropertyMass.text = toRoundedString(obj.mass)
        txtPropertyRotation.text = toRoundedString(selectedObject!!.angle * RADIANS_TO_DEGREES)
        setSliderAndLabel(sldPropertyRestitution, lblPropertyRestitution, obj.restitution)
        setSliderAndLabel(sldPropertyFrictionStatic, lblPropertyFrictionStatic, obj.frictionStatic)
        setSliderAndLabel(sldPropertyFrictionDynamic, lblPropertyFrictionDynamic, obj.frictionDynamic)
    }
    /// endregion

    /// region =====Methods: Scene=====
    @FXML
    fun onSceneClearClicked() {
        FhysicsCore.clear()
    }
    /// endregion

    /// region =====Methods: Gravity=====
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
    /// endregion

    /// region =====Methods: Miscellaneous=====
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
        FhysicsCore.dt = 1.0f / (FhysicsCore.UPDATES_PER_SECOND * FhysicsCore.SUB_STEPS) * timeSpeed
    }

    @FXML
    fun onBorderWidthTyped() {
        val width: Float = parseTextField(txtBorderWidth, 1f)
        FhysicsCore.BORDER.width = max(width, 1f) // Minimum border size of 1x1
        CollisionSolver.updateBorderObjects()

        // Make sure the text field matches the actual border width
        if (width < 1f) {
            txtBorderWidth.text = "1.0"
        }
    }

    @FXML
    fun onBorderHeightTyped() {
        val height: Float = parseTextField(txtBorderHeight, 1f)
        FhysicsCore.BORDER.height = max(height, 1f) // Minimum border size of 1x1
        CollisionSolver.updateBorderObjects()

        // Make sure the text field matches the actual border height
        if (height < 1f) {
            txtBorderHeight.text = "1.0"
        }
    }

    @FXML
    fun onBorderRestitutionChanged() {
        borderRestitution = sldBorderRestitution.value.toFloat()
        lblBorderRestitution.text = toRoundedString(borderRestitution)
    }

    @FXML
    fun onBorderFrictionStaticChanged() {
        borderFrictionStatic = sldBorderFrictionStatic.value.toFloat()
        lblBorderFrictionStatic.text = toRoundedString(borderFrictionStatic)
    }

    @FXML
    fun onBorderFrictionDynamicChanged() {
        borderFrictionDynamic = sldBorderFrictionDynamic.value.toFloat()
        lblBorderFrictionDynamic.text = toRoundedString(borderFrictionDynamic)
    }
    /// endregion

    /// region =====Methods: QuadTree=====
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
            QuadTree.divideNextUpdate = true
        }
    }
    /// endregion

    /// region =====Methods: Debug=====
    @FXML
    fun onBoundingBoxesClicked() {
        drawBoundingBoxes = !drawBoundingBoxes
    }

    @FXML
    fun onSubPolygonsClicked() {
        drawSubPolygons = cbSubPolygons.isSelected
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
    fun onRenderTimeClicked() {
        drawRenderTime = cbRenderTime.isSelected
    }
    /// endregion

    /// region =====Initialization and helper=====
    @FXML // This method is called by the FXMLLoader when initialization is complete
    fun initialize() {
        /// region =====Singleton=====
        instance = this
        drawer = RenderUtil.drawer
        /// endregion

        /// region =====Object Properties=====
        restrictToNumericInput(txtPropertyMass, false)
        restrictToNumericInput(txtPropertyRotation)
        /// endregion

        /// region =====Spawn Object=====
        cbSpawnPreview.isSelected = drawSpawnPreview
        cbSpawnStatic.isSelected = spawnStatic
        txtSpawnRadius.text = spawnRadius.toString()
        txtSpawnWidth.text = spawnWidth.toString()
        txtSpawnHeight.text = spawnHeight.toString()
        txtSpawnWidth.isDisable = true
        txtSpawnHeight.isDisable = true
        cbCustomColor.isSelected = customColor
        clrSpawnColor.value = RenderUtil.colorToPaint(spawnColor) as javafx.scene.paint.Color

        restrictToNumericInput(txtSpawnRadius, false)
        restrictToNumericInput(txtSpawnWidth, false)
        restrictToNumericInput(txtSpawnHeight, false)

        when (spawnObjectType) {
            SpawnObjectType.NOTHING -> onSpawnNothingClicked()
            SpawnObjectType.CIRCLE -> onSpawnCircleClicked()
            SpawnObjectType.RECTANGLE -> onSpawnRectangleClicked()
            SpawnObjectType.POLYGON -> onSpawnPolygonClicked()
        }
        /// endregion

        /// region =====Gravity=====
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
        /// endregion

        /// region =====Miscellaneous=====
        btnTimePause.isSelected = !FhysicsCore.running
        btnTimeStep.isDisable = FhysicsCore.running
        txtTimeSpeed.text = timeSpeed.toString()
        txtBorderWidth.text = FhysicsCore.BORDER.width.toString()
        txtBorderHeight.text = FhysicsCore.BORDER.height.toString()
        setSliderAndLabel(sldBorderRestitution, lblBorderRestitution, borderRestitution)
        setSliderAndLabel(sldBorderFrictionStatic, lblBorderFrictionStatic, borderFrictionStatic)
        setSliderAndLabel(sldBorderFrictionDynamic, lblBorderFrictionDynamic, borderFrictionDynamic)

        restrictToNumericInput(txtTimeSpeed, false)
        restrictToNumericInput(txtBorderWidth, false)
        restrictToNumericInput(txtBorderHeight, false)
        /// endregion

        /// region =====QuadTree=====
        cbQuadTree.isSelected = drawQuadTree
        cbQTNodeUtilization.isDisable = !drawQuadTree
        cbQTNodeUtilization.isSelected = drawQTNodeUtilization
        cbOptimizeQTCapacity.isSelected = optimizeQTCapacity
        txtQuadTreeCapacity.text = QuadTree.capacity.toString()

        restrictToNumericInput(txtQuadTreeCapacity, false)
        /// endregion

        /// region =====Debug=====
        cbBoundingBoxes.isSelected = drawBoundingBoxes
        cbSubPolygons.isSelected = drawSubPolygons
        cbObjectCount.isSelected = drawObjectCount
        cbMSPU.isSelected = drawMSPU
        cbUPS.isSelected = drawUPS
        /// endregion
    }

    /**
     * Rounds a float value to two decimal places and converts it to a string.
     *
     * @param value The float value to convert.
     * @return The string representation of the float value with two decimal places.
     */
    private fun toRoundedString(value: Float): String {
        return String.format(Locale.US, "%.2f", value)
    }

    /**
     * Sets the value of a slider and its corresponding label.
     *
     * @param slider The slider to set the value of.
     * @param label The label to set the text of.
     * @param value The value to set the slider and label to.
     */
    private fun setSliderAndLabel(slider: Slider, label: Label, value: Float) {
        slider.value = value.toDouble()
        label.text = toRoundedString(value)
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
    /// endregion

    /// endregion

    companion object {
        /// region =====Singleton=====
        lateinit var instance: UIController
        lateinit var drawer: FhysicsObjectDrawer
        /// endregion

        /// region =====Spawn Object=====
        var spawnObjectType: SpawnObjectType = SpawnObjectType.CIRCLE
            private set
        var drawSpawnPreview: Boolean = true
            private set
        var spawnStatic: Boolean = false
            private set
        var spawnRadius: Float = 1.0f
            private set
        var spawnWidth: Float = 1.0f
            private set
        var spawnHeight: Float = 1.0f
            private set
        var customColor: Boolean = false
            private set
        var spawnColor: Color = Color(255, 255, 255)
            private set
        /// endregion

        /// region =====Object Properties=====
        private const val DEGREES_TO_RADIANS: Float = 0.017453292f
        private const val RADIANS_TO_DEGREES: Float = 57.29578f
        /// endregion

        /// region =====Gravity=====
        var gravityType: GravityType = GravityType.DIRECTIONAL
            private set
        val gravityDirection: Vector2 = Vector2(0.0f, -10.0f)
        val gravityPoint: Vector2 = Vector2( // The center of the world
            (FhysicsCore.BORDER.width / 2.0).toFloat(),
            (FhysicsCore.BORDER.height / 2.0).toFloat()
        )
        var gravityPointStrength: Float = 100.0f
            private set
        /// endregion

        /// region =====Miscellaneous=====
        var timeSpeed: Float = 1.0f
            private set
        var borderRestitution: Float = 0.5f
            private set
        var borderFrictionStatic: Float = 0f
            private set
        var borderFrictionDynamic: Float = 0f
            private set
        /// endregion

        /// region =====QuadTree=====
        var drawQuadTree: Boolean = false
            private set
        var drawQTNodeUtilization: Boolean = true
            private set
        var optimizeQTCapacity: Boolean = false
            private set
        /// endregion

        /// region =====Debug=====
        var drawBoundingBoxes: Boolean = false
            private set
        var drawSubPolygons: Boolean = true
            private set
        var drawQTCapacity: Boolean = false
            private set
        var drawMSPU: Boolean = true
            private set
        var drawUPS: Boolean = false
            private set
        var drawObjectCount: Boolean = false
            private set
        var drawRenderTime: Boolean = false
            private set
        /// endregion
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
