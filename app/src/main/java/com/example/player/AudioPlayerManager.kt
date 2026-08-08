package com.example.player

import android.content.Context
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.audiofx.Equalizer
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.media3.common.AudioAttributes as Media3AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.model.EqualizerPreset
import com.example.model.GlassTheme
import com.example.model.Track
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class RepeatMode {
    OFF, ALL, ONE
}

class AudioPlayerManager(private val context: Context) {

    companion object {
        @Volatile
        private var instance: AudioPlayerManager? = null

        var activeInstance: AudioPlayerManager?
            get() = instance
            set(value) { instance = value }

        fun getInstance(context: Context): AudioPlayerManager {
            return instance ?: synchronized(this) {
                instance ?: AudioPlayerManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var exoPlayer: ExoPlayer? = null
    private var equalizerEffect: Equalizer? = null

    // Audio Focus Management
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var resumeOnFocusGain = false
    private var isDucked = false
    private var focusRequest: AudioFocusRequest? = null

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                resumeOnFocusGain = false
                isDucked = false
                try {
                    exoPlayer?.volume = 1.0f
                } catch (_: Exception) {}
                pause(userAction = false)
                abandonAudioFocus()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                if (_isPlaying.value) {
                    resumeOnFocusGain = true
                    pause(userAction = false)
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                if (_isPlaying.value) {
                    isDucked = true
                    try {
                        exoPlayer?.volume = 0.3f
                    } catch (e: Exception) {
                        Log.e("AudioPlayerManager", "Error setting volume for ducking", e)
                    }
                }
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                if (isDucked) {
                    isDucked = false
                    try {
                        exoPlayer?.volume = 1.0f
                    } catch (e: Exception) {
                        Log.e("AudioPlayerManager", "Error restoring volume after ducking", e)
                    }
                }
                if (resumeOnFocusGain) {
                    resumeOnFocusGain = false
                    try {
                        exoPlayer?.volume = 1.0f
                    } catch (_: Exception) {}
                    resume()
                }
            }
        }
    }

    private fun requestAudioFocus(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attr = android.media.AudioAttributes.Builder()
                .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(attr)
                .setWillPauseWhenDucked(false)
                .setAcceptsDelayedFocusGain(true)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()
            focusRequest = request
            val res = audioManager.requestAudioFocus(request)
            return res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            val res = audioManager.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
            return res == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
            focusRequest = null
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(audioFocusChangeListener)
        }
    }

