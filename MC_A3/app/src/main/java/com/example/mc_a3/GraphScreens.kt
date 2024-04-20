package com.example.mc_a3

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import com.example.mc_a3.ui.theme.MC_A3Theme
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.Utils

class GraphScreens : ComponentActivity() {
    private lateinit var sensorDataListState: MutableState<List<SensorData>>
    private val isLoading = mutableStateOf(true)

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize MPAndroidChart library
        Utils.init(applicationContext)

        setContent {
            MC_A3Theme {
                // Initialize the database
                val database = SensorDatabase.getDatabase(applicationContext)

                // Initialize a mutable state for sensor data
                sensorDataListState = remember { mutableStateOf(emptyList()) }

                // Launch a coroutine to retrieve sensor data
                LaunchedEffect(true) {
                    val sensorDataDao = database.SensorDataDao()
                    val sensorDataList = sensorDataDao.getAllSensorData()
                    sensorDataListState.value = sensorDataList
                    isLoading.value = false
                }

                if (!isLoading.value) {
                    // Plot the graphs using the retrieved sensor data
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        LineChartExample(sensorDataListState.value)
                    }
                }
            }
        }
    }
}

@Composable
fun LineChartExample(dataPoints: List<SensorData>) {
    Column(modifier = Modifier.fillMaxSize()) {
        val modifier = Modifier
            .fillMaxWidth()
            .weight(1f)

        Box(
            modifier = modifier.background(color = Color.White.copy(alpha = 0.2f)),
            contentAlignment = Alignment.BottomCenter
        ) {
            AndroidView(
                modifier = Modifier
                    .height(IntrinsicSize.Min)
                    .align(Alignment.BottomCenter)
                    .aspectRatio(1f),
                factory = { context ->
                    LineChart(context).apply {
                        setupLineChart(this, dataPoints.map { it.timestamp.toFloat() }, dataPoints.map { it.x }, Color.Red)
                    }
                }
            )
        }

        Box(
            modifier = modifier.background(color = Color.White.copy(alpha = 0.2f)),
            contentAlignment = Alignment.BottomCenter
        ) {
            AndroidView(
                modifier = Modifier
                    .height(IntrinsicSize.Min)
                    .align(Alignment.BottomCenter)
                    .aspectRatio(1f),
                factory = { context ->
                    LineChart(context).apply {
                        setupLineChart(this, dataPoints.map { it.timestamp.toFloat() }, dataPoints.map { it.y }, Color.Blue)
                    }
                }
            )
        }

        Box(
            modifier = modifier.background(color = Color.White.copy(alpha = 0.2f)),
            contentAlignment = Alignment.BottomCenter
        ) {
            AndroidView(
                modifier = Modifier
                    .height(IntrinsicSize.Min)
                    .align(Alignment.BottomCenter)
                    .aspectRatio(1f),
                factory = { context ->
                    LineChart(context).apply {
                        setupLineChart(this, dataPoints.map { it.timestamp.toFloat() }, dataPoints.map { it.z }, Color.Green)
                    }
                }
            )
        }
    }
}

