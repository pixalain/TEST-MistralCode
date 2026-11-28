package com.f1live.app.data.repository

import com.f1live.app.data.model.Constructor
import com.f1live.app.data.model.DriverStanding
import kotlinx.serialization.Serializable

@Serializable
data class ConstructorProfile(
    val constructor: Constructor,
    val season: String,
    val points: String? = null,
    val position: String? = null,
    val wins: Int = 0,
    val podiums: Int = 0,
    val bestResult: String? = null,
    val titles: Int = 0,
    val drivers: List<DriverStanding> = emptyList(),
    val recentForm: List<Pair<String, Double>> = emptyList(), // round label -> points
    val logoUrl: String? = null,
    val logoPath: String? = null
)

// points per round for comparison chart
@Serializable
data class ConstructorRoundPoints(
    val constructorId: String,
    val name: String,
    val rounds: List<Pair<String, Double>> // "R3" -> points
)

// Static verified Wikimedia bitmap logos for 2026 teams (fallback: dynamic search)
val TEAM_LOGOS: Map<String, String> = mapOf(
    "red_bull" to "https://upload.wikimedia.org/wikipedia/commons/9/9f/Red_Bull_Racing_-_2005_Logo.png",
    "mercedes" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/4/42/Mercedes_AMG_Petronas_logo.png/500px-Mercedes_AMG_Petronas_logo.png",
    "ferrari" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/e/e4/Ferrari_F430_Scuderia_16M_logo.jpg/500px-Ferrari_F430_Scuderia_16M_logo.jpg",
    "mclaren" to "https://upload.wikimedia.org/wikipedia/commons/c/c1/Mclaren_logo.jpg",
    "aston_martin" to "https://upload.wikimedia.org/wikipedia/commons/0/03/Aston_Martin_F1_Team_logo_2024.jpg",
    "alpine" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/2/24/Logo_of_alpine_f1_team_2022.png/500px-Logo_of_alpine_f1_team_2022.png",
    "williams" to "https://upload.wikimedia.org/wikipedia/commons/e/e8/Williams_Racing_2020_logo.png",
    "sauber" to "https://thumb.wikimedia.org/wikipedia/commons/thumb/e/e4/Logo_of_Stake_F1_Team_Kick_Sauber.png/500px-Logo_of_Stake_F1_Team_Kick_Sauber.png",
    "haas" to "https://upload.wikimedia.org/wikipedia/commons/c/c7/Haas_F1_Team_logo.png"
)
