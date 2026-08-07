package com.example.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.player.AudioPlayerManager

class MusicWidgetReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val player = AudioPlayerManager.activeInstance

        when (intent?.action) {
            MusicWidgetManager.ACTION_TOGGLE_PLAY -> {
                player?.togglePlayPause()
            }
            MusicWidgetManager.ACTION_PREV -> {
                player?.previousTrack()
            }
            MusicWidgetManager.ACTION_NEXT -> {
                player?.nextTrack()
            }
            MusicWidgetManager.ACTION_FAVORITE -> {
                val currentTrack = player?.currentTrack?.value
                if (currentTrack != null) {
                    player.updateCurrentTrackFavorite(!currentTrack.isFavorite)
                }
            }
            MusicWidgetManager.ACTION_SHUFFLE -> {
                player?.toggleShuffle()
            }
            MusicWidgetManager.ACTION_REPEAT -> {
                player?.toggleRepeat()
            }
        }

        // Immediately update all widgets
        MusicWidgetManager.updateAllWidgets(context)
    }
}
