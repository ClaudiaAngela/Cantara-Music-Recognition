package com.example.musicrecognizer

import android.content.Context
import java.io.File

// clasa care inregistreaza (microfon)
class AudioProcessor(private val context: Context) {
    fun getTempFile(): File {

        // Creăm un fișier în folderul privat "cache" al aplicației
        return File(context.cacheDir, "recording.flac")
    }
    }

