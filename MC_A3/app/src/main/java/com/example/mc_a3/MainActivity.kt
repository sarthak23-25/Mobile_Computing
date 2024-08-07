package com.example.mc_a3

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.mc_a3.ui.theme.MC_A3Theme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.io.IOException

class MainActivity : ComponentActivity(), SensorEventListener {

    private var sensorManager: SensorManager? = null
    private var isAccelerometerActiveState = mutableStateOf(false)
    private var startTimeMillis: Long = 0
    private val xValues = mutableListOf<Float>()
    private val yValues = mutableListOf<Float>()
    private val secarr = mutableListOf<Int>()
    private val zValues = mutableListOf<Float>()
    private lateinit var sensorDataDao: SensorDataDao
    private lateinit var sensorDatabase: SensorDatabase
    private var isRecording = false
    private var latestX: Float = 0f
    private var latestY: Float = 0f
    private var latestZ: Float = 0f
    private lateinit var navController: NavHostController
    private var lastCollectionTime = 0L
    private var seconds = 0

    private fun saveAllDataToCSV(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val allSensorData = sensorDataDao.getAllSensorData()
            saveToCSV(context, allSensorData)
        }
    }
    override fun onAccuracyChanged(s: Sensor?, i: Int) {}

    override fun onSensorChanged(event: SensorEvent?) {
        if (isAccelerometerActiveState.value && event!!.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            getAccelerometer(event)
            val elapsedTimeMillis = System.currentTimeMillis() - startTimeMillis
            if (elapsedTimeMillis >= 100000) {
                stopAccelerometer()
            }
        }
    }
    private fun stopAccelerometer() {
        isAccelerometerActiveState.value = false
        sensorManager?.unregisterListener(this)
        printValuesToLog()
    }

    private suspend fun saveToCSV(context: Context, sensorDataList: List<SensorData>) {
        withContext(Dispatchers.IO) {
            val fileName = "sensor_data.csv"
            val filePath = File(context.filesDir, fileName)

            try {
                FileWriter(filePath).use { writer ->
                    writer.append("Timestamp,X,Y,Z\n")
                    sensorDataList.forEach { sensorData ->
                        writer.append("${sensorData.timestamp},${sensorData.x},${sensorData.y},${sensorData.z}\n")
                    }
                    writer.flush()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "CSV file saved successfully", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: IOException) {
                // Log the error
                Log.e("SaveToCSV", "Error saving CSV file: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error saving CSV file", Toast.LENGTH_SHORT).show()
                }
                e.printStackTrace()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorDatabase = SensorDatabase.getDatabase(this)
        sensorDataDao = sensorDatabase.SensorDataDao()

        val currentTimestamp = System.currentTimeMillis()
        setContent {
            MC_A3Theme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainActivityContent(context = applicationContext,latestX, latestY, latestZ, seconds)
                }
            }
        }
    }

    @Composable
    fun MainActivityContent(context: Context, x: Float, y: Float, z: Float, seconds: Int) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                DisplayResult(
                    x = x,
                    y = y,
                    z = z,
                    timestamp = seconds,
                    isAccelerometerActive = isAccelerometerActiveState.value,
                    onToggleAccelerometerActive = { toggleAccelerometerActive() },
                    onClickSave = { saveData(seconds) },
                    onSaveAllDataToCSV = { saveAllDataToCSV(context) } // Pass saveAllDataToCSV here
                )
            }
        }
    }

    private fun saveData(timestamp: Int) {
        Toast.makeText(this, "Data Saved", Toast.LENGTH_SHORT).show()
        CoroutineScope(Dispatchers.IO).launch {
            for (i in xValues.indices) {
                val x = xValues[i]
                val y = yValues[i]
                val z = zValues[i]
                val sec = secarr[i]
                sensorDataDao.insertSensorData(SensorData(x = x, y = y, z = z, timestamp = sec))
            }
            // Clear lists after saving data
            xValues.clear()
            yValues.clear()
            zValues.clear()
        }
    }

    private fun getAccelerometer(event: SensorEvent) {
        val currentTime = System.currentTimeMillis()

        if (currentTime - lastCollectionTime >= 500L) {
            lastCollectionTime = currentTime

            val xVal = event.values[0]
            val yVal = event.values[1]
            val zVal = event.values[2]
            latestX = xVal
            latestY = yVal
            latestZ = zVal
           // val currentTimeMillis = System.currentTimeMillis()
            xValues.add(xVal)
            yValues.add(yVal)
            zValues.add(zVal)
            secarr.add(seconds)
            printValuesToLog()
            seconds = seconds + 1
            setContent {
                MainActivityContent(this,xVal, yVal, zVal, seconds) // Update MainActivityContent with the new accelerometer data
            }
        }

    }

    private fun toggleAccelerometerActive() {
        val currentState = isAccelerometerActiveState.value
        if (currentState) {
            stopAccelerometer()
        } else {
            startTimeMillis = System.currentTimeMillis()
            sensorManager?.registerListener(
                this,
                sensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL
            )
            isAccelerometerActiveState.value = true
        }
    }

    private fun printValuesToLog() {
        for (i in xValues.indices) {
            val x = xValues[i]
            val y = yValues[i]
            val z = zValues[i]
            val sec = secarr[i]
            Log.d("AccelerometerValues", "Second $i - X: $x, Y: $y, Z: $z")
        }
    }
    override fun onResume() {
        super.onResume()
        sensorManager!!.registerListener(
            this,
            sensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_NORMAL
        )
    }

    override fun onPause() {
        super.onPause()
        sensorManager!!.unregisterListener(this)
    }
}