    // State Flows
    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: StateFlow<Track?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0)
    val currentPositionMs: StateFlow<Int> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(200000)
    val durationMs: StateFlow<Int> = _durationMs.asStateFlow()

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.OFF)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    // Sleep Timer (seconds remaining)
    private val _sleepTimerSeconds = MutableStateFlow<Int?>(null)
    val sleepTimerSeconds: StateFlow<Int?> = _sleepTimerSeconds.asStateFlow()

    private var sleepTimerJob: Job? = null

    // Theme & Equalizer
    private val _currentTheme = MutableStateFlow(GlassTheme.DarkGreen)
    val currentTheme: StateFlow<GlassTheme> = _currentTheme.asStateFlow()

    private val _activeEqPreset = MutableStateFlow(EqualizerPreset.Flat)
    val activeEqPreset: StateFlow<EqualizerPreset> = _activeEqPreset.asStateFlow()

    private val _eqBandGains = MutableStateFlow(listOf(0f, 0f, 0f, 0f, 0f))
    val eqBandGains: StateFlow<List<Float>> = _eqBandGains.asStateFlow()

    // Track Queue & Shuffle Data
    private var playlistQueue = listOf<Track>()
    private var currentIndex = -1
    private val shuffleQueue = mutableListOf<Int>()
    private var shufflePointer = -1

    private fun generateShuffleQueue() {
        shuffleQueue.clear()
        if (playlistQueue.isEmpty() || currentIndex !in playlistQueue.indices) {
            shufflePointer = -1
            return
        }

        val size = playlistQueue.size
        val currentIdx = currentIndex

        if (size == 1) {
            repeat(100) { shuffleQueue.add(currentIdx) }
            shufflePointer = 19
            return
        }

        val otherIndices = (0 until size).filter { it != currentIdx }

        val beforeList = mutableListOf<Int>()
        var lastAdded = currentIdx
        for (i in 0 until 19) {
            val candidates = otherIndices.filter { it != lastAdded }.ifEmpty { otherIndices }
            val picked = candidates.random()
            beforeList.add(picked)
            lastAdded = picked
        }

        val afterList = mutableListOf<Int>()
        lastAdded = currentIdx
        for (i in 0 until 80) {
            val candidates = otherIndices.filter { it != lastAdded }.ifEmpty { otherIndices }
            val picked = candidates.random()
            afterList.add(picked)
            lastAdded = picked
        }

        shuffleQueue.addAll(beforeList) // 0..18 (19 items)
        shuffleQueue.add(currentIdx)    // 19 (20th item)
        shuffleQueue.addAll(afterList)  // 20..99 (80 items)

        shufflePointer = 19
    }

    private fun clearShuffleQueue() {
        shuffleQueue.clear()
        shufflePointer = -1
    }

    private var progressJob: Job? = null

    init {
        activeInstance = this
        startProgressLoop()
    }

    fun updateServiceNotification() {
        try {
            com.example.widget.MusicWidgetManager.updateAllWidgets(context)
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Failed to update widgets", e)
        }

        val track = _currentTrack.value ?: return
        try {
            com.example.service.MediaPlaybackService.updateNotification(
                context = context,
                title = track.title,
                artist = track.artist,
                album = track.album,
                durationMs = _durationMs.value,
                positionMs = _currentPositionMs.value,
                albumArtUri = track.albumArtUri,
                gradientIndex = track.coverGradientIndex,
                isPlaying = _isPlaying.value,
                isFavorite = track.isFavorite
            )
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Failed to update notification service", e)
        }
    }

    private fun getOrCreateExoPlayer(): ExoPlayer {
        val existing = exoPlayer
        if (existing != null) return existing

        return ExoPlayer.Builder(context)
            .setAudioAttributes(
                Media3AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                /* handleAudioFocus = */ false
            )
            .build().also { player ->
                player.addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        when (playbackState) {
                            Player.STATE_READY -> {
                                val dur = player.duration
                                _durationMs.value = if (dur > 0) dur.toInt() else (_currentTrack.value?.durationSeconds?.times(1000) ?: 200000)
                                setupEqualizer(player.audioSessionId)
                                updateServiceNotification()
                                triggerPlaybackStateSave()
                            }
                            Player.STATE_ENDED -> {
                                handleTrackCompletion()
                            }
                            else -> {}
                        }
                    }

                    override fun onIsPlayingChanged(isPlaying: Boolean) {
                        _isPlaying.value = isPlaying
                        updateServiceNotification()
                        triggerPlaybackStateSave()
                    }

                    override fun onAudioSessionIdChanged(audioSessionId: Int) {
                        setupEqualizer(audioSessionId)
                    }

                    override fun onPlayerError(error: PlaybackException) {
                        Log.e("AudioPlayerManager", "ExoPlayer error: ${error.message}", error)
                        _isPlaying.value = false
                        updateServiceNotification()
                    }
                })
                exoPlayer = player
            }
    }

    fun setQueue(tracks: List<Track>, startIndex: Int = 0) {
        val targetTrack = tracks.getOrNull(startIndex)
        if (targetTrack != null && _currentTrack.value?.id == targetTrack.id && exoPlayer != null) {
            playlistQueue = tracks
            currentIndex = startIndex
            if (!_isPlaying.value) {
                resume()
            }
            return
        }
        playlistQueue = tracks
        if (tracks.isNotEmpty() && startIndex in tracks.indices) {
            currentIndex = startIndex
            if (_isShuffle.value) {
                generateShuffleQueue()
            } else {
                clearShuffleQueue()
            }
            playTrackAtIndex(startIndex)
        }
    }

    fun playTrack(track: Track) {
        if (_currentTrack.value?.id == track.id && exoPlayer != null) {
            if (!_isPlaying.value) {
                resume()
            }
            return
        }
        val index = playlistQueue.indexOfFirst { it.id == track.id }
        if (index != -1) {
            currentIndex = index
            if (_isShuffle.value) {
                generateShuffleQueue()
            } else {
                clearShuffleQueue()
            }
            playTrackAtIndex(index)
        } else {
            playlistQueue = listOf(track)
            currentIndex = 0
            if (_isShuffle.value) {
                generateShuffleQueue()
            } else {
                clearShuffleQueue()
            }
            playTrackAtIndex(0)
        }
    }

    private var currentTrackListeningSeconds: Long = 0L
    private var lastFlushTimeMs: Long = 0L
    var onFlushListeningTimeListener: ((trackId: Long, seconds: Long) -> Unit)? = null
    var onPlaybackStateChanged: ((trackId: Long, positionMs: Long, queueTrackIds: List<Long>) -> Unit)? = null

    fun triggerPlaybackStateSave() {
        val trackId = _currentTrack.value?.id ?: return
        val posMs = _currentPositionMs.value.toLong()
        val queueIds = playlistQueue.map { it.id }
        onPlaybackStateChanged?.invoke(trackId, posMs, queueIds)
    }

    fun restorePlaybackState(tracks: List<Track>, index: Int, positionMs: Int) {
        if (tracks.isEmpty() || index !in tracks.indices) return
        playlistQueue = tracks
        currentIndex = index
        if (_isShuffle.value) {
            generateShuffleQueue()
        } else {
            clearShuffleQueue()
        }
        val track = tracks[index]
        _currentTrack.value = track
        _durationMs.value = if (track.durationSeconds > 0) track.durationSeconds * 1000 else 200000
        _currentPositionMs.value = positionMs
        _isPlaying.value = false
        updateServiceNotification()
    }

    fun flushListeningTime() {
        val trackId = _currentTrack.value?.id ?: return
        val secondsToFlush = currentTrackListeningSeconds
        if (secondsToFlush > 0) {
            currentTrackListeningSeconds = 0L
            lastFlushTimeMs = System.currentTimeMillis()
            onFlushListeningTimeListener?.invoke(trackId, secondsToFlush)
        }
    }

    private fun playTrackAtIndex(index: Int, startPositionMs: Int = 0) {
        if (index !in playlistQueue.indices) return
        flushListeningTime()
        currentIndex = index
        val track = playlistQueue[index]
        _currentTrack.value = track
        _durationMs.value = if (track.durationSeconds > 0) track.durationSeconds * 1000 else 200000
        _currentPositionMs.value = if (startPositionMs > 0) startPositionMs else 0

        val player = getOrCreateExoPlayer()

        val url = track.audioUrl
        val uri: Uri? = when {
            url.startsWith("content://") || url.startsWith("file://") || url.startsWith("http://") || url.startsWith("https://") -> {
                Uri.parse(url)
            }
            url.startsWith("/") -> {
                val file = java.io.File(url)
                if (file.exists()) Uri.fromFile(file) else Uri.parse("file://$url")
            }
            url.isNotBlank() -> {
                Uri.parse(url)
            }
            else -> null
        }

        if (uri == null) {
            Log.e("AudioPlayerManager", "Track audio URL is empty")
            _isPlaying.value = false
            updateServiceNotification()
            return
        }

        try {
            val mediaItem = MediaItem.fromUri(uri)
            player.stop()
            player.setMediaItem(mediaItem)
            player.prepare()

            if (startPositionMs > 0) {
                player.seekTo(startPositionMs.toLong())
            }

            requestAudioFocus()
            player.playWhenReady = true
            _isPlaying.value = true

            setupEqualizer(player.audioSessionId)
            updateServiceNotification()
            triggerPlaybackStateSave()
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Failed to prepare ExoPlayer for track: ${track.title}", e)
            _isPlaying.value = false
            updateServiceNotification()
        }
        triggerPlaybackStateSave()
    }

    fun togglePlayPause() {
        if (_isPlaying.value) {
            pause()
        } else {
            resume()
        }
    }

    fun pause(userAction: Boolean = true) {
        if (userAction) {
            resumeOnFocusGain = false
            abandonAudioFocus()
        }
        flushListeningTime()
        _isPlaying.value = false
        try {
            exoPlayer?.playWhenReady = false
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Error pausing ExoPlayer", e)
        }
        updateServiceNotification()
        triggerPlaybackStateSave()
    }

    fun stop() {
        resumeOnFocusGain = false
        abandonAudioFocus()
        stopCurrentMedia()
        _isPlaying.value = false
        _currentPositionMs.value = 0
        updateServiceNotification()
        triggerPlaybackStateSave()
    }

    fun resume() {
        if (_currentTrack.value == null && playlistQueue.isNotEmpty()) {
            playTrackAtIndex(0)
            return
        }
        if (exoPlayer == null && _currentTrack.value != null) {
            playTrackAtIndex(currentIndex, startPositionMs = _currentPositionMs.value)
            return
        }
        requestAudioFocus()
        try {
            exoPlayer?.playWhenReady = true
            _isPlaying.value = true
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Error resuming ExoPlayer", e)
            _isPlaying.value = false
        }
        updateServiceNotification()
        triggerPlaybackStateSave()
    }

    fun seekTo(positionMs: Int) {
        _currentPositionMs.value = positionMs
        try {
            exoPlayer?.seekTo(positionMs.toLong())
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Error seeking ExoPlayer", e)
        }
        updateServiceNotification()
        triggerPlaybackStateSave()
    }

    fun nextTrack() {
        if (playlistQueue.isEmpty()) return

        if (_isShuffle.value) {
            if (shuffleQueue.isEmpty() || shufflePointer < 0) {
                generateShuffleQueue()
            } else {
                shufflePointer++
                if (shufflePointer >= shuffleQueue.size) {
                    val size = playlistQueue.size
                    val otherIndices = (0 until size).filter { it != currentIndex }.ifEmpty { (0 until size).toList() }
                    var lastAdded = shuffleQueue.lastOrNull() ?: currentIndex
                    for (i in 0 until 80) {
                        val candidates = otherIndices.filter { it != lastAdded }.ifEmpty { otherIndices }
                        val picked = candidates.random()
                        shuffleQueue.add(picked)
                        lastAdded = picked
                    }
                }
            }
            val targetIndex = shuffleQueue.getOrNull(shufflePointer) ?: 0
            playTrackAtIndex(targetIndex)
        } else {
            clearShuffleQueue()
            val nextIdx = (currentIndex + 1) % playlistQueue.size
            playTrackAtIndex(nextIdx)
        }
    }

    fun previousTrack() {
        if (playlistQueue.isEmpty()) return
        if (_currentPositionMs.value > 3000) {
            seekTo(0)
            return
        }

        if (_isShuffle.value) {
            if (shuffleQueue.isEmpty() || shufflePointer <= 0) {
                seekTo(0)
            } else {
                shufflePointer--
                val prevIdx = shuffleQueue.getOrElse(shufflePointer) { 0 }
                playTrackAtIndex(prevIdx)
            }
        } else {
            clearShuffleQueue()
            val prevIdx = if (currentIndex - 1 < 0) playlistQueue.size - 1 else currentIndex - 1
            playTrackAtIndex(prevIdx)
        }
    }

    fun updateCurrentTrackFavorite(isFavorite: Boolean) {
        val current = _currentTrack.value ?: return
        _currentTrack.value = current.copy(isFavorite = isFavorite)
        playlistQueue = playlistQueue.map {
            if (it.id == current.id) it.copy(isFavorite = isFavorite) else it
        }
        updateServiceNotification()
    }

    fun toggleFavoriteCurrentTrack() {
        val current = _currentTrack.value ?: return
        val newIsFavorite = !current.isFavorite
        updateCurrentTrackFavorite(newIsFavorite)
        scope.launch(Dispatchers.IO) {
            try {
                val dao = com.example.data.local.AppDatabase.getDatabase(context).musicDao()
                dao.updateFavoriteStatus(current.id, newIsFavorite)
            } catch (e: Exception) {
                Log.e("AudioPlayerManager", "Failed to update DB favorite status", e)
            }
        }
    }

    fun updateCurrentTrackInfo(title: String, artist: String, album: String = "") {
        val current = _currentTrack.value ?: return
        val newAlbum = if (album.isNotBlank()) album else current.album
        _currentTrack.value = current.copy(title = title, artist = artist, album = newAlbum)
        playlistQueue = playlistQueue.map {
            if (it.id == current.id) it.copy(title = title, artist = artist, album = newAlbum) else it
        }
        updateServiceNotification()
    }

    fun removeTrackFromQueue(trackId: Long) {
        val isCurrentPlaying = _currentTrack.value?.id == trackId
        playlistQueue = playlistQueue.filter { it.id != trackId }
        
        if (isCurrentPlaying) {
            if (playlistQueue.isNotEmpty()) {
                if (currentIndex >= playlistQueue.size) {
                    currentIndex = 0
                }
                if (_isShuffle.value) {
                    generateShuffleQueue()
                } else {
                    clearShuffleQueue()
                }
                playTrackAtIndex(currentIndex)
            } else {
                exoPlayer?.stop()
                exoPlayer?.clearMediaItems()
                _isPlaying.value = false
                _currentTrack.value = null
                clearShuffleQueue()
            }
        } else {
            // Adjust currentIndex if necessary
            val currentId = _currentTrack.value?.id
            if (currentId != null) {
                currentIndex = playlistQueue.indexOfFirst { it.id == currentId }
            }
            if (_isShuffle.value) {
                generateShuffleQueue()
            } else {
                clearShuffleQueue()
            }
        }
    }

    fun setShuffle(enabled: Boolean) {
        _isShuffle.value = enabled
        if (enabled) {
            generateShuffleQueue()
        } else {
            clearShuffleQueue()
        }
        updateServiceNotification()
    }

    fun toggleShuffle() {
        _isShuffle.value = !_isShuffle.value
        if (_isShuffle.value) {
            generateShuffleQueue()
        } else {
            clearShuffleQueue()
        }
        updateServiceNotification()
    }

    fun toggleRepeat() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        updateServiceNotification()
    }

    fun cyclePlaybackMode() {
        when {
            !_isShuffle.value && _repeatMode.value == RepeatMode.OFF -> {
                // Sequential -> Repeat Track
                _isShuffle.value = false
                _repeatMode.value = RepeatMode.ONE
                clearShuffleQueue()
            }
            !_isShuffle.value && _repeatMode.value == RepeatMode.ONE -> {
                // Repeat Track -> Shuffle
                _isShuffle.value = true
                _repeatMode.value = RepeatMode.OFF
                generateShuffleQueue()
            }
            else -> {
                // Shuffle -> Sequential
                _isShuffle.value = false
                _repeatMode.value = RepeatMode.OFF
                clearShuffleQueue()
            }
        }
        updateServiceNotification()
    }

    private fun handleTrackCompletion() {
        flushListeningTime()
        when (_repeatMode.value) {
            RepeatMode.ONE -> playTrackAtIndex(currentIndex)
            RepeatMode.ALL -> nextTrack()
            RepeatMode.OFF -> {
                if (_isShuffle.value) {
                    nextTrack()
                } else {
                    if (currentIndex < playlistQueue.lastIndex) {
                        playTrackAtIndex(currentIndex + 1)
                    } else {
                        _isPlaying.value = false
                        _currentPositionMs.value = 0
                        try {
                            exoPlayer?.seekTo(0)
                            exoPlayer?.playWhenReady = false
                        } catch (e: Exception) {
                            Log.e("AudioPlayerManager", "Error seeking to 0 on complete", e)
                        }
                        updateServiceNotification()
                    }
                }
            }
        }
    }

    // Sleep Timer Logic
    fun setSleepTimerMinutes(minutes: Int?) {
        sleepTimerJob?.cancel()
        if (minutes == null || minutes <= 0) {
            _sleepTimerSeconds.value = null
            return
        }
        val totalSec = minutes * 60
        _sleepTimerSeconds.value = totalSec

        sleepTimerJob = scope.launch(Dispatchers.Main) {
            var remaining = totalSec
            while (remaining > 0 && isActive) {
                delay(1000L)
                remaining--
                _sleepTimerSeconds.value = remaining
            }
            if (remaining <= 0) {
                pause()
                _sleepTimerSeconds.value = null
            }
        }
    }

    // Equalizer logic
    private fun setupEqualizer(audioSessionId: Int) {
        if (audioSessionId == 0) return
        try {
            equalizerEffect?.release()
            equalizerEffect = Equalizer(0, audioSessionId).apply {
                enabled = true
                applyEqualizerToHardware()
            }
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Failed to initialize hardware Equalizer effect", e)
        }
    }

    private fun applyEqualizerToHardware() {
        val eq = equalizerEffect ?: return
        try {
            if (!eq.enabled) {
                eq.enabled = true
            }
            val numBands = eq.numberOfBands.toInt()
            val range = eq.bandLevelRange
            val minLevel = range.getOrNull(0) ?: -1500
            val maxLevel = range.getOrNull(1) ?: 1500
            val gains = _eqBandGains.value

            for (i in 0 until numBands) {
                if (i in gains.indices) {
                    val gainMb = (gains[i] * 100f).toInt().coerceIn(minLevel.toInt(), maxLevel.toInt()).toShort()
                    eq.setBandLevel(i.toShort(), gainMb)
                }
            }
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Error applying equalizer gains", e)
        }
    }

    fun setEqualizerPreset(preset: EqualizerPreset) {
        if (preset.name.equals("Custom", ignoreCase = true)) {
            _activeEqPreset.value = EqualizerPreset.Custom.copy(gains = _eqBandGains.value)
        } else {
            _activeEqPreset.value = preset
            _eqBandGains.value = preset.gains
        }
        applyEqualizerToHardware()
    }

    fun setCustomGains(gains: List<Float>) {
        if (gains.size == 5) {
            _eqBandGains.value = gains
            _activeEqPreset.value = EqualizerPreset.Custom.copy(gains = gains)
            applyEqualizerToHardware()
        }
    }

    fun updateCustomBandGain(bandIndex: Int, gain: Float) {
        val currentGains = _eqBandGains.value.toMutableList()
        if (bandIndex in currentGains.indices) {
            currentGains[bandIndex] = gain
            _eqBandGains.value = currentGains
            _activeEqPreset.value = EqualizerPreset.Custom.copy(gains = currentGains)
            applyEqualizerToHardware()
        }
    }

    // Theme switching
    fun setTheme(theme: GlassTheme) {
        _currentTheme.value = theme
    }

    private fun startProgressLoop() {
        progressJob = scope.launch(Dispatchers.Main) {
            var playSecondCounter = 0
            while (isActive) {
                delay(250L)
                val player = exoPlayer
                if (_isPlaying.value && player != null) {
                    try {
                        if (player.isPlaying) {
                            _currentPositionMs.value = player.currentPosition.toInt()
                            playSecondCounter++
                            if (playSecondCounter >= 4) {
                                playSecondCounter = 0
                                currentTrackListeningSeconds++

                                // Throttle DB updates: flush every 10 seconds of continuous playback
                                if (currentTrackListeningSeconds >= 10L || (System.currentTimeMillis() - lastFlushTimeMs) >= 10_000L) {
                                    flushListeningTime()
                                    triggerPlaybackStateSave()
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // ignore transient state errors
                    }
                }
            }
        }
    }

    private fun stopCurrentMedia() {
        flushListeningTime()
        try {
            equalizerEffect?.release()
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Error releasing Equalizer", e)
        }
        equalizerEffect = null
        exoPlayer?.apply {
            try {
                stop()
                clearMediaItems()
            } catch (e: Exception) {}
        }
    }

    fun release() {
        flushListeningTime()
        progressJob?.cancel()
        sleepTimerJob?.cancel()
        stopCurrentMedia()
        try {
            exoPlayer?.release()
        } catch (e: Exception) {}
        exoPlayer = null
        instance = null
    }
}
