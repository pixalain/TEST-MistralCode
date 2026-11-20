package com.f1live.app.data.repository

import kotlinx.serialization.Serializable

@Serializable
data class AlphaSessionEntry(
    val position: Int,
    val driverCode: String,
    val driverName: String,
    val time: String? = null
)

@Serializable
data class WeekendSession(
    val code: String,          // FP1, FP2, FP3, Q, SR, R
    val title: String,         // "Practice 1"
    val timestamp: String?,    // UTC ISO
    val held: Boolean,         // session already took place
    val entries: List<AlphaSessionEntry> = emptyList() // top results if available
)

@Serializable
data class RaceWeekend(
    val season: String,
    val roundNumber: Int,
    val raceName: String,
    val sessions: List<WeekendSession> = emptyList()
)
