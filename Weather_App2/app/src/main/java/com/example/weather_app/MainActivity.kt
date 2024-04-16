package com.example.weather_app

import WeatherViewModel
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
import androidx.compose.material3.TextField
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
import androidx.lifecycle.viewModelScope
import com.example.weather_app.ui.theme.Weather_AppTheme
import com.example.weathera2.WeatherData
import com.example.weathera2.WeatherDataDao
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
    private lateinit var dao: WeatherDataDao
    private lateinit var viewModel: WeatherViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dao = WeatherDatabase.getDatabase(applicationContext).weatherDataDao()
        viewModel = WeatherViewModel(weatherApiService, dao)

        setContent {
            Weather_AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = LightBlue
                ) {
                    WeatherMainContentScreen(weatherApiService, dao, viewModel)
                }
            }
        }
    }
}

private val coroutineScope = CoroutineScope(Dispatchers.Main)

private fun fetchWeatherData(
    date: String,
    latitude: Double,
    longitude: Double,
    weatherApiService: WeatherAPI,
    callback: (String, String) -> Unit
) {
    coroutineScope.launch {
        try {
            val response = withContext(Dispatchers.IO) {
                weatherApiService.getWeatherData(
                    latitude = latitude,
                    longitude = longitude,
                    startDate = date,
                    endDate = date
                )
            }
            val maxTemp = response.daily.temperature2mMax.firstOrNull()?.toString() ?: "N/A"
            val minTemp = response.daily.temperature2mMin.firstOrNull()?.toString() ?: "N/A"

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
                        latitude = latitude,
                        longitude = longitude,
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

            callback(if (maxTemp == "N/A") averageMaxTemp else maxTemp,
                if (minTemp == "N/A") averageMinTemp else minTemp)
        } catch (e: Exception) {
            Log.e("API_ERROR", "Error fetching weather data: ${e.message}")
            callback("N/A", "N/A")
        }
    }
}

private fun fetchHistoricalWeatherData(
    startDate: String,
    endDate: String,
    latitude: Double,
    longitude: Double,
    weatherApiService: WeatherAPI,
    dao: WeatherDataDao,
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
                        latitude = latitude,
                        longitude = longitude,
                        startDate = formattedDate,
                        endDate = formattedDate
                    )
                }

                val maxTemp = response.daily.temperature2mMax.firstOrNull() ?: Double.NaN
                val minTemp = response.daily.temperature2mMin.firstOrNull() ?: Double.NaN

                val weatherData = WeatherData(
                    location = "Berlin",
                    date = formattedDate,
                    temperatureMax = maxTemp.takeIf { !it.isNaN() } ?: 0.0, // Assign default value if maxTemp is NaN
                    temperatureMin = minTemp.takeIf { !it.isNaN() } ?: 0.0    // Assign default value if minTemp is NaN
                )
                weatherDataList.add(weatherData)

                calendar.add(Calendar.DATE, 1)
            }

            callback(weatherDataList)
        } catch (e: Exception) {
            Log.e("API_ERROR", "Error fetching historical weather data: ${e.message}")
            callback(emptyList())
        }
    }
}


private val LightBlue = Color(0xFFADD8E6)

@Composable
fun WeatherMainContentScreen(weatherApiService: WeatherAPI, dao: WeatherDataDao, viewModel: WeatherViewModel) {
    var selectedDate by remember { mutableStateOf(Calendar.getInstance()) }
    var maxTemperature by remember { mutableStateOf("") }
    var minTemperature by remember { mutableStateOf("") }
    var latitude by remember { mutableStateOf("") }
    var longitude by remember { mutableStateOf("") }

    var showDataDialog by remember { mutableStateOf(false) }
    var weatherDataList by remember { mutableStateOf<List<WeatherData>>(emptyList()) }
    var weatherDataListDialog by remember { mutableStateOf<List<WeatherData>>(emptyList()) }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Image(
            painter = painterResource(id = R.drawable.craiyon_003352_a_image_for_background_weather_app__day),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceAround
        ) {
            Column {
                TextField(
                    value = latitude,
                    onValueChange = { latitude = it },
                    label = { Text("Latitude") }
                )
                Spacer(modifier = Modifier.width(8.dp))
                TextField(
                    value = longitude,
                    onValueChange = { longitude = it },
                    label = { Text("Longitude") }
                )
            }

            DateBox(selectedDate) { date ->
                selectedDate = date
            }

            Button(
                onClick = {
                    val formattedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(selectedDate.time)
                    fetchWeatherData(formattedDate, latitude.toDouble(), longitude.toDouble(), weatherApiService) { maxTemp, minTemp ->
                        maxTemperature = maxTemp
                        minTemperature = minTemp
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Submit")
            }

            Button(
                onClick = {
                    val currentDate = Calendar.getInstance() // Get current date
                    val endDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(currentDate.time)

                    val startDateCalendar = Calendar.getInstance()
                    startDateCalendar.add(Calendar.YEAR, -10) // Subtract 10 years from current date
                    val startDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(startDateCalendar.time)

                    fetchHistoricalWeatherData(startDate, endDate, latitude.toDouble(), longitude.toDouble(), weatherApiService, dao) { data ->
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

            Button(
                onClick = {
                    viewModel.fetchWeatherDataAndStore(latitude.toDouble(), longitude.toDouble())
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Fetch and Store Data")
            }

            Button(
                onClick = {
                    // Fetch all weather data from the Room database
                    viewModel.viewModelScope.launch {
                        val weatherDataList = dao.getAllWeatherData()
                        // Log the retrieved weather data
                        weatherDataList.forEach { weatherData ->
                            Log.d("WeatherData", "Location: , Date: ${weatherData.date}, Max Temp: ${weatherData.temperatureMax}, Min Temp: ${weatherData.temperatureMin}")
                        }
                        // Set the state to show the data dialog
                        showDataDialog = true
                        weatherDataListDialog = weatherDataList
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Show Stored Data")
            }

            var weatherDataListDialog by remember { mutableStateOf<List<WeatherData>>(emptyList()) }

            if (showDataDialog) {
                WeatherDataDownloadDialog(weatherDataListDialog) {
                    showDataDialog = false
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TemperatureDisplayBox("Max: $maxTemperature", backgroundColor = Color.Blue)
                Spacer(modifier = Modifier.width(8.dp))
                TemperatureDisplayBox("Min: $minTemperature", backgroundColor = Color.Blue)
            }
        }
        if (showDataDialog) {
            WeatherDataDownloadDialog(weatherDataList) {
                showDataDialog = false
            }
        }
    }
}

@Composable
fun WeatherDataDownloadDialog(weatherDataList: List<WeatherData>, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose) {
        AlertDialog(
            onDismissRequest = onClose,
            title = {
                Text(text = "Weather Data Downloaded")
            },
            text = {
                Column {
                    weatherDataList.forEach { weatherData ->
                        Text(
                            text = "Location: , Date: ${weatherData.date}, Max Temp: ${weatherData.temperatureMax}, Min Temp: ${weatherData.temperatureMin}"
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
            onDateSelected(calendar.value)
        }
    }.show()
}

@Composable
fun TemperatureDisplayBox(temperature: String, backgroundColor: Color) {
    Surface(
        modifier = Modifier
            .width(120.dp)
            .height(60.dp),
        color = backgroundColor,
        shape = RoundedCornerShape(8.dp),
        shadowElevation = 4.dp
    ) {
        Box(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxSize()
        ) {
            Text(
                text = temperature,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}
