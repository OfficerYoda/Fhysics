package de.officeryoda.fhysics.rendering

import de.officeryoda.fhysics.engine.FhysicsCore
import de.officeryoda.fhysics.engine.collision.CollisionSolver
import de.officeryoda.fhysics.engine.datastructures.spatial.QuadTree
import de.officeryoda.fhysics.engine.math.Vector2
import de.officeryoda.fhysics.engine.objects.FhysicsObject
import de.officeryoda.fhysics.rendering.SceneListener.polyVertices
import de.officeryoda.fhysics.rendering.SceneListener.selectedObject
import de.officeryoda.fhysics.rendering.SceneListener.spawnPreview
import de.officeryoda.fhysics.rendering.SceneListener.updateSpawnPreview
import javafx.fxml.FXML
import javafx.scene.control.*
import javafx.scene.layout.AnchorPane
import java.awt.Color
import java.util.*
import kotlin.math.max

/**
 * Controller class for the UI.
 *
 * Doubles as a data container for the settings made through the UI. // TODO remove this when Setting class is implemented
 */
class UIController {

    /// region ========Fields========

    /// region =====Fields: Spawn Object=====
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

    /// region =====Fields: Forces=====
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

    @FXML
    private lateinit var sldDamping: Slider

    @FXML
    private lateinit var lblDamping: Label
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
        updateSpawnFieldsAvailability()
    }

    @FXML
    fun onSpawnCircleClicked() {
        spawnObjectType = SpawnObjectType.CIRCLE
        updateSpawnPreview()
        updateSpawnFieldsAvailability()
    }

    @FXML
    fun onSpawnRectangleClicked() {
        spawnObjectType = SpawnObjectType.RECTANGLE
        updateSpawnPreview()
        updateSpawnFieldsAvailability()
    }

    @FXML
    fun onSpawnPolygonClicked() {
        spawnObjectType = SpawnObjectType.POLYGON
        updateSpawnPreview()
        updateSpawnFieldsAvailability()
        // Clear the polygon vertices list for a new polygon
        polyVertices.clear()
    }

    private fun updateSpawnFieldsAvailability() {
        txtSpawnRadius.isDisable = spawnObjectType != SpawnObjectType.CIRCLE
        txtSpawnWidth.isDisable = spawnObjectType != SpawnObjectType.RECTANGLE
        txtSpawnHeight.isDisable = spawnObjectType != SpawnObjectType.RECTANGLE
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
        lblPropertyRestitution.text = roundedToString(selectedObject!!.restitution)
    }

    @FXML
    fun onPropertyFrictionStaticChanged() {
        selectedObject!!.frictionStatic = sldPropertyFrictionStatic.value.toFloat()
        lblPropertyFrictionStatic.text = roundedToString(selectedObject!!.frictionStatic)
    }

    @FXML
    fun onPropertyFrictionDynamicChanged() {
        selectedObject!!.frictionDynamic = sldPropertyFrictionDynamic.value.toFloat()
        lblPropertyFrictionDynamic.text = roundedToString(selectedObject!!.frictionDynamic)
    }

    @FXML
    fun onPropertyRemoveClicked() {
        selectedObject?.let {
            QuadTree.remove(it)
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
        txtPropertyMass.text = roundedToString(obj.mass)
        txtPropertyRotation.text = roundedToString(selectedObject!!.angle * RADIANS_TO_DEGREES)
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

    /// region =====Methods: Forces=====
    @FXML
    fun onGravityDirectionClicked() {
        gravityType = GravityType.DIRECTIONAL
        updateGravityFieldsAvailability()
    }

    @FXML
    fun onGravityPointClicked() {
        gravityType = GravityType.TOWARDS_POINT
        updateGravityFieldsAvailability()
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

    private fun updateGravityFieldsAvailability() {
        txtGravityDirectionX.isDisable = gravityType != GravityType.DIRECTIONAL
        txtGravityDirectionY.isDisable = gravityType != GravityType.DIRECTIONAL

        txtGravityPointX.isDisable = gravityType != GravityType.TOWARDS_POINT
        txtGravityPointY.isDisable = gravityType != GravityType.TOWARDS_POINT
        txtGravityPointStrength.isDisable = gravityType != GravityType.TOWARDS_POINT
    }

    @FXML
    fun onDampingChanged() {
        damping = sldDamping.value.toFloat()
        lblDamping.text = roundedToString(damping, 4)
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
        handleBorderSizeTyped(txtBorderWidth)
    }

    @FXML
    fun onBorderHeightTyped() {
        handleBorderSizeTyped(txtBorderHeight)
    }

    private fun handleBorderSizeTyped(textField: TextField) {
        val size: Float = parseTextField(textField, 1f)
        FhysicsCore.BORDER.height = max(size, 1f) // Minimum border size of 1x1
        CollisionSolver.updateBorderObjects()
        QuadTree.rebuildFlag = true // Rebuild to resize the QTNodes

        // Make sure the text field matches the actual border height
        if (size < 1f) {
            txtBorderHeight.text = "1.0"
        }
    }

    @FXML
    fun onBorderRestitutionChanged() {
        borderRestitution = sldBorderRestitution.value.toFloat()
        lblBorderRestitution.text = roundedToString(borderRestitution)
    }

    @FXML
    fun onBorderFrictionStaticChanged() {
        borderFrictionStatic = sldBorderFrictionStatic.value.toFloat()
        lblBorderFrictionStatic.text = roundedToString(borderFrictionStatic)
    }

    @FXML
    fun onBorderFrictionDynamicChanged() {
        borderFrictionDynamic = sldBorderFrictionDynamic.value.toFloat()
        lblBorderFrictionDynamic.text = roundedToString(borderFrictionDynamic)
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
    fun onQuadTreeCapacityTyped() {
        val capacity: Int = txtQuadTreeCapacity.text.toIntOrNull() ?: 0
        if (capacity > 0) {
            QuadTree.capacity = capacity
            QuadTree.rebuildFlag = true
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

    /// region =====Initialization and Helper=====
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

        /// region =====Forces=====
        txtGravityDirectionX.text = gravityDirection.x.toString()
        txtGravityDirectionY.text = gravityDirection.y.toString()
        txtGravityPointX.text = gravityPoint.x.toString()
        txtGravityPointY.text = gravityPoint.y.toString()
        txtGravityPointStrength.text = gravityPointStrength.toString()
        updateGravityFieldsAvailability()
        setSliderAndLabel(sldDamping, lblDamping, damping, 4)

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
     * Rounds a float [value] to two decimal places and converts it to a string.
     */
    private fun roundedToString(value: Float, decimalPlaces: Int = 2): String {
        return String.format(Locale.US, "%.${decimalPlaces}f", value)
    }

    /**
     * Sets the [value] of a [slider] and its corresponding [label].
     */
    private fun setSliderAndLabel(slider: Slider, label: Label, value: Float, lblDecimalPlaces: Int = 2) {
        slider.value = value.toDouble()
        label.text = roundedToString(value, lblDecimalPlaces)
    }

    /**
     * Restricts the input of a [text field][textField] to numeric values.
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
     * Parses the text of a [text field][textField] to a float.
     * If the text cannot be parsed, the [default value][default] is returned.
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

        /// region =====Forces=====
        var gravityType: GravityType = GravityType.DIRECTIONAL
            private set
        val gravityDirection: Vector2 = Vector2(0.0f, -0.0f)
        val gravityPoint: Vector2 = Vector2( // Default: The center of the world
            (FhysicsCore.BORDER.width / 2.0).toFloat(),
            (FhysicsCore.BORDER.height / 2.0).toFloat()
        )
        var gravityPointStrength: Float = 100.0f
            private set
        var damping: Float = 0.000f
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

        fun setBorderProperties(restitution: Float, frictionStatic: Float, frictionDynamic: Float) {
            borderRestitution = restitution
            borderFrictionStatic = frictionStatic
            borderFrictionDynamic = frictionDynamic
        }
        /// endregion

        /// region =====QuadTree=====
        var drawQuadTree: Boolean = true
            private set
        var drawQTNodeUtilization: Boolean = true
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
        var drawObjectCount: Boolean = true
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
