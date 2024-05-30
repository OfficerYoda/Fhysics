/**
 * Sample Skeleton for 'ui.fxml' Controller Class
 */

package de.officeryoda.fhysics.rendering

import de.officeryoda.fhysics.engine.FhysicsCore
import de.officeryoda.fhysics.engine.Vector2
import de.officeryoda.fhysics.objects.Circle
import de.officeryoda.fhysics.objects.FhysicsObject
import de.officeryoda.fhysics.objects.Rectangle
import javafx.fxml.FXML
import javafx.scene.control.*
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
    private lateinit var btnPause: ToggleButton

    @FXML
    private lateinit var btnStep: Button

    @FXML
    private lateinit var txtTimeSpeed: TextField

    /// =====Debug=====
    @FXML
    private lateinit var cbQuadTree: CheckBox

    @FXML
    private lateinit var cbQTNodeUtilization: CheckBox

    @FXML
    private lateinit var cbQTCapacity: CheckBox

    @FXML
    private lateinit var cbMSPU: CheckBox

    @FXML
    private lateinit var cbUPS: CheckBox

    @FXML
    private lateinit var cbObjectCount: CheckBox

    @FXML
    private lateinit var cbBoundingBoxes: CheckBox

    @FXML
    private lateinit var lblWallElasticity: Label

    @FXML
    private lateinit var sldWallElasticity: Slider

    /// =====Spawn Object=====
    @FXML
    fun onCircleClicked() {
        spawnObjectType = SpawnObjectType.CIRCLE
        updateSpawnPreview()
        setSpawnFieldAvailability(radius = true, width = false, height = false)
    }

    @FXML
    fun onRectangleClicked() {
        spawnObjectType = SpawnObjectType.RECTANGLE
        updateSpawnPreview()
        setSpawnFieldAvailability(radius = false, width = true, height = true)
    }

    @FXML
    fun onNothingClicked() {
        spawnObjectType = SpawnObjectType.NOTHING
        updateSpawnPreview()
        setSpawnFieldAvailability(radius = false, width = false, height = false)
    }

    private fun updateSpawnPreview() {
        if(spawnObjectType == SpawnObjectType.NOTHING) {
            RenderUtil.drawer.spawnPreview = null
            return
        }

        Rectangle(SceneListener.mouseWorldPos, spawnWidth, spawnHeight)
        val obj: FhysicsObject = when (spawnObjectType) {
            SpawnObjectType.CIRCLE -> Circle(SceneListener.mouseWorldPos, spawnRadius)
            SpawnObjectType.RECTANGLE -> Rectangle(SceneListener.mouseWorldPos, spawnWidth, spawnHeight)
            else -> throw IllegalArgumentException("Invalid spawn object type")
        }

        obj.color = Color(obj.color.red, obj.color.green, obj.color.blue, 128)
        RenderUtil.drawer.spawnPreview = obj
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
    fun onRadiusTyped() {
        spawnRadius = parseTextField(txtSpawnRadius)
        updateSpawnPreview()
    }

    @FXML
    fun onWidthTyped() {
        spawnWidth = parseTextField(txtSpawnWidth)
        updateSpawnPreview()
    }

    @FXML
    fun onHeightTyped() {
        spawnHeight = parseTextField(txtSpawnHeight)
        updateSpawnPreview()
    }

    /// =====Gravity=====
    @FXML
    fun onDirectionClicked() {
        gravityType = GravityType.DIRECTIONAL
        setGravityFieldsAvailability(direction = true, point = false)
    }

    @FXML
    fun onPointClicked() {
        gravityType = GravityType.TOWARDS_POINT
        setGravityFieldsAvailability(direction = false, point = true)
    }

    private fun setGravityFieldsAvailability(direction: Boolean, point: Boolean) {
        txtGravityDirectionX.isDisable = !direction
        txtGravityDirectionY.isDisable = !direction

        txtGravityPointX.isDisable = !point
        txtGravityPointY.isDisable = !point
        txtGravityPointStrength.isDisable = !point
    }

    @FXML
    fun onDirectionXTyped() {
        gravityDirection.x = parseTextField(txtGravityDirectionX)
    }

    @FXML
    fun onDirectionYTyped() {
        gravityDirection.y = parseTextField(txtGravityDirectionY)
    }

    @FXML
    fun onPointXTyped() {
        gravityPoint.x = parseTextField(txtGravityPointX)
    }

    @FXML
    fun onPointYTyped() {
        gravityPoint.y = parseTextField(txtGravityPointY)
    }

    @FXML
    fun onStrengthTyped() {
        gravityPointStrength = parseTextField(txtGravityPointStrength)
    }

    /// =====Time=====
    @FXML
    fun onPauseClicked() {
        FhysicsCore.running = !btnPause.isSelected
        btnStep.isDisable = FhysicsCore.running
    }

    @FXML
    fun onStepClicked() {
        FhysicsCore.update()
    }

    @FXML
    fun onTimeSpeedTyped() {
        timeSpeed = parseTextField(txtTimeSpeed)
        FhysicsCore.dt = 1.0F / FhysicsCore.UPDATES_PER_SECOND * timeSpeed
    }

    /// =====Debug=====
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
    fun onBoundingBoxesClicked() {
        drawBoundingBoxes = !drawBoundingBoxes
    }

    @FXML
    fun onWallElasticityChanged() {
        wallElasticity = sldWallElasticity.value.toFloat()
        lblWallElasticity.text = String.format(Locale.US, "%.2f", wallElasticity)
    }

    /// =====Initialization and helper=====
    @FXML // This method is called by the FXMLLoader when initialization is complete
    fun initialize() {
        when (spawnObjectType) {
            SpawnObjectType.CIRCLE -> onCircleClicked()
            SpawnObjectType.RECTANGLE -> onRectangleClicked()
            SpawnObjectType.NOTHING -> onNothingClicked()
        }

        restrictToNumericInput(txtSpawnRadius, false)
        restrictToNumericInput(txtSpawnWidth, false)
        restrictToNumericInput(txtSpawnHeight, false)

        restrictToNumericInput(txtGravityDirectionX)
        restrictToNumericInput(txtGravityDirectionY)
        restrictToNumericInput(txtGravityPointX)
        restrictToNumericInput(txtGravityPointY)
        restrictToNumericInput(txtGravityPointStrength)

        restrictToNumericInput(txtTimeSpeed, false)

        /// =====Spawn Object=====
        cbSpawnPreview.isSelected = drawSpawnPreview
        txtSpawnRadius.text = spawnRadius.toString()
        txtSpawnWidth.text = spawnWidth.toString()
        txtSpawnHeight.text = spawnHeight.toString()
        txtSpawnWidth.isDisable = true
        txtSpawnHeight.isDisable = true

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

        /// =====Time=====
        btnPause.isSelected = !FhysicsCore.running
        btnStep.isDisable = FhysicsCore.running
        txtTimeSpeed.text = timeSpeed.toString()

        /// =====Debug=====
        cbQuadTree.isSelected = drawQuadTree
        cbQTNodeUtilization.isSelected = drawQTNodeUtilization
        cbQTNodeUtilization.isDisable = !drawQuadTree
        cbQTNodeUtilization.isSelected = drawQTNodeUtilization

        cbObjectCount.isSelected = drawObjectCount
        cbMSPU.isSelected = drawMSPU
        cbUPS.isSelected = drawUPS
        cbBoundingBoxes.isSelected = drawBoundingBoxes

        sldWallElasticity.value = wallElasticity.toDouble()
        lblWallElasticity.text = String.format(Locale.US, "%.2f", wallElasticity)
    }

    private fun restrictToNumericInput(textField: TextField, allowNegatives: Boolean = true) {
        textField.textProperty().addListener { _, oldValue, newValue ->
            val regexPattern: String = if (allowNegatives) "-?\\d*\\.?\\d*" else "\\d*\\.?\\d*"
            if (!newValue.matches(regexPattern.toRegex())) {
                textField.text = oldValue
            }
        }
    }

    private fun parseTextField(textField: TextField): Float {
        return textField.text.toFloatOrNull() ?: 0.0F
    }

    companion object {
        /// =====Spawn Object=====
        var spawnObjectType: SpawnObjectType = SpawnObjectType.CIRCLE
            private set
        var drawSpawnPreview: Boolean = true
            private set
        var spawnRadius: Float = 1.0F
            private set
        var spawnWidth: Float = 1.0F
            private set
        var spawnHeight: Float = 1.0F
            private set

        /// =====Gravity=====
        var gravityType: GravityType = GravityType.DIRECTIONAL
            private set
        val gravityDirection: Vector2 = Vector2(0.0f, 0.0f)
        val gravityPoint: Vector2 = Vector2( // The center of the world
            (FhysicsCore.BORDER.width / 2.0).toFloat(),
            (FhysicsCore.BORDER.height / 2.0).toFloat()
        )
        var gravityPointStrength: Float = 100.0f
            private set

        /// =====Time=====
        var timeSpeed: Float = 1.0F
            private set

        /// =====Debug=====
        var drawQuadTree: Boolean = false
            private set
        var drawQTNodeUtilization: Boolean = true
            private set

        var drawQTCapacity: Boolean = false
            private set
        var drawMSPU: Boolean = true
            private set
        var drawUPS: Boolean = false
            private set
        var drawObjectCount: Boolean = false
            private set
        var drawBoundingBoxes: Boolean = false
            private set

        var wallElasticity: Float = 1.0F
            private set
    }
}

enum class SpawnObjectType {
    CIRCLE,
    RECTANGLE,
    NOTHING
}

enum class GravityType {
    DIRECTIONAL,
    TOWARDS_POINT
}