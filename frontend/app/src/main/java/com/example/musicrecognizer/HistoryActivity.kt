package com.example.musicrecognizer
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.musicrecognizer.databinding.ActivityHistoryBinding
class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private lateinit var adapter: TrackAdapter
    private lateinit var trackManager: TrackManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inițializarea ViewBinding și setarea layout-ului
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        // Inițializarea managerului care citește și șterge piesele salvate local
        trackManager = TrackManager(this)

        // Inițializarea adaptorului pentru RecyclerView
        adapter = TrackAdapter(
            onSpotifyClick = { track -> openSpotify(track) },
            onDeleteClick = { track -> deleteTrack(track) }
        )
        // Configurarea listei verticale pentru istoricul pieselor
        binding.recyclerViewHistory.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewHistory.adapter = adapter
        // Încărcarea datelor salvate local
        loadTracks()
    }
    // Încărcarea tuturor pieselor din istoricul local.
    private fun loadTracks() {
        try {
            val tracks = trackManager.getAllTracks()

            if (tracks.isNotEmpty()) {
                adapter.submitList(tracks)
                binding.tvEmptyHistory.visibility = android.view.View.GONE
            } else {  // Dacă istoricul este gol, se afișează un mesaj corespunzător.
                binding.recyclerViewHistory.visibility = android.view.View.GONE
                binding.tvEmptyHistory.visibility = android.view.View.VISIBLE
                binding.tvEmptyHistory.text = "No tracks yet"
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    // Deschiderea piesei în aplicația Spotify sau în browser.
    private fun openSpotify(track: Track) {
        try {
            if (!track.spotifyUrl.isNullOrEmpty()) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(track.spotifyUrl))
                startActivity(intent)
            } else {
                Toast.makeText(this, "No Spotify URL", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    // Ștergerea unei singure piese din istoricul local.
    private fun deleteTrack(track: Track) {
        try {
            trackManager.deleteTrack(track)
            loadTracks()
            Toast.makeText(this, "Deleted", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    // Tratarea acțiunilor din meniu.
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            // Butonul Home închide ecranul de istoric
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_clear_history -> {
                try {  // Ștergerea completă a istoricului local
                    trackManager.deleteAll()
                    loadTracks()
                    Toast.makeText(this, "Cleared", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    // Crearea meniului specific ecranului de istoric.
    // Se încarcă resursele din fișierul history_menu.
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        try {
            menuInflater.inflate(R.menu.history_menu, menu)
            return true
        } catch (e: Exception) {
            return false
        }
    }
}