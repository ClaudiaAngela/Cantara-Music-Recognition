package com.example.musicrecognizer
import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
// Clasă responsabilă de gestionarea istoricului local al pieselor identificate.
class TrackManager(private val context: Context) {
    // SharedPreferences utilizat pentru stocarea locală a istoricului.
    private val sharedPref = context.getSharedPreferences("tracks", Context.MODE_PRIVATE)
    // Instanță Gson folosită pentru conversia listei de Track în JSON și invers.
    private val gson = Gson()
    private val key = "track_list"

    fun saveTrack(track: Track) {
        val tracks = getAllTracks().toMutableList()
        // Adăugarea piesei noi la începutul listei.
        tracks.add(0, track.copy(id = tracks.size + 1))

        val json = gson.toJson(tracks)
        sharedPref.edit().putString(key, json).apply()
    }
    // Returnează toate piesele salvate local. Dacă nu există date, se returnează o listă goală
    fun getAllTracks(): List<Track> {
        val json = sharedPref.getString(key, "[]") ?: "[]"
        val type = object : TypeToken<List<Track>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }
    // Șterge o piesă din istoric
    fun deleteTrack(track: Track) {
        val tracks = getAllTracks().toMutableList()
        tracks.remove(track)

        val json = gson.toJson(tracks)
        sharedPref.edit().putString(key, json).apply()
    }
    // Șterge complet istoricul local

    fun deleteAll() {
        sharedPref.edit().clear().apply()
    }
}