@Composable
fun DisplayResult(
    x: Float,
    y: Float,
    z: Float,
    timestamp: Int,
    isAccelerometerActive: Boolean,
    onToggleAccelerometerActive: () -> Unit,
    onClickSave: () -> Unit,
    onSaveAllDataToCSV: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxSize()
    ) {
        Surface(
            color = Color.Transparent,
            border = BorderStroke(4.dp, Color.Black),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(horizontal = 16.dp) // Add padding to stabilize the width
                .width(200.dp) // Set a fixed width for the Surface
        ) {
            Text(
                text = "X-Axis = ${"%.4f".format(x)}\nY-Axis = ${"%.4f".format(y)}\nZ-Axis = ${"%.4f".format(z)}\n",
                modifier = Modifier.padding(16.dp),
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                maxLines = 3 // Ensure the text does not exceed 3 lines
            )
        }
        Spacer(modifier = Modifier.height(16.dp)) // Add space between text and buttons

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onToggleAccelerometerActive,
            colors = ButtonDefaults.buttonColors(
                contentColor = Color.White,
                containerColor = if (isAccelerometerActive) Color.Red else Color(0xFFFF8300)
            ),
            border = BorderStroke(2.dp, Color.DarkGray),
            shape = ButtonDefaults.elevatedShape,
            modifier = Modifier.size(180.dp, 40.dp)
        ) {
            Text(
                text = if (isAccelerometerActive) "Stop" else "Start",
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 10.sp
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            enabled = true,
            onClick = onClickSave,
            colors = ButtonDefaults.buttonColors(
                contentColor = Color.White,
                containerColor = Color(0xFF2E5A88)
            ),
            border = BorderStroke(2.dp, Color.DarkGray),
            shape = ButtonDefaults.elevatedShape,
            modifier = Modifier.size(180.dp, 40.dp) // Adjust size here
        ) {
            Text(
                text = "Save",
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 10.sp // Decrease font size to fit the text
            )
        }
        Spacer(modifier = Modifier.height(16.dp))// Add space between buttons
        Button(
            enabled = true,
            onClick = {
                val intent = Intent(context, GraphScreens::class.java)
                context.startActivity(intent)
            },
            colors = ButtonDefaults.buttonColors(
                contentColor = Color.White,
                containerColor = Color(0xFF007a00)
            ),
            border = BorderStroke(2.dp, Color.DarkGray),
            shape = ButtonDefaults.elevatedShape,
            modifier = Modifier.size(180.dp, 40.dp) // Adjust size here
        ) {
            Text(
                text = "Graph",
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 10.sp // Decrease font size to fit the text
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            enabled = true,
            onClick = onSaveAllDataToCSV,
            colors = ButtonDefaults.buttonColors(
                contentColor = Color.White,
                containerColor = Color.Cyan
            ),
            border = BorderStroke(2.dp, Color.DarkGray),
            shape = ButtonDefaults.elevatedShape,
            modifier = Modifier.size(180.dp, 40.dp) // Adjust size here
        ) {
            Text(
                text = "Save CSV",
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 10.sp // Decrease font size to fit the text
            )
        }
    }
}
