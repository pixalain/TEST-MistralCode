package com.f1live.app.data.repository

import kotlinx.serialization.Serializable

@Serializable
data class RegulationPoint(val text: String)

@Serializable
data class RegulationSection(val title: String, val points: List<String> = emptyList())

@Serializable
data class RegulationCategory(val title: String, val sections: List<RegulationSection> = emptyList())

@Serializable
data class Regulations(val categories: List<RegulationCategory> = emptyList())
