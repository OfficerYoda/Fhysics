package de.officeryoda.fhysics.visual

import de.officeryoda.fhysics.engine.*
import de.officeryoda.fhysics.engine.collision.CollisionSolver
import de.officeryoda.fhysics.engine.datastructures.QuadTree
import de.officeryoda.fhysics.engine.objects.FhysicsObject
import de.officeryoda.fhysics.visual.SceneListener.polyVertices
import de.officeryoda.fhysics.visual.SceneListener.selectedObject
import de.officeryoda.fhysics.visual.SceneListener.spawnPreview
import de.officeryoda.fhysics.visual.SceneListener.updateSpawnPreview
import javafx.fxml.FXML
import javafx.scene.control.*
import javafx.scene.layout.AnchorPane
import javafx.scene.paint.Color
import java.util.*
import kotlin.math.max

/**
 * Handles the user interface of the Fhysics engine.
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

    /// region =====Fields: Scene=====
    @FXML
    private lateinit var btnSceneLoad: Button

    @FXML
    private lateinit var boxSceneLoadName: ComboBox<String>
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
    private lateinit var cbSubSteps: CheckBox

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
        Settings.spawnObjectType = SpawnObjectType.NOTHING
        updateSpawnPreview()
        updateSpawnFieldsAvailability()
    }

    @FXML
    fun onSpawnCircleClicked() {
        Settings.spawnObjectType = SpawnObjectType.CIRCLE
        updateSpawnPreview()
        updateSpawnFieldsAvailability()
    }

    @FXML
    fun onSpawnRectangleClicked() {
        Settings.spawnObjectType = SpawnObjectType.RECTANGLE
        updateSpawnPreview()
        updateSpawnFieldsAvailability()
    }

    @FXML
    fun onSpawnPolygonClicked() {
        Settings.spawnObjectType = SpawnObjectType.POLYGON
        updateSpawnPreview()
        updateSpawnFieldsAvailability()
        // Clear the polygon vertices list for a new polygon
        polyVertices.clear()
    }

    private fun updateSpawnFieldsAvailability() {
        txtSpawnRadius.isDisable = Settings.spawnObjectType != SpawnObjectType.CIRCLE
        txtSpawnWidth.isDisable = Settings.spawnObjectType != SpawnObjectType.RECTANGLE
        txtSpawnHeight.isDisable = Settings.spawnObjectType != SpawnObjectType.RECTANGLE
    }

    @FXML
    fun onSpawnStaticClicked() {
        Settings.spawnStatic = cbSpawnStatic.isSelected
    }

    @FXML
    fun onSpawnRadiusTyped() {
        Settings.spawnRadius = parseTextField(txtSpawnRadius)
        updateSpawnPreview()
    }

    @FXML
    fun onSpawnWidthTyped() {
        Settings.spawnWidth = parseTextField(txtSpawnWidth)
        updateSpawnPreview()
    }

    @FXML
    fun onSpawnHeightTyped() {
        Settings.spawnHeight = parseTextField(txtSpawnHeight)
        updateSpawnPreview()
    }

    @FXML
    fun onCustomColorClicked() {
        Settings.useCustomColor = cbCustomColor.isSelected
        clrSpawnColor.isDisable = !cbCustomColor.isSelected

        if (cbCustomColor.isSelected) {
            spawnPreview?.color = Settings.spawnColor
        }
    }

    @FXML
    fun onSpawnColorAction() {
        Settings.spawnColor = RenderUtil.paintToColor(clrSpawnColor.value)
        spawnPreview?.color = Settings.spawnColor
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
     * Expands the UI pane for object properties.
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
        SceneManager.clearScene()
    }

    @FXML
    fun onSceneLoadClicked() {
        val sceneName: String? = boxSceneLoadName.value

        SceneManager.loadScene(sceneName)
    }
    /// endregion

    /// region =====Methods: Forces=====
    @FXML
    fun onGravityDirectionClicked() {
        Settings.gravityType = GravityType.DIRECTIONAL
        updateGravityFieldsAvailability()
    }

    @FXML
    fun onGravityPointClicked() {
        Settings.gravityType = GravityType.TOWARDS_POINT
        updateGravityFieldsAvailability()
    }

    @FXML
    fun onGravityDirectionXTyped() {
        Settings.gravityDirection.x = parseTextField(txtGravityDirectionX)
    }

    @FXML
    fun onGravityDirectionYTyped() {
        Settings.gravityDirection.y = parseTextField(txtGravityDirectionY)
    }

    @FXML
    fun onGravityPointXTyped() {
        Settings.gravityPoint.x = parseTextField(txtGravityPointX)
    }

    @FXML
    fun onGravityPointYTyped() {
        Settings.gravityPoint.y = parseTextField(txtGravityPointY)
    }

    @FXML
    fun onGravityStrengthTyped() {
        Settings.gravityPointStrength = parseTextField(txtGravityPointStrength)
    }

    private fun updateGravityFieldsAvailability() {
        txtGravityDirectionX.isDisable = Settings.gravityType != GravityType.DIRECTIONAL
        txtGravityDirectionY.isDisable = Settings.gravityType != GravityType.DIRECTIONAL

        txtGravityPointX.isDisable = Settings.gravityType != GravityType.TOWARDS_POINT
        txtGravityPointY.isDisable = Settings.gravityType != GravityType.TOWARDS_POINT
        txtGravityPointStrength.isDisable = Settings.gravityType != GravityType.TOWARDS_POINT
    }

    @FXML
    fun onDampingChanged() {
        Settings.damping = sldDamping.value.toFloat()
        lblDamping.text = roundedToString(Settings.damping, 4)
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
        Settings.timeScale = parseTextField(txtTimeSpeed)
        FhysicsCore.dt = 1.0f / (FhysicsCore.UPDATES_PER_SECOND * FhysicsCore.SUB_STEPS) * Settings.timeScale
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
        var size: Float = max(parseTextField(textField, 1f), 1f) // Minimum border size of 1x1
        FhysicsCore.BORDER.width = size
        FhysicsCore.BORDER.height = size
        CollisionSolver.updateBorderObjects()
        QuadTree.rebuildFlag = true // Rebuild to resize the QTNodes

        // Make sure the text field matches the actual border height
        if (size < 1f) {
            txtBorderHeight.text = "1.0"
        }
    }

    @FXML
    fun onBorderRestitutionChanged() {
        Settings.borderRestitution = sldBorderRestitution.value.toFloat()
        lblBorderRestitution.text = roundedToString(Settings.borderRestitution)
    }

    @FXML
    fun onBorderFrictionStaticChanged() {
        Settings.borderFrictionStatic = sldBorderFrictionStatic.value.toFloat()
        lblBorderFrictionStatic.text = roundedToString(Settings.borderFrictionStatic)
    }

    @FXML
    fun onBorderFrictionDynamicChanged() {
        Settings.borderFrictionDynamic = sldBorderFrictionDynamic.value.toFloat()
        lblBorderFrictionDynamic.text = roundedToString(Settings.borderFrictionDynamic)
    }
    /// endregion

    /// region =====Methods: QuadTree=====
    @FXML
    fun onQuadTreeClicked() {
        Settings.drawQuadTree = cbQuadTree.isSelected

        // Node utilization is only drawn if the quad tree is drawn
        cbQTNodeUtilization.isDisable = !Settings.drawQuadTree
    }

    @FXML
    fun onQTNodeUtilizationClicked() {
        Settings.drawQTNodeUtilization = cbQTNodeUtilization.isSelected
    }

    @FXML
    fun onQuadTreeCapacityTyped() {
        val capacity: Int = txtQuadTreeCapacity.text.toIntOrNull() ?: 2
        QuadTree.capacity = capacity.toInt()
        QuadTree.rebuildFlag = true // Rebuild to resize the QTNodes

        // Make sure the text field matches the actual QuadTree capacity
        if (capacity < 2) {
            txtBorderHeight.text = "2.0"
        }
    }
    /// endregion

    /// region =====Methods: Debug=====
    @FXML
    fun onBoundingBoxesClicked() {
        Settings.showBoundingBoxes = !Settings.showBoundingBoxes
    }

    @FXML
    fun onSubPolygonsClicked() {
        Settings.showSubPolygons = cbSubPolygons.isSelected
    }

    @FXML
    fun onQTCapacityClicked() {
        Settings.showQTCapacity = cbQTCapacity.isSelected
    }

    @FXML
    fun onMSPUClicked() {
        Settings.showMSPU = cbMSPU.isSelected
    }

    @FXML
    fun onUPSClicked() {
        Settings.showUPS = cbUPS.isSelected
    }

    @FXML
    fun onSubStepsClicked() {
        Settings.showSubSteps = cbSubSteps.isSelected
    }

    @FXML
    fun onObjectCountClicked() {
        Settings.showObjectCount = cbObjectCount.isSelected
    }

    @FXML
    fun onRenderTimeClicked() {
        Settings.showRenderTime = cbRenderTime.isSelected
    }
    /// endregion
    /// endregion

    /// region =====Initialization and Helper=====
    @FXML // This method is called by the FXMLLoader when initialization is complete
    fun initialize() {
        instance = this
        renderer = RenderUtil.render

        updateUi()
        restrictUI()
    }

    /**
     * Updates the UI to match the current settings.
     *
     * This method should only be called from the JavaFX application thread.
     */
    private fun updateUi() {
        /// region =====Scene=====
        boxSceneLoadName.items.setAll(SceneManager.scenes.map { it.name })
        boxSceneLoadName.promptText = boxSceneLoadName.value ?: "Select a scene"
        /// endregion

        /// region =====Spawn Object=====
        cbSpawnStatic.isSelected = Settings.spawnStatic
        txtSpawnRadius.text = Settings.spawnRadius.toString()
        txtSpawnWidth.text = Settings.spawnWidth.toString()
        txtSpawnHeight.text = Settings.spawnHeight.toString()
        txtSpawnWidth.isDisable = true
        txtSpawnHeight.isDisable = true
        cbCustomColor.isSelected = Settings.useCustomColor
        clrSpawnColor.value = RenderUtil.colorToPaint(Settings.spawnColor) as Color

        when (Settings.spawnObjectType) {
            SpawnObjectType.NOTHING -> onSpawnNothingClicked()
            SpawnObjectType.CIRCLE -> onSpawnCircleClicked()
            SpawnObjectType.RECTANGLE -> onSpawnRectangleClicked()
            SpawnObjectType.POLYGON -> onSpawnPolygonClicked()
        }
        /// endregion

        /// region =====Forces=====
        txtGravityDirectionX.text = Settings.gravityDirection.x.toString()
        txtGravityDirectionY.text = Settings.gravityDirection.y.toString()
        txtGravityPointX.text = Settings.gravityPoint.x.toString()
        txtGravityPointY.text = Settings.gravityPoint.y.toString()
        txtGravityPointStrength.text = Settings.gravityPointStrength.toString()
        updateGravityFieldsAvailability()
        setSliderAndLabel(sldDamping, lblDamping, Settings.damping, 4)
        /// endregion

        /// region =====Miscellaneous=====
        btnTimePause.isSelected = !FhysicsCore.running
        btnTimeStep.isDisable = FhysicsCore.running
        txtTimeSpeed.text = Settings.timeScale.toString()
        txtBorderWidth.text = FhysicsCore.BORDER.width.toString()
        txtBorderHeight.text = FhysicsCore.BORDER.height.toString()
        setSliderAndLabel(sldBorderRestitution, lblBorderRestitution, Settings.borderRestitution)
        setSliderAndLabel(sldBorderFrictionStatic, lblBorderFrictionStatic, Settings.borderFrictionStatic)
        setSliderAndLabel(sldBorderFrictionDynamic, lblBorderFrictionDynamic, Settings.borderFrictionDynamic)
        /// endregion

        /// region =====QuadTree=====
        cbQuadTree.isSelected = Settings.drawQuadTree
        cbQTNodeUtilization.isDisable = !Settings.drawQuadTree
        cbQTNodeUtilization.isSelected = Settings.drawQTNodeUtilization
        txtQuadTreeCapacity.text = QuadTree.capacity.toString()
        /// endregion

        /// region =====Debug=====
        cbBoundingBoxes.isSelected = Settings.showBoundingBoxes
        cbSubPolygons.isSelected = Settings.showSubPolygons
        cbObjectCount.isSelected = Settings.showObjectCount
        cbMSPU.isSelected = Settings.showMSPU
        cbUPS.isSelected = Settings.showUPS
        /// endregion

        updateUiFlag = false
    }

    /**
     * Restricts the input of some UI elements to numeric values.
     */
    private fun restrictUI() {
        /// region =====Object Properties=====
        restrictToNumericInput(txtPropertyMass, false)
        restrictToNumericInput(txtPropertyRotation)
        /// endregion

        /// region =====Spawn Object=====
        restrictToNumericInput(txtSpawnRadius, false)
        restrictToNumericInput(txtSpawnWidth, false)
        restrictToNumericInput(txtSpawnHeight, false)
        /// endregion

        /// region =====Forces=====
        restrictToNumericInput(txtGravityDirectionX)
        restrictToNumericInput(txtGravityDirectionY)
        restrictToNumericInput(txtGravityPointX)
        restrictToNumericInput(txtGravityPointY)
        restrictToNumericInput(txtGravityPointStrength)
        /// endregion

        /// region =====Miscellaneous=====
        restrictToNumericInput(txtTimeSpeed, false)
        restrictToNumericInput(txtBorderWidth, false)
        restrictToNumericInput(txtBorderHeight, false)
        /// endregion

        /// region =====QuadTree=====
        restrictToNumericInput(txtQuadTreeCapacity, false)
        /// endregion
    }

    /**
     * Rounds a float [value] to the specified [decimalPlaces] (default: 2) and converts it to a string.
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

    companion object {
        lateinit var instance: UIController
        lateinit var renderer: Renderer

        /** Whether the UI should be updated */
        var updateUiFlag: Boolean = false

        private const val DEGREES_TO_RADIANS: Float = 0.017453292f
        private const val RADIANS_TO_DEGREES: Float = 57.29578f

        /**
         * Updates the UI to match the current settings.
         *
         * This method should only be called from the JavaFX application thread.
         */
        fun updateUi() {
            if (updateUiFlag) {
                instance.updateUi()
            }
        }

        /**
         * Expands the UI pane for object properties.
         */
        fun expandObjectPropertiesPane() {
            instance.expandObjectPropertiesPane()
        }
    }
}
