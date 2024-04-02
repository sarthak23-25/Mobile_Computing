import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weather_app.WeatherAPI
import kotlinx.coroutines.launch

class WeatherViewModel(private val weatherApiService: WeatherAPI) : ViewModel() {

    fun fetchWeatherData(date: String, callback: (String, String) -> Unit) {
        viewModelScope.launch {
            try {
                val response = weatherApiService.getWeatherData(52.52, 13.419998, date, date)
                val maxTemp = response.daily.temperature2mMax.firstOrNull()?.toString() ?: "N/A"
                val minTemp = response.daily.temperature2mMin.firstOrNull()?.toString() ?: "N/A"
                callback(maxTemp, minTemp)
            } catch (e: Exception) {
                callback("N/A", "N/A")
            }
        }
    }
}
