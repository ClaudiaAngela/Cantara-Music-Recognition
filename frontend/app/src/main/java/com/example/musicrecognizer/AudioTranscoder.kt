package com.example.musicrecognizer
import com.arthenica.ffmpegkit.FFmpegKit
import java.io.File

object AudioTranscoder {

    /**
     * Convert PCM 16-bit little-endian (raw) -> FLAC
     * @return true dacă a reușit conversia
     */
    fun pcmToFlac(
        pcmFile: File,
        flacFile: File,
        sampleRate: Int,
        channels: Int
    ): Boolean {

        // PCM raw nu are header, deci trebuie să-i spunem formatul:
        // -f s16le = signed 16-bit little endian
        // -ar 44100 = sample rate
        // -ac 1 = mono
        val cmd = listOf(
            "-hide_banner",
            "-loglevel", "error",
            "-f", "s16le",
            "-ar", sampleRate.toString(),
            "-ac", channels.toString(),
            "-i", pcmFile.absolutePath,
            "-compression_level", "5",
            flacFile.absolutePath
        ).joinToString(" ")

        val session = FFmpegKit.execute(cmd)
        return session.returnCode.isValueSuccess
    }
}