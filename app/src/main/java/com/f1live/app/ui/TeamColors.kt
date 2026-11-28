package com.f1live.app.ui

import androidx.compose.ui.graphics.Color

fun teamColor(constructorId: String?): Color = when (constructorId?.lowercase()) {
    "red_bull", "redbull" -> Color(0xFF3671C6)
    "ferrari" -> Color(0xFFE8002D)
    "mercedes" -> Color(0xFF27F4D2)
    "mclaren" -> Color(0xFFFF8000)
    "aston_martin" -> Color(0xFF229971)
    "alpine" -> Color(0xFF0093CC)
    "williams" -> Color(0xFF64C4FF)
    "racing_bulls", "vcarb", "rb" -> Color(0xFF6692FF)
    "sauber", "kick_sauber", "stake" -> Color(0xFF52E252)
    "haas" -> Color(0xFFB6BABD)
    else -> Color(0xFFE10600)
}
