package me.bmax.apatch.util

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import me.bmax.apatch.ui.theme.MusicConfig
import java.io.File

object MusicManager : DefaultLifecycleObserver {
    private const val TAG = "MusicManager"
    private var mediaPlayer: MediaPlayer? = null
    private var context: Context? = null
    
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var progressJob: Job? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0)
    val currentPosition: StateFlow<Int> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0)
    val duration: StateFlow<Int> = _duration.asStateFlow()

    fun init(ctx: Context) {
        context = ctx.applicationContext
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        
        // Initial setup if enabled and auto-play is on
        if (MusicConfig.isMusicEnabled && MusicConfig.isAutoPlayEnabled) {
            prepareAndPlay()
        }
    }

    private fun startProgressUpdater() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                if (mediaPlayer?.isPlaying == true) {
                    _currentPosition.value = mediaPlayer?.currentPosition ?: 0
                }
                delay(1000)
            }
        }
    }

    private fun prepareAndPlay() {
        if (context == null) return
        val file = MusicConfig.getMusicFile(context!!) ?: return

        try {
            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer()
            } else {
                mediaPlayer?.reset()
            }

            mediaPlayer?.apply {
                setDataSource(context!!, Uri.fromFile(file))
                setVolume(MusicConfig.volume, MusicConfig.volume)
                isLooping = MusicConfig.isLoopingEnabled
                setOnPreparedListener { mp ->
                    mp.start()
                    _duration.value = mp.duration
                    _isPlaying.value = true
                    startProgressUpdater()
                }
                setOnCompletionListener {
                    if (!isLooping) {
                        _isPlaying.value = false
                        _currentPosition.value = 0
                    }
                }
                setOnErrorListener { _, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what extra=$extra")
                    _isPlaying.value = false
                    true
                }
                prepareAsync()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play music", e)
            _isPlaying.value = false
        }
    }

    fun play() {
        if (mediaPlayer == null) {
            prepareAndPlay()
        } else {
            try {
                if (!mediaPlayer!!.isPlaying) {
                    mediaPlayer?.start()
                    _isPlaying.value = true
                    startProgressUpdater()
                }
            } catch (e: Exception) {
                 Log.e(TAG, "Error in play()", e)
                 // Try to recover
                 prepareAndPlay()
            }
        }
    }

    fun seekTo(position: Int) {
        try {
            mediaPlayer?.seekTo(position)
            _currentPosition.value = position
        } catch (e: Exception) {
            Log.e(TAG, "Error in seekTo()", e)
        }
    }

    fun pause() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
                _isPlaying.value = false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in pause()", e)
        }
    }

    fun toggle() {
        if (_isPlaying.value) {
            pause()
        } else {
            play()
        }
    }

    fun stop() {
        try {
            progressJob?.cancel()
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            _isPlaying.value = false
            _currentPosition.value = 0
            _duration.value = 0
        } catch (e: Exception) {
            Log.e(TAG, "Error in stop()", e)
        }
    }

    fun updateVolume(volume: Float) {
        try {
            mediaPlayer?.setVolume(volume, volume)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting volume", e)
        }
    }

    fun updateLooping(looping: Boolean) {
        try {
            mediaPlayer?.isLooping = looping
        } catch (e: Exception) {
            Log.e(TAG, "Error setting looping", e)
        }
    }
    
    fun reload() {
        // Called when settings change (e.g. new file selected)
        stop()
        if (MusicConfig.isMusicEnabled) {
             if (MusicConfig.isAutoPlayEnabled || _isPlaying.value) {
                 prepareAndPlay()
             }
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        // App comes to foreground
        if (MusicConfig.isMusicEnabled && MusicConfig.isAutoPlayEnabled) {
             if (mediaPlayer == null) {
                 prepareAndPlay()
             } else if (!mediaPlayer!!.isPlaying) {
                 // Resume playback from where it paused
                 play()
             }
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        // App goes to background
        pause()
    }
}
