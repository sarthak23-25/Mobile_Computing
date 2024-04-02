package com.example.weather_app

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.weather_app.R.drawable.craiyon_003352_a_image_for_background_weather_app__day
import com.example.weather_app.ui.theme.Weather_AppTheme
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://archive-api.open-meteo.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val weatherApiService = retrofit.create(WeatherAPI::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Weather_AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = LightBlue // Changed background color to light blue
                ) {
                    WeatherContent(weatherApiService)
                }
            }
        }
    }
}

private val coroutineScope = CoroutineScope(Dispatchers.Main)

private fun fetchWeatherData(
    date: String,
    weatherApiService: WeatherAPI,
    callback: (String, String) -> Unit
) {
    coroutineScope.launch { // Launch a coroutine scope
        try {
            Log.d("API_CALL", "Fetching weather data for date: $date")

            // Call the getWeatherData function within the coroutine scope
            val response = withContext(Dispatchers.IO) {
                weatherApiService.getWeatherData(
                    latitude = 52.52,
                    longitude = 13.419998,
                    startDate = date,
                    endDate = date
                )
            }
            Log.d("API_RESPONSE", "Response received: $response")

            // Parse the response and extract max and min temperatures
            val maxTemp = response.daily.temperature2mMax.firstOrNull()?.toString() ?: "N/A"
            val minTemp = response.daily.temperature2mMin.firstOrNull()?.toString() ?: "N/A"

            // Invoke the callback with the extracted temperatures
            callback(maxTemp, minTemp)
        } catch (e: Exception) {
            // Handle any exceptions that occur during the API call
            Log.e("API_ERROR", "Error fetching weather data: ${e.message}")
            callback("N/A", "N/A")
        }
    }
}


// Custom colors
private val LightBlue = Color(0xFFADD8E6)
private val DarkBlue = Color(0xFF00008B)

@Composable
fun WeatherContent(weatherApiService: WeatherAPI) {
    var selectedDate by remember { mutableStateOf(Calendar.getInstance()) }
    var maxTemperature by remember { mutableStateOf("") }
    var minTemperature by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val formattedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate.time)
        try {
            val response = weatherApiService.getWeatherData(52.52, 13.419998, "2024-03-21", "2024-03-21")
            println(response)
            val firstTemperature2mMax = response.daily.temperature2mMax.firstOrNull()
            val firstTemperature2mMin = response.daily.temperature2mMin.firstOrNull()
            maxTemperature = firstTemperature2mMax.toString()
            minTemperature = firstTemperature2mMin.toString()
        } catch (e: Exception) {
            Log.e("API_ERROR", "Error fetching weather data: ${e.message}")
            maxTemperature = "N/A"
            minTemperature = "N/A"
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Background Image
        Image(
            painter = painterResource(id = craiyon_003352_a_image_for_background_weather_app__day),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )

        // Content Layout
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(horizontal = 8.dp), // Add horizontal padding
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceAround // Add space between items
        ) {
            // Date Box
            var selectedDate by remember { mutableStateOf(Calendar.getInstance()) }
            DateBox(selectedDate) { date ->
                selectedDate = selectedDate
            }

            // Submit Button
            Button(
                onClick = {
                    val formattedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate.time)
                    fetchWeatherData(formattedDate, weatherApiService) { maxTemp, minTemp ->
                        maxTemperature = maxTemp
                        minTemperature = minTemp
                        println("Max Temperature: $maxTemp, Min Temperature: $minTemp") // Print fetched weather data
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Red,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Submit")
            }

            // Temperature Display Boxes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TemperatureDisplayBox("Max: $maxTemperature", backgroundColor = DarkBlue)
                Spacer(modifier = Modifier.width(8.dp))
                TemperatureDisplayBox("Min: $minTemperature", backgroundColor = DarkBlue)
            }
        }
    }
}

@Composable
fun DateBox(selectedDate: Calendar, onDateSelected: (Calendar) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Surface(
        modifier = Modifier
            .wrapContentWidth()
            .clickable { showDialog = true },
        color = Color.White,
        shape = RoundedCornerShape(8.dp),
        shadowElevation = 4.dp
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(8.dp)
        ) {
            Text(
                text = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate.time),
                color = Color.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(8.dp)
            )
        }
    }

    if (showDialog) {
        DateDialog(selectedDate = selectedDate) { date ->
            onDateSelected(date)
            showDialog = false
        }
    }
}

@Composable
fun DateDialog(selectedDate: Calendar, onDateSelected: (Calendar) -> Unit) {
    val context = LocalContext.current
    val calendar = remember { mutableStateOf(selectedDate) }

    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            calendar.value.set(year, month, dayOfMonth)
        },
        calendar.value.get(Calendar.YEAR),
        calendar.value.get(Calendar.MONTH),
        calendar.value.get(Calendar.DAY_OF_MONTH)
    ).apply {
        setOnDismissListener {
            val formattedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.value.time)
            onDateSelected(calendar.value) // Update the selectedDate variable
        }
    }.show()
}

@Composable
fun TemperatureDisplayBox(temperature: String, backgroundColor: Color) {
    Surface(
        modifier = Modifier
            .width(120.dp)
            .height(60.dp),
        color = backgroundColor, // Use the provided background color
        shape = RoundedCornerShape(8.dp), // Set rounded corners
        shadowElevation = 4.dp // Adjust elevation as needed
    ) {
        Box(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxSize()
        ) {
            Text(
                text = temperature,
                color = Color.White, // Set text color to white
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}
