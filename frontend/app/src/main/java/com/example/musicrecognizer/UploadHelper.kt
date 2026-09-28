package com.example.musicrecognizer

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

suspend fun uploadFlacToBackend(flacFile: File): IdentifyResponse {
    val mime = "audio/flac".toMediaType()
    val body = flacFile.asRequestBody(mime)
    val part = MultipartBody.Part.createFormData("file", flacFile.name, body)

    return try {
        ApiClient.api.identify(part)
    } catch (e: Exception) {
        IdentifyResponse(error = "Upload failed: ${e.message}")
    }
}