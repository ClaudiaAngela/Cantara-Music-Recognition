package com.example.musicrecognizer
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
object ApiClient {
    // Adresa serverului backend.
    private const val BASE_URL = "http://10.84.124.35:8000/"
    private val httpClient: OkHttpClient.Builder = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)      // timeout pentru conectare
        .readTimeout(600, TimeUnit.SECONDS)         // timeout pentru citirea răspunsului (2 min)
        .writeTimeout(60, TimeUnit.SECONDS)         // timeout pentru trimiterea request-ului
        .callTimeout(660, TimeUnit.SECONDS)         // timeout total pentru apel (3 min)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
    // Inițializarea obiectului Retrofit.
    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(httpClient.build())
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    // Interfața API generată automat de Retrofit.
    // Prin acest obiect se realizează apelurile către backend.
    val api: MusicApiService = retrofit.create(MusicApiService::class.java)
}