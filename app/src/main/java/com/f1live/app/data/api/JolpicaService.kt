package com.f1live.app.data.api

import com.f1live.app.data.model.AlphaEnvelope
import com.f1live.app.data.model.AlphaLapsData
import com.f1live.app.data.model.AlphaResultRow
import com.f1live.app.data.model.AlphaResultsIndex
import com.f1live.app.data.model.AlphaRound
import com.f1live.app.data.model.AlphaSessionResults
import com.f1live.app.data.model.ConstructorStandingsData
import com.f1live.app.data.model.DriverStandingsData
import com.f1live.app.data.model.DriversData
import com.f1live.app.data.model.RacesData
import com.f1live.app.data.model.SeasonsData
import com.f1live.app.data.model.SprintData
import kotlinx.serialization.json.JsonObject
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url
import java.util.concurrent.TimeUnit

interface JolpicaService {

    @GET("ergast/f1/current/driverStandings.json")
    suspend fun driverStandings(@Query("limit") limit: Int = 100): JsonObject

    @GET("ergast/f1/{season}/driverStandings.json")
    suspend fun seasonDriverStandings(
        @Path("season") season: String,
        @Query("limit") limit: Int = 100
    ): JsonObject

    @GET("ergast/f1/current/constructorStandings.json")
    suspend fun constructorStandings(@Query("limit") limit: Int = 100): JsonObject

    @GET("ergast/f1/{season}/constructorStandings.json")
    suspend fun seasonConstructorStandings(
        @Path("season") season: String,
        @Query("limit") limit: Int = 100
    ): JsonObject

    @GET("ergast/f1/current.json")
    suspend fun seasonSchedule(@Query("limit") limit: Int = 100): JsonObject

    @GET("ergast/f1/{season}.json")
    suspend fun seasonScheduleFor(
        @Path("season") season: String,
        @Query("limit") limit: Int = 100
    ): JsonObject

    @GET("ergast/f1/current/{round}/results.json")
    suspend fun raceResults(
        @Path("round") round: String,
        @Query("limit") limit: Int = 100
    ): JsonObject

    @GET("ergast/f1/{season}/{round}/results.json")
    suspend fun seasonRaceResults(
        @Path("season") season: String,
        @Path("round") round: String,
        @Query("limit") limit: Int = 100
    ): JsonObject

    @GET("ergast/f1/current/{round}/sprint.json")
    suspend fun sprintResults(
        @Path("round") round: String,
        @Query("limit") limit: Int = 100
    ): JsonObject

    @GET("ergast/f1/{season}/{round}/sprint.json")
    suspend fun seasonSprintResults(
        @Path("season") season: String,
        @Path("round") round: String,
        @Query("limit") limit: Int = 100
    ): JsonObject

    @GET("ergast/f1/current/{round}/qualifying.json")
    suspend fun qualifyingResults(
        @Path("round") round: String,
        @Query("limit") limit: Int = 100
    ): JsonObject

    @GET("ergast/f1/{season}/{round}/qualifying.json")
    suspend fun seasonQualifyingResults(
        @Path("season") season: String,
        @Path("round") round: String,
        @Query("limit") limit: Int = 100
    ): JsonObject

    @GET("ergast/f1/seasons.json")
    suspend fun seasons(@Query("limit") limit: Int = 100): JsonObject

    @GET("ergast/f1/drivers/{driverId}.json")
    suspend fun driverInfo(@Path("driverId") driverId: String): JsonObject

    @GET("ergast/f1/drivers/{driverId}/results.json")
    suspend fun driverResults(
        @Path("driverId") driverId: String,
        @Query("limit") limit: Int = 100,
        @Query("offset") offset: Int = 0
    ): JsonObject

    @GET("ergast/f1/drivers/{driverId}/qualifying.json")
    suspend fun driverQualifying(
        @Path("driverId") driverId: String,
        @Query("limit") limit: Int = 100,
        @Query("offset") offset: Int = 0
    ): JsonObject

    @GET("ergast/f1/drivers/{driverId}/seasons.json")
    suspend fun driverSeasons(
        @Path("driverId") driverId: String,
        @Query("limit") limit: Int = 100
    ): JsonObject

    @GET("ergast/f1/drivers/{driverId}/sprint.json")
    suspend fun driverSprint(
        @Path("driverId") driverId: String,
        @Query("limit") limit: Int = 100,
        @Query("offset") offset: Int = 0
    ): JsonObject
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

    @GET("f1/alpha/laps/{roundId}/R/")
    suspend fun raceLaps(@Path("roundId") roundId: String): AlphaEnvelope<AlphaLapsData>
}

interface WikipediaService {
    @GET
    suspend fun pageImages(@Url url: String): JsonObject
}

interface WikidataService {
    @GET
    suspend fun api(@Url url: String): JsonObject
}

object ApiClient {

    val plain: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build()
    }

    val wikipedia: WikipediaService by lazy {
        Retrofit.Builder()
            .baseUrl("https://en.wikipedia.org/")
            .client(
                OkHttpClient.Builder()
                    .addInterceptor { chain ->
                        chain.proceed(
                            chain.request().newBuilder()
                                .header("User-Agent", "F1LiveApp/1.0 (local dev; contact: none)")
                                .build()
                        )
                    }
                    .build()
            )
            .addConverterFactory(Json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(WikipediaService::class.java)
    }

    val wikidata: WikidataService by lazy {
        Retrofit.Builder()
            .baseUrl("https://www.wikidata.org/")
            .client(
                OkHttpClient.Builder()
                    .addInterceptor { chain ->
                        chain.proceed(
                            chain.request().newBuilder()
                                .header("User-Agent", "F1LiveApp/1.0 (local dev; contact: none)")
                                .build()
                        )
                    }
                    .build()
            )
            .addConverterFactory(Json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(WikidataService::class.java)
    }


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
        .apply {
            if (com.f1live.app.BuildConfig.DEBUG) {
                addInterceptor(
                    HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BASIC
                    }
                )
            }
        }
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
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
