package com.example.weathera2

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

@Entity(tableName = "weather_data")
data class WeatherData(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val location: String,
    val date: String,
    val temperatureMax: Double,
    val temperatureMin: Double
)

data class AverageWeatherData(
    val avgMax: Double,
    val avgMin: Double
)
@Dao
interface WeatherDataDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeatherData(weatherData: WeatherData)

    @Query("SELECT * FROM weather_data")
    suspend fun getAllWeatherData(): List<WeatherData>

    @Query("SELECT AVG(temperatureMax) as avgMax, AVG(temperatureMin) as avgMin FROM weather_data WHERE date = :date")
    suspend fun avgWeatherData(date: String): AverageWeatherData

}

@Database(entities = [WeatherData::class], version = 1)
abstract class WeatherDatabase : RoomDatabase() {
    abstract fun weatherDataDao(): WeatherDataDao

    companion object {
        @Volatile
        private var INSTANCE: WeatherDatabase? = null

        fun getDatabase(context: Context): WeatherDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WeatherDatabase::class.java,
                    "weather_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