private fun setupLineChart(lineChart: LineChart, xValues: List<Float>, yValues: List<Float>, lineColor: Color) {
    with(lineChart) {
        setNoDataText("No data available")
        setTouchEnabled(true)
        isDragEnabled = true
        setScaleEnabled(true)
        setPinchZoom(true)
        legend.textColor = Color.White.toArgb()
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.setDrawGridLines(false)
        xAxis.valueFormatter = IndexAxisValueFormatter(xValues.map { it.toString() })
        xAxis.granularity = 1f
        xAxis.textColor = Color.White.toArgb()
        xAxis.axisLineWidth = 2f
        xAxis.gridLineWidth = 1.5f
        xAxis.axisLineColor = Color.White.toArgb()
        xAxis.gridColor = Color.White.toArgb()
        legend.textColor = Color.White.toArgb()
        legend.textSize = 14f
        axisLeft.textColor = Color.White.toArgb()
        axisLeft.axisLineWidth = 2f
        axisLeft.gridLineWidth = 1.5f
        axisLeft.axisLineColor = Color.White.toArgb()
        axisLeft.gridColor = Color.White.toArgb()
        description.text = "Time (seconds)"
        description.textColor = Color.White.toArgb()
        axisRight.isEnabled = false

        val leftAxis: YAxis = axisLeft
        leftAxis.setDrawGridLines(true)

        val lineDataSet = LineDataSet(yValues.mapIndexed { index, y -> Entry(xValues[index], y) }, "Values").apply {
            color = lineColor.toArgb()
            setCircleColor(lineColor.toArgb())
            lineWidth = 2f
            circleRadius = 3f
            setDrawCircleHole(false)
            valueTextSize = 9f
            valueTextColor = Color.White.toArgb()
            setDrawFilled(true)
            fillColor = lineColor.toArgb()
            setDrawValues(true)
            setValueTextColor(Color.White.toArgb())
        }

        val data = LineData(lineDataSet)
        setData(data)

        yValues.forEachIndexed { index, value ->
            Log.d("LineChartExample", "Index: $index, Time: ${xValues[index]}, Value: $value")
        }
    }
}

//////////////////////////////////
//@Composable
//fun LineChartExample(dataPoints: List<SensorData>) {
//    Box(modifier = Modifier.fillMaxWidth().background(color = Color.White.copy(alpha = 0.2f)),
//        contentAlignment = Alignment.BottomCenter
//    ) {
//        AndroidView(
//            modifier = Modifier
//                .height(IntrinsicSize.Min)
//                .align(Alignment.BottomCenter)
//                .aspectRatio(1f),
//            factory = { context ->
//                LineChart(context).apply {
//                    setNoDataText("No data available")
//                    setTouchEnabled(true)
//                    isDragEnabled = true
//                    setScaleEnabled(true)
//                    setPinchZoom(true)
//                    legend.textColor = Color.White.toArgb()
//                    xAxis.position = XAxis.XAxisPosition.BOTTOM
//                    xAxis.setDrawGridLines(false)
//                    xAxis.valueFormatter =
//                        IndexAxisValueFormatter(dataPoints.map { it.timestamp.toString() })
//                    print(xAxis.valueFormatter)
//                    xAxis.granularity = 1f
//                    xAxis.apply {
//                        // Set text color for X-axis labels
//                        textColor = Color.White.toArgb()
//                        axisLineWidth = 2f
//                        // Set grid line width
//                        gridLineWidth = 1.5f
//                        axisLineColor = Color.White.toArgb()
//                        gridColor = Color.White.toArgb()
//                    }
//                    legend.apply {
//                        // Set legend text color to white
//                        textColor = Color.White.toArgb()
//                        // Increase legend text size
//                        textSize = 14f // Adjust the size as needed
//                    }
//                    axisLeft.apply {
//                        // Set text color for Y-axis labels
//                        textColor = Color.White.toArgb()
//                        axisLineWidth = 2f
//                        // Set grid line width
//                        gridLineWidth = 1.5f
//                        axisLineColor = Color.White.toArgb()
//                        gridColor = Color.White.toArgb()
//                    }
//                    // Set the x-axis description label
//                    description.text = "Time (seconds)"
//                    description.textColor = Color.White.toArgb()
//
//
//                    axisRight.isEnabled = false
//
//                    val leftAxis: YAxis = axisLeft
//                    leftAxis.setDrawGridLines(true)
//
//                    // Find the minimum and maximum values for x and y axes
//                    val xMin = dataPoints.minByOrNull { it.timestamp }?.timestamp ?: 0f
//                    val xMax = dataPoints.maxByOrNull { it.timestamp }?.timestamp ?: 0f
//                    val yMin = dataPoints.minOf { it.x }
//                    val yMax = dataPoints.maxOf { it.x }
//
//                    // Set minimum and maximum values for the axes with some padding
//                    axisLeft.axisMinimum = yMin - 1f
//                    axisLeft.axisMaximum = yMax + 1f
//                    xAxis.axisMinimum = 0f
//                    xAxis.axisMaximum = xMax.toFloat() + 1f
//
//                    val xValues = ArrayList<Entry>()
//
//                    dataPoints.forEach { dataPoint ->
//                        xValues.add(Entry(dataPoint.timestamp.toFloat(), dataPoint.x))
//                    }
//
//                    val set1 = LineDataSet(xValues, "X Values").apply {
//                        color = ColorTemplate.getHoloBlue()
//                        setCircleColor(ColorTemplate.getHoloBlue())
//                        lineWidth = 2f
//                        circleRadius = 3f
//                        setDrawCircleHole(false)
//                        valueTextSize = 9f
//                        valueTextColor = Color.White.toArgb()
//                        setDrawFilled(true)
//                        fillColor = ColorTemplate.getHoloBlue()
//                        // Set the color of the legend text to white
//                        setDrawValues(true)
//                        setValueTextColor(Color.White.toArgb())
//                    }
//
//                    var data = LineData(set1)
//                    setData(data)
//                    dataPoints.forEachIndexed { index, recordedEntry ->
//                        Log.d(
//                            "RecordedEntry",
//                            "Index: $index, Time: ${recordedEntry.timestamp}, X Angle: ${recordedEntry.x}"
//                        )
//                    }
//                }
//            }
//        )
//    }
//}
//////////////////////////////////
//@RequiresApi(Build.VERSION_CODES.O)
//@Composable
//fun LineChartExample(sensorDataList: List<SensorData>) {
//    AndroidView(
//        modifier = Modifier.fillMaxSize(),
//        factory = { context ->
//            LineChart(context).apply {
//                data = prepareLineData(sensorDataList)
//                description = Description().apply {
//                    text = ""
//                }
//                setNoDataText("No data available")
//                setNoDataTextColor(android.graphics.Color.BLACK)
//                invalidate()
//            }
//        }
//    )
//}

