package com.f1live.app.data.api

import com.f1live.app.data.model.AlphaEnvelope
import com.f1live.app.data.model.AlphaResultRow
import com.f1live.app.data.model.AlphaResultsIndex
import com.f1live.app.data.model.AlphaRound
import com.f1live.app.data.model.AlphaSessionResults
import com.f1live.app.data.model.ConstructorStandingsData
import com.f1live.app.data.model.DriverStandingsData
import com.f1live.app.data.model.DriversData
import com.f1live.app.data.model.Envelope
import com.f1live.app.data.model.RacesData
import com.f1live.app.data.model.SprintData
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface JolpicaService {

    @GET("ergast/f1/current/driverStandings.json")
    suspend fun driverStandings(@Query("limit") limit: Int = 100): Envelope<DriverStandingsData>

    @GET("ergast/f1/{season}/driverStandings.json")
    suspend fun seasonDriverStandings(
        @Path("season") season: String,
        @Query("limit") limit: Int = 100
    ): Envelope<DriverStandingsData>

    @GET("ergast/f1/current/constructorStandings.json")
    suspend fun constructorStandings(@Query("limit") limit: Int = 100): Envelope<ConstructorStandingsData>

    @GET("ergast/f1/current.json")
    suspend fun seasonSchedule(@Query("limit") limit: Int = 100): Envelope<RacesData>

    @GET("ergast/f1/current/{round}/results.json")
    suspend fun raceResults(
        @Path("round") round: String,
        @Query("limit") limit: Int = 100
    ): Envelope<RacesData>

    @GET("ergast/f1/current/{round}/sprint.json")
    suspend fun sprintResults(
        @Path("round") round: String,
        @Query("limit") limit: Int = 100
    ): Envelope<SprintData>

    @GET("ergast/f1/current/{round}/qualifying.json")
    suspend fun qualifyingResults(
        @Path("round") round: String,
        @Query("limit") limit: Int = 100
    ): Envelope<RacesData>

    @GET("ergast/f1/drivers/{driverId}.json")
    suspend fun driverInfo(@Path("driverId") driverId: String): Envelope<DriversData>

    @GET("ergast/f1/drivers/{driverId}/results.json")
    suspend fun driverResults(
        @Path("driverId") driverId: String,
        @Query("limit") limit: Int = 100,
        @Query("offset") offset: Int = 0
    ): Envelope<RacesData>

    @GET("ergast/f1/drivers/{driverId}/qualifying.json")
    suspend fun driverQualifying(
        @Path("driverId") driverId: String,
        @Query("limit") limit: Int = 100,
        @Query("offset") offset: Int = 0
    ): Envelope<RacesData>
}

interface AlphaService {

    @GET("f1/alpha/core/rounds/")
    suspend fun rounds(@Query("year") year: Int): AlphaEnvelope<List<AlphaRound>>

    @GET("f1/alpha/results/{roundId}/")
    suspend fun resultsIndex(@Path("roundId") roundId: String): AlphaEnvelope<AlphaResultsIndex>

    @GET("f1/alpha/results/{roundId}/{code}/")
    suspend fun sessionResults(
        @Path("roundId") roundId: String,
        @Path("code") code: String
    ): AlphaEnvelope<AlphaSessionResults>
}

object ApiClient {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val okHttp = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val req = chain.request().newBuilder()
                .header("User-Agent", "F1LiveApp/1.0.0")
                .build()
            chain.proceed(req)
        }
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
        )
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.jolpi.ca/")
        .client(okHttp)
        .addConverterFactory(
            json.asConverterFactory("application/json".toMediaType())
        )
        .build()

    val service: JolpicaService = retrofit.create(JolpicaService::class.java)

    val alpha: AlphaService = retrofit.create(AlphaService::class.java)
}
