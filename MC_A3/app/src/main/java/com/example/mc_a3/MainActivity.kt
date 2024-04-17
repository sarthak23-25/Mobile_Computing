package com.example.mc_a3
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mc_a3.ui.theme.MC_A3Theme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity(), SensorEventListener {

    private var sensorManager: SensorManager? = null

    override fun onAccuracyChanged(s: Sensor?, i: Int) {}

    override fun onSensorChanged(event: SensorEvent?) {
        if (event!!.sensor.type == Sensor.TYPE_ACCELEROMETER) {
            getAccelerometer(event)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        setContent {
            MC_A3Theme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainActivityContent()
                }
            }
        }
    }

    @Composable
    fun MainActivityContent() {
        // Empty composable as the content is being displayed dynamically
    }

    private fun getAccelerometer(event: SensorEvent) {
        // Movement
        val xVal = event.values[0]
        val yVal = event.values[1]
        val zVal = event.values[2]
        setContent {
            DisplayResult(x = xVal, y = yVal, z = zVal)
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
fun DisplayResult(x: Float, y: Float, z: Float, modifier: Modifier = Modifier) {

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
    )
    {
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

        StartButtonWithToast(
            onClick = {
                // Start button clicked
            },
            modifier = Modifier.size(180.dp, 40.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            enabled = true,
            onClick = {},
            colors = ButtonDefaults.buttonColors(
                contentColor = Color.White,
                containerColor  = Color(0xFFFF8300)
            ),
            border = BorderStroke(2.dp, Color.DarkGray),
            shape = ButtonDefaults.elevatedShape,
            modifier = Modifier.size(180.dp, 40.dp) // Adjust size here
        )
        {
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
            onClick = {},
            colors = ButtonDefaults.buttonColors(
                contentColor = Color.White,
                containerColor  = Color(0xFF007a00)
            ),
            border = BorderStroke(2.dp, Color.DarkGray),
            shape = ButtonDefaults.elevatedShape,
            modifier = Modifier.size(180.dp, 40.dp) // Adjust size here
        )
        {
            Text(
                text = "Graph",
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 10.sp // Decrease font size to fit the text
            )
        }
    }
}

@Composable
fun StartButtonWithToast(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current // Retrieve the current context
    Button(
        enabled = true,
        onClick = {
            CoroutineScope(Dispatchers.Main).launch {
                delay(10000L) // Delay for 10 seconds
                Toast.makeText(
                    context,
                    "Done",
                    Toast.LENGTH_SHORT
                ).show() // Show toast message after 10 seconds
            }
        },
        colors = ButtonDefaults.buttonColors(
            contentColor = Color.White,
            containerColor  = Color(0xFF2E5A88)
        ),
        border = BorderStroke(2.dp, Color.DarkGray),
        shape = ButtonDefaults.elevatedShape,
        modifier = modifier // Adjust size here
    )
    {
        Text(
            text = "Start",
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 10.sp // Decrease font size to fit the text
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun GreetingPreview() {
    MC_A3Theme {
        DisplayResult(0f, 0f, 0f)
    }
}
