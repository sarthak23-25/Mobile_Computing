package com.example.weather_app

import retrofit2.http.GET
import retrofit2.http.Query

interface WeatherAPI {
    @GET("v1/archive")
    suspend fun getWeatherData(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("start_date") startDate: String,
        @Query("end_date") endDate: String,
        @Query("daily") daily: String = "temperature_2m_max,temperature_2m_min",
        @Query("timezone") timezone: String = "auto",
    ): WeatherResponse}
