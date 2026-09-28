package com.example.musicrecognizer

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.musicrecognizer.databinding.ItemTrackBinding
import java.text.SimpleDateFormat
import java.util.*

class TrackAdapter(
    private val onSpotifyClick: (Track) -> Unit,
    private val onDeleteClick: (Track) -> Unit
) : ListAdapter<Track, TrackAdapter.TrackViewHolder>(TrackDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrackViewHolder {
        val binding = ItemTrackBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TrackViewHolder(binding, onSpotifyClick, onDeleteClick)
    }

    override fun onBindViewHolder(holder: TrackViewHolder, position: Int) {
        try {
            holder.bind(getItem(position))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    class TrackViewHolder(
        private val binding: ItemTrackBinding,
        private val onSpotifyClick: (Track) -> Unit,
        private val onDeleteClick: (Track) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(track: Track) {
            binding.tvTrackTitle.text = track.title ?: "Unknown"
            binding.tvTrackArtist.text = track.artist ?: "Unknown"
            binding.tvTrackAlbum.text = track.album ?: "Unknown"
            binding.tvTrackConfidence.text = "Confidence: ${String.format("%.1f%%", track.confidence * 100)}"
            binding.tvTrackDate.text = formatDate(track.timestamp)

            if (!track.imageUrl.isNullOrEmpty()) {
                Glide.with(binding.root.context)
                    .load(track.imageUrl)
                    .placeholder(R.drawable.ic_music)
                    .error(R.drawable.ic_music)
                    .into(binding.imgTrack)
            }

            binding.btnTrackSpotify.setOnClickListener {
                onSpotifyClick(track)
            }

            binding.btnTrackDelete.setOnClickListener {
                onDeleteClick(track)
            }
        }

        private fun formatDate(timestamp: Long): String {
            return try {
                val sdf = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
                sdf.format(Date(timestamp))
            } catch (e: Exception) {
                "Unknown date"
            }
        }
    }

    class TrackDiffCallback : DiffUtil.ItemCallback<Track>() {
        override fun areItemsTheSame(oldItem: Track, newItem: Track): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Track, newItem: Track): Boolean {
            return oldItem == newItem
        }
    }
}