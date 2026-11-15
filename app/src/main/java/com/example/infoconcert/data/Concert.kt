package com.example.infoconcert.data

data class Concert(
    val name: String,
    val city: String,
    val date: String,
    val time: String?,
    val venue: String,
    val price: String?,
    val bookingUrl: String?,
    val infoUrl: String
)
