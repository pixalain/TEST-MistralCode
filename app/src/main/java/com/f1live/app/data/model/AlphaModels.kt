package com.f1live.app.data.model

import kotlinx.serialization.Serializable

@Serializable
data class AlphaEnvelope<T>(
    val metadata: AlphaMetadata? = null,
    val data: T? = null
)

@Serializable
data class AlphaMetadata(
    val timestamp: String? = null,
    val count: Int? = null
)

@Serializable
data class AlphaRound(
    val id: String,
    val number: Int,
    val name: String? = null,
    val is_cancelled: Boolean? = null,
    val sessions: List<AlphaSession>? = null
)

@Serializable
data class AlphaSession(
    val id: String? = null,
    val number: Int? = null,
    val type: String? = null,
    val type_display: String? = null,
    val is_cancelled: Boolean? = null,
    val timestamp: String? = null,
    val local_timestamp: String? = null,
    val timezone: String? = null
)

@Serializable
data class AlphaResultsIndex(
    val year: Int? = null,
    val round_number: Int? = null,
    val round_name: String? = null,
    val available_results: List<AlphaAvailableResult> = emptyList()
)

@Serializable
data class AlphaAvailableResult(
    val code: String,
    val title: String? = null,
    val url: String? = null
)

@Serializable
data class AlphaSessionResults(
    val code: String? = null,
    val title: String? = null,
    val timestamp: String? = null,
    val local_timestamp: String? = null,
    val timezone: String? = null,
    val results: List<AlphaResultRow> = emptyList()
)

@Serializable
data class AlphaResultRow(
    val driver: AlphaDriver,
    val team: AlphaTeam? = null,
    val position: Int? = null,
    val position_text: String? = null,
    val time: String? = null,
    val status: String? = null,
    val laps: Int? = null,
    val car_number: Int? = null,
    val components: Map<String, AlphaComponent>? = null
) {
    val displayPosition: String get() = position_text ?: position?.toString() ?: "-"
    val gridPosition: Int? get() = components?.get("GRID")?.position
    val fastestLap: String? get() = components?.get("FLAP")?.time
    val fastestLapRank: Int? get() = components?.get("FLAP")?.position
}

@Serializable
data class AlphaDriver(
    val id: String? = null,
    val abbreviation: String? = null,
    val given_name: String = "",
    val family_name: String = "",
    val nationality: String? = null,
    val permanent_car_number: Int? = null
) {
    val fullName: String get() = "$given_name $family_name".trim()
}

@Serializable
data class AlphaTeam(
    val id: String? = null,
    val name: String = "",
    val primary_color: String? = null
)

@Serializable
data class AlphaComponent(
    val key: String? = null,
    val name: String? = null,
    val position: Int? = null,
    val time: String? = null
)
