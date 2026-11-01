package com.example.data.remote

import com.example.data.model.OpenMeteoWeatherResponse
import com.example.data.model.UsgsFeatureCollection
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface UsgsApi {
    @GET("earthquakes/feed/v1.0/summary/all_day.geojson")
    suspend fun getRecentEarthquakes(): UsgsFeatureCollection
}

interface WeatherApi {
    @GET("v1/forecast")
    suspend fun getCurrentWeather(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") currentParams: String = "temperature_2m,relative_humidity_2m,surface_pressure,wind_speed_10m,weather_code,is_day,apparent_temperature,uv_index,rain,showers,cloud_cover",
        @Query("hourly") hourlyParams: String = "temperature_2m,precipitation_probability,weather_code",
        @Query("daily") dailyParams: String = "temperature_2m_max,temperature_2m_min,precipitation_sum,weather_code",
        @Query("timezone") timezone: String = "auto"
    ): OpenMeteoWeatherResponse
}

object RetrofitClient {
    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .build()

    val usgsApi: UsgsApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://earthquake.usgs.gov/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(UsgsApi::class.java)
    }

    val weatherApi: WeatherApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(WeatherApi::class.java)
    }
}
