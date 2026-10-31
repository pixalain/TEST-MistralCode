package com.f1live.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Envelope<T>(val MRData: T)

@Serializable
data class DriverStandingsData(
    @SerialName("StandingsTable") val standingsTable: StandingsTable
)

@Serializable
data class ConstructorStandingsData(
    @SerialName("StandingsTable") val standingsTable: StandingsTable
)

@Serializable
data class StandingsTable(
    val season: String,
    val round: String? = null,
    val StandingsLists: List<StandingsList> = emptyList()
)

@Serializable
data class StandingsList(
    val season: String? = null,
    val round: String? = null,
    val DriverStandings: List<DriverStanding>? = null,
    val ConstructorStandings: List<ConstructorStanding>? = null
)

@Serializable
data class DriverStanding(
    val position: String,
    val positionText: String? = null,
    val points: String,
    val wins: String? = null,
    val Driver: Driver,
    val Constructors: List<Constructor> = emptyList()
)

@Serializable
data class ConstructorStanding(
    val position: String,
    val positionText: String? = null,
    val points: String,
    val wins: String? = null,
    val Constructor: Constructor
)

@Serializable
data class Driver(
    val driverId: String,
    val permanentNumber: String? = null,
    val code: String? = null,
    val givenName: String,
    val familyName: String,
    val dateOfBirth: String? = null,
    val nationality: String? = null
)

@Serializable
data class Constructor(
    val constructorId: String,
    val name: String,
    val nationality: String? = null
)

@Serializable
data class RacesData(
    @SerialName("RaceTable") val raceTable: RaceTable
)

@Serializable
data class RaceTable(
    val season: String,
    val Races: List<Race> = emptyList()
)

@Serializable
data class Race(
    val season: String,
    val round: String,
    val raceName: String,
    val Circuit: Circuit,
    val date: String,
    val time: String? = null,
    @SerialName("FirstPractice") val firstPractice: Session? = null,
    @SerialName("SecondPractice") val secondPractice: Session? = null,
    @SerialName("ThirdPractice") val thirdPractice: Session? = null,
    val Qualifying: Session? = null,
    val Sprint: Session? = null,
    val Results: List<Result>? = null,
    val QualifyingResults: List<QualifyingResult>? = null
) {
    val locality: String get() = Circuit.Location.locality
    val country: String get() = Circuit.Location.country
    val circuitName: String get() = Circuit.circuitName
}

@Serializable
data class SprintData(
    @SerialName("RaceTable") val raceTable: SprintRaceTable
)

@Serializable
data class SprintRaceTable(
    val season: String,
    val Races: List<SprintRace> = emptyList()
)

@Serializable
data class SprintRace(
    val season: String,
    val round: String,
    val raceName: String,
    val Circuit: Circuit,
    val date: String,
    val time: String? = null,
    val Sprint: List<Result>? = null
)

@Serializable
data class QualifyingResult(
    val number: String? = null,
    val position: String,
    val Driver: Driver,
    val Constructor: Constructor,
    val Q1: String? = null,
    val Q2: String? = null,
    val Q3: String? = null
) {
    val bestTime: String?
        get() = listOfNotNull(Q3, Q2, Q1).firstOrNull()
}

@Serializable
data class Result(
    val number: String? = null,
    val position: String? = null,
    val positionText: String? = null,
    val points: String? = null,
    val Driver: Driver,
    val Constructor: Constructor,
    val grid: String? = null,
    val laps: String? = null,
    val status: String? = null,
    val Time: RaceTime? = null,
    val FastestLap: FastestLap? = null
) {
    val displayPosition: String
        get() = positionText ?: position ?: "-"
}

@Serializable
data class RaceTime(
    val millis: String? = null,
    val time: String? = null
)

@Serializable
data class FastestLap(
    val rank: String? = null,
    val lap: String? = null,
    val Time: RaceTime? = null
)

@Serializable
data class Circuit(
    val circuitId: String,
    val circuitName: String,
    val Location: Location
)

@Serializable
data class Location(
    val locality: String,
    val country: String
)

@Serializable
data class Session(
    val date: String,
    val time: String? = null
)
