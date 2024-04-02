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

@Dao
interface WeatherDataDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeatherData(weatherData: List<WeatherData>)

    @Query("SELECT * FROM weather_data")
    suspend fun getAllWeatherData(): List<WeatherData>

    @Query("SELECT * FROM weather_data WHERE location = :location AND date LIKE :dateSuffix ORDER BY id DESC LIMIT 10")
    suspend fun getWeatherDataByLocationAndDateSuffix(location: String, dateSuffix: String): List<WeatherData>

    @Query("DELETE FROM weather_data WHERE location = :location")
    suspend fun deleteByLocation(location: String)

    @Query("SELECT EXISTS(SELECT 1 FROM weather_data WHERE location = :location AND date = :date LIMIT 1)")
    suspend fun doesWeatherDataExist(location: String, date: String): Boolean
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

