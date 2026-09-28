package com.example.musicrecognizer
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.example.musicrecognizer.databinding.ActivityMainBinding
import java.io.File
import com.bumptech.glide.Glide
import android.net.Uri

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var flacFile: File
    private val recorder = AudioRecorder()
    private var isRecording = false
    private lateinit var pcmFile: File
    private lateinit var trackManager: TrackManager

    companion object {
        // REQ_RECORD_AUDIO este codul cererii pentru permisiunea microfonului.
        private const val REQ_RECORD_AUDIO = 2001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inițializarea ViewBinding și încărcarea layout-ului principal
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)


        pcmFile = File(cacheDir, "recording.pcm")

        flacFile = File(cacheDir, "recording.flac")

        // // Inițializarea managerului responsabil de salvarea și încărcarea istoricului
        trackManager = TrackManager(this)

        // Butonul rotund din UI
        binding.btnShazam.setOnClickListener {
            onShazamButtonPressed()
        }
    }
    // Handler pentru apăsarea butonului principal.
    private fun onShazamButtonPressed() {
        if (!hasMicPermission()) {
            requestMicPermission()
            return
        }

        if (!isRecording) {
            startRecording()
        } else {
            stopRecording()
        }
    }

    // Pornirea înregistrării audio.
    private fun startRecording() {
        isRecording = true

        if (pcmFile.exists()) pcmFile.delete()

        recorder.start(pcmFile)

        Toast.makeText(this, "Recording started...", Toast.LENGTH_SHORT).show()
    }


    // Oprirea înregistrarii
    private fun stopRecording() {
        isRecording = false

        recorder.stop()

        Toast.makeText(this, "Recording stopped. PCM saved.", Toast.LENGTH_SHORT).show()

        Thread {
            // Ștergerea fișierului FLAC vechi, dacă există
            if (flacFile.exists()) flacFile.delete()

            // Conversia semnalului audio din format PCM în FLAC
            val ok = AudioTranscoder.pcmToFlac(
                pcmFile = pcmFile,
                flacFile = flacFile,
                sampleRate = recorder.getSampleRate(),
                channels = recorder.getChannels()
            )
            // Revenirea pe thread-ul principal pentru actualizarea UI
            runOnUiThread {
                if (ok) {
                    Toast.makeText(this, "FLAC created: ${flacFile.name}", Toast.LENGTH_SHORT).show()
                    // Dacă fișierul FLAC a fost creat cu succes,
                    // acesta este încărcat către backend pentru identificare
                    uploadFlacCoroutine()
                } else {
                    Toast.makeText(this, "FLAC conversion failed!", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    /**
     Upload FLAC la backend și afișează rezultatul
     */
    private fun uploadFlacCoroutine() {
        lifecycleScope.launch {
            try {
                Toast.makeText(this@MainActivity, "Uploading to backend...", Toast.LENGTH_SHORT).show()
                // Apel către funcția care trimite fișierul audio la backend
                val response = uploadFlacToBackend(flacFile)
                // Dacă serverul returnează o eroare, aceasta este afișată în UI
                if (response.error != null) {
                    binding.tvTitle.text = "Error"
                    binding.tvArtist.text = response.error
                    binding.tvAlbum.text = ""
                    binding.tvConfidence.text = ""
                    Toast.makeText(this@MainActivity, "Error: ${response.error}", Toast.LENGTH_LONG).show()
                } else {
                    // Afișarea datelor primite din backend în componentele TextView
                    binding.tvTitle.text = "Title: ${response.title ?: "Unknown"}"
                    binding.tvArtist.text = "Artist: ${response.artist ?: "Unknown"}"
                    binding.tvAlbum.text = "Album: ${response.album ?: "Unknown"}"
                    binding.tvConfidence.text = "Confidence: ${String.format("%.1f%%", (response.confidence ?: 0.0) * 100)}"

                    // Încărcarea imaginii albumului din URL-ul primit de la Spotify
                    if (!response.image_url.isNullOrEmpty()) {
                        Glide.with(this@MainActivity)
                            .load(response.image_url)
                            .placeholder(R.drawable.ic_music)
                            .error(R.drawable.ic_error)
                            .into(binding.imgAlbum)
                    }

                    // Configurarea butonului care deschide link-ul piesei în Spotify
                    if (!response.spotify_url.isNullOrEmpty()) {
                        binding.btnSpotify.setOnClickListener {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(response.spotify_url))
                            startActivity(intent)
                        }
                    }

                    // Salvarea rezultatului obținut în istoricul local
                    saveTrackToDatabase(response)

                    Toast.makeText(
                        this@MainActivity,
                        "Identified: ${response.title} - ${response.artist}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                // Tratarea excepțiilor apărute la upload sau la procesarea răspunsului
                binding.tvTitle.text = "Exception"
                binding.tvArtist.text = e.message ?: "Unknown error"
                Toast.makeText(this@MainActivity, "Upload error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // Salvarea piesei identificate în istoricul local al aplicației.
    private fun saveTrackToDatabase(response: IdentifyResponse) {
        try {
            val track = Track(
                title = response.title ?: "Unknown",
                artist = response.artist ?: "Unknown",
                album = response.album ?: "Unknown",
                imageUrl = response.image_url,
                spotifyUrl = response.spotify_url,
                confidence = response.confidence ?: 0.0,
                timestamp = System.currentTimeMillis()
            )
            // Salvarea efectivă a piesei în istoricul local
            trackManager.saveTrack(track)
            Toast.makeText(this@MainActivity, "Saved to history", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this@MainActivity, "Error saving: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    // Verificarea permisiunii pentru microfon.

    private fun hasMicPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
    // Solicitarea permisiunii pentru microfon.
    // Dacă utilizatorul nu a acordat încă accesul, Android afișează dialogul
    // standard de permisiune.
    private fun requestMicPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            REQ_RECORD_AUDIO
        )
    }
    // Tratarea răspunsului primit după solicitarea permisiunii.
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQ_RECORD_AUDIO) {
            val granted = grantResults.isNotEmpty() &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED

            if (granted) {
                Toast.makeText(this, "Microphone permission granted. Tap again.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Microphone permission denied.", Toast.LENGTH_SHORT).show()
            }
        }
    }
    // Crearea meniului aplicației.
    // Meniul conține opțiunea de acces la istoricul identificărilor.
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    // Aplicația deschide ecranul HistoryActivity.
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_history -> {
                startActivity(Intent(this, HistoryActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // Oprire sigură dacă utilizatorul iese din aplicație în timp ce înregistrează
    override fun onStop() {
        super.onStop()
        if (isRecording) {
            stopRecording()
        }
    }

}
