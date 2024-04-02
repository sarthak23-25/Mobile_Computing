package com.example.weather_app

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.weather_app.ui.theme.Weather_AppTheme
import com.example.weathera2.WeatherData
import com.example.weathera2.WeatherDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
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
        val startDate = "2014-03-16" // Adjust start date
        val endDate = "2024-03-30" // Adjust end date

        fetchHistoricalWeatherData(startDate, endDate, weatherApiService) { weatherDataList ->
            // Store the fetched data in the database
            storeWeatherDataInDatabase(weatherDataList)

            printWeatherDataFromDatabase()
        }
    }

    private fun storeWeatherDataInDatabase(weatherDataList: List<WeatherData>) {
        coroutineScope.launch {
            val dao = WeatherDatabase.getDatabase(applicationContext).weatherDataDao()
            dao.insertWeatherData(weatherDataList)
        }
    }

    private fun printWeatherDataFromDatabase() {
        println("---------------------------------------------------------------------")
        coroutineScope.launch {
            val dao = WeatherDatabase.getDatabase(applicationContext).weatherDataDao()
            val weatherDataList = dao.getAllWeatherData()
            weatherDataList.forEach { weatherData ->
                Log.d("WeatherData", "Location: ${weatherData.location}, Date: ${weatherData.date}, Max Temp: ${weatherData.temperatureMax}, Min Temp: ${weatherData.temperatureMin}")
            }
        }
        println("---------------------------------------------------------------------")
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

            // If N/A is encountered, calculate the average of the last 3 days' temperatures
            val formattedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(date)
            val lastThreeDays = Calendar.getInstance()
            lastThreeDays.time = formattedDate
            var sumMax = 0.0
            var sumMin = 0.0
            var validCount = 0

            for (i in 0 until 3) {
                lastThreeDays.add(Calendar.DATE, -1)
                val prevDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(lastThreeDays.time)
                val prevResponse = withContext(Dispatchers.IO) {
                    weatherApiService.getWeatherData(
                        latitude = 52.52,
                        longitude = 13.419998,
                        startDate = prevDate,
                        endDate = prevDate
                    )
                }
                prevResponse.daily.temperature2mMax.firstOrNull()?.let {
                    sumMax += it
                    validCount++
                }
                prevResponse.daily.temperature2mMin.firstOrNull()?.let {
                    sumMin += it
                    validCount++
                }
            }

            val averageMaxTemp = if (validCount > 0) String.format("%.1f", sumMax / validCount) else "N/A"
            val averageMinTemp = if (validCount > 0) String.format("%.1f", sumMin / validCount) else "N/A"

            // Invoke the callback with the extracted temperatures or averages
            callback(if (maxTemp == "N/A") averageMaxTemp else maxTemp,
                if (minTemp == "N/A") averageMinTemp else minTemp)
        } catch (e: Exception) {
            // Handle any exceptions that occur during the API call
            Log.e("API_ERROR", "Error fetching weather data: ${e.message}")
            callback("N/A", "N/A")
        }
    }
}

private fun fetchHistoricalWeatherData(
    startDate: String,
    endDate: String,
    weatherApiService: WeatherAPI,
    callback: (List<WeatherData>) -> Unit
) {
    coroutineScope.launch {
        try {
            val weatherDataList = mutableListOf<WeatherData>()

            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val calendar = Calendar.getInstance()
            calendar.time = sdf.parse(startDate) ?: Date()

            while (calendar.time.before(sdf.parse(endDate) ?: Date())) {
                val formattedDate = sdf.format(calendar.time)
                val response = withContext(Dispatchers.IO) {
                    weatherApiService.getWeatherData(
                        latitude = 52.52,
                        longitude = 13.419998,
                        startDate = formattedDate,
                        endDate = formattedDate
                    )
                }

                val maxTemp = response.daily.temperature2mMax.firstOrNull() ?: Double.NaN
                val minTemp = response.daily.temperature2mMin.firstOrNull() ?: Double.NaN

                weatherDataList.add(
                    WeatherData(
                        location = "Berlin", // or any other location
                        date = formattedDate,
                        temperatureMax = maxTemp,
                        temperatureMin = minTemp
                    )
                )

                calendar.add(Calendar.DATE, 1) // Move to the next date
            }

            callback(weatherDataList)
        } catch (e: Exception) {
            Log.e("API_ERROR", "Error fetching historical weather data: ${e.message}")
            callback(emptyList())
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

    var showDataDialog by remember { mutableStateOf(false) }
    var weatherDataList by remember { mutableStateOf<List<WeatherData>>(emptyList()) }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Background Image
        Image(
            painter = painterResource(id = R.drawable.craiyon_003352_a_image_for_background_weather_app__day),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )

        // Content Layout
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceAround
        ) {
            // Date Box
            DateBox(selectedDate) { date ->
                selectedDate = date
            }

            // Submit Button
            Button(
                onClick = {
                    val formattedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate.time)
                    fetchWeatherData(formattedDate, weatherApiService) { maxTemp, minTemp ->
                        maxTemperature = maxTemp
                        minTemperature = minTemp
                        Log.d("WeatherData", "Max Temperature: $maxTemp, Min Temperature: $minTemp")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Submit")
            }

            // Fetch Data Button
            Button(
                onClick = {
                    val startDate = "2014-03-16" // Adjust start date
                    val endDate = "2024-03-30" // Adjust end date

                    fetchHistoricalWeatherData(startDate, endDate, weatherApiService) { data ->
                        weatherDataList = data
                        showDataDialog = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Fetch Data")
            }

            // Temperature Display Boxes
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TemperatureDisplayBox("Max: $maxTemperature", backgroundColor = Color.Blue)
                Spacer(modifier = Modifier.width(8.dp))
                TemperatureDisplayBox("Min: $minTemperature", backgroundColor = Color.Blue)
            }

            // Data Screen Button
            Button(
                onClick = {
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Data Screen")
            }
        }
        if (showDataDialog) {
            WeatherDataDialog(weatherDataList) {
                showDataDialog = false
            }
        }
    }
}

@Composable
fun WeatherDataDialog(weatherDataList: List<WeatherData>, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose) {
        AlertDialog(
            onDismissRequest = onClose,
            title = {
                Text(text = "Weather Data")
            },
            text = {
                Column {
                    weatherDataList.forEach { weatherData ->
                        Text(
                            text = "Location: ${weatherData.location}, Date: ${weatherData.date}, Max Temp: ${weatherData.temperatureMax}, Min Temp: ${weatherData.temperatureMin}"
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = onClose
                ) {
                    Text("Close")
                }
            }
        )
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