@RequiresApi(Build.VERSION_CODES.O)
private fun prepareLineData(sensorDataList: List<SensorData>): LineData {
    val entriesX = mutableListOf<Entry>()
    val entriesY = mutableListOf<Entry>()
    val entriesZ = mutableListOf<Entry>()

    sensorDataList.forEachIndexed { index, sensorData ->
        entriesX.add(Entry(sensorData.timestamp.toFloat(), sensorData.x))
        entriesY.add(Entry(sensorData.timestamp.toFloat(), sensorData.y))
        entriesZ.add(Entry(sensorData.timestamp.toFloat(), sensorData.z))
    }

    val lineDataSetX = LineDataSet(entriesX, "X Values").apply {
        color = android.graphics.Color.RED
        setDrawCircles(false)
        setDrawValues(false)
    }
    val lineDataSetY = LineDataSet(entriesY, "Y Values").apply {
        color = android.graphics.Color.BLUE
        setDrawCircles(false)
        setDrawValues(false)
    }
    val lineDataSetZ = LineDataSet(entriesZ, "Z Values").apply {
        color = android.graphics.Color.GREEN
        setDrawCircles(false)
        setDrawValues(false)
    }

    return LineData(lineDataSetX, lineDataSetY, lineDataSetZ)
}

//@RequiresApi(Build.VERSION_CODES.O)
//@Preview(showBackground = true)
//@Composable
//fun PlotGraphsPreview() {
//    MC_A3Theme {
//        // Mock sensor data list for preview
//        val sensorDataList = listOf(
//            SensorData(1, 0.5f, 0.7f, 0.3f, 0), // Example data
//            SensorData(2, 0.6f, 0.8f, 0.4f, 1620506233888),
//            SensorData(3, 0.7f, 0.9f, 0.5f, 1620506234899)
//            // Add more mock data as needed
//        )
//        LineChartExample(sensorDataList)
//    }
//}
