package com.example.musicrecognizer
import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

// Reprezintă structura răspunsului JSON primit de la backend.
data class IdentifyResponse(
    val status: String? = null,
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val image_url: String? = null,
    val spotify_url: String? = null,
    val confidence: Double? = null,
    val error: String? = null
)
// Interfață Retrofit care definește operațiile disponibile pe API.
// Folosită pentru încărcarea unui fișier audio către endpoint-ul /identify, folosind cerere multipart.
interface MusicApiService {
    @Multipart
    @POST("identify")
    suspend fun identify(
        @Part file: MultipartBody.Part
    ): IdentifyResponse
}