package com.f1live.app.data.repository

import com.f1live.app.data.model.ConstructorStandingsData
import com.f1live.app.data.model.DriverStandingsData
import com.f1live.app.data.model.DriversData
import com.f1live.app.data.model.RacesData
import com.f1live.app.data.model.SeasonsData
import com.f1live.app.data.model.SprintData
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject

object F1Json {
    val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    inline fun <reified T> mrData(obj: JsonObject): T {
        val mr = obj["MRData"]?.jsonObject
            ?: throw IllegalStateException("Réponse invalide: pas de MRData")
        return json.decodeFromString<T>(mr.toString())
    }

    fun total(obj: JsonObject): String? =
        (obj["MRData"]?.jsonObject?.get("total") as? JsonPrimitive)?.content

    fun driverStandings(obj: JsonObject): DriverStandingsData = mrData(obj)
    fun constructorStandings(obj: JsonObject): ConstructorStandingsData = mrData(obj)
    fun races(obj: JsonObject): RacesData = mrData(obj)
    fun sprint(obj: JsonObject): SprintData = mrData(obj)
    fun drivers(obj: JsonObject): DriversData = mrData(obj)
    fun seasons(obj: JsonObject): SeasonsData = mrData(obj)
}
