package com.example.musicrecognizer

data class Track(
    val id: Int = 0,
    val title: String,
    val artist: String,
    val album: String,
    val imageUrl: String? = null,
    val spotifyUrl: String? = null,
    val confidence: Double,
    val timestamp: Long = System.currentTimeMillis()
)