import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weather_app.WeatherAPI
import com.example.weathera2.WeatherData
import com.example.weathera2.WeatherDataDao
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class WeatherViewModel(
    private val weatherApiService: WeatherAPI,
    private val dao: WeatherDataDao // Inject the WeatherDataDao dependency
) : ViewModel() {

    fun fetchWeatherDataAndStore(latitude: Double, longitude: Double) {
        val currentDate = Calendar.getInstance()

        // Calculate endDate as currentDate - 2 days
        val endDate = Calendar.getInstance()
        endDate.add(Calendar.DATE, -2)
        val formattedEndDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(endDate.time)

        // Calculate startDate as currentDate - 10 years
        val startDate = Calendar.getInstance()
        startDate.add(Calendar.YEAR, -10)
        val formattedStartDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(startDate.time)

        viewModelScope.launch {
            try {
                // Loop through the dates between startDate and endDate
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val calendar = Calendar.getInstance()
                calendar.time = sdf.parse(formattedStartDate) ?: currentDate.time

                while (calendar.time.before(sdf.parse(formattedEndDate) ?: currentDate.time)) {
                    val formattedDate = sdf.format(calendar.time)
                    val response = weatherApiService.getWeatherData(latitude, longitude, formattedDate, formattedDate)
                    println(response)
                    val maxTemp = response.daily.temperature2mMax.firstOrNull() ?: Double.NaN
                    val minTemp = response.daily.temperature2mMin.firstOrNull() ?: Double.NaN

                    if (!maxTemp.isNaN() && !minTemp.isNaN()) {
                        val weatherData = WeatherData(
                            location = "Berlin",
                            date = formattedDate,
                            temperatureMax = maxTemp,
                            temperatureMin = minTemp
                        )
                        // Store the fetched weather data row-wise into the Room database
                        dao.insertWeatherData(weatherData)
                    } else {
                        Log.e("API_ERROR", "MaxTemp and MinTemp are null or NaN for date: $formattedDate")
                    }

                    calendar.add(Calendar.DATE, 1) // Move to the next date
                }
            } catch (e: Exception) {
                Log.e("API_ERROR", "Error fetching and storing weather data: ${e.message}")
            }
        }
    }
}
