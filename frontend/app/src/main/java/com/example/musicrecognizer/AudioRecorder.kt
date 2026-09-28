package com.example.musicrecognizer
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicBoolean
import android.annotation.SuppressLint
class AudioRecorder {
    // setări audio
    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null
    private val isRecording = AtomicBoolean(false)

    @SuppressLint("MissingPermission")
    fun start(pcmFile: File) {
        val minBuf = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        val bufferSize = (minBuf * 2).coerceAtLeast(sampleRate)

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize
        )

        audioRecord?.startRecording()
        isRecording.set(true)

        recordingThread = Thread {
            FileOutputStream(pcmFile).use { out ->
                val buffer = ByteArray(bufferSize)
                while (isRecording.get()) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (read > 0) out.write(buffer, 0, read)
                }
                out.flush()
            }
        }.also { it.start() }
    }

    fun stop() {
        isRecording.set(false)

        try { recordingThread?.join(800) } catch (_: InterruptedException) {}
        recordingThread = null

        audioRecord?.apply {
            try { stop() } catch (_: Exception) {}
            release()
        }
        audioRecord = null
    }

    fun getSampleRate(): Int = sampleRate
    fun getChannels(): Int = 1 // mono
}