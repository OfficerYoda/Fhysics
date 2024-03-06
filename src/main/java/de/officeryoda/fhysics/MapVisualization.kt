package de.officeryoda.fhysics

import de.officeryoda.fhysics.engine.QuadTree
import javafx.animation.AnimationTimer
import org.jfree.chart.ChartFactory
import org.jfree.chart.ChartPanel
import org.jfree.chart.axis.NumberAxis
import org.jfree.data.category.CategoryDataset
import org.jfree.data.category.DefaultCategoryDataset
import java.awt.Dimension
import javax.swing.JFrame
import javax.swing.WindowConstants

class MapVisualization(private val qtCapacity: MutableMap<Int, Double>) : JFrame("Map Visualization") {

    init {
        initUI()
        object : AnimationTimer() {
            override fun handle(now: Long) {
                createChart()
            }
        }.start()
    }

    private fun initUI() {
        createChart()

        defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
        setLocationRelativeTo(null)
        isVisible = true
    }

    private fun createChart() {
        val dataset = createDataset()

        val chart = ChartFactory.createLineChart(
            "Current Capacity: ${QuadTree.capacity}",
            "Capacity",
            "Average MSPU",
            dataset
        )

        val yAxis = chart.categoryPlot.rangeAxis as NumberAxis
        if (qtCapacity.size > 1) {
            yAxis.setRange(qtCapacity.values.min(), qtCapacity.values.max())
        } else {
            yAxis.setRange(0.0, 10.0)
        }

        val chartPanel = ChartPanel(chart)
        chartPanel.preferredSize = Dimension(560, 390)
        contentPane = chartPanel

        pack()
    }

    private fun createDataset(): CategoryDataset {
        val dataset = DefaultCategoryDataset()

        for ((key, value) in qtCapacity.entries.sortedBy { it.key }) {
            dataset.addValue(value, "avg. MSPU", String.format("%02d", key))
        }

        return dataset
    }
}
