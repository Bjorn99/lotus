package com.dn0ne.player

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.audiofx.Equalizer
import android.os.CountDownTimer
import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.ui.util.fastForEach
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.dn0ne.player.core.data.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.koin.android.ext.android.get
import java.lang.Exception

private class EqualizerSettings(context: Context) {
    private val sharedPreferences =
        context.getSharedPreferences("equalizer-settings", Context.MODE_PRIVATE)

    private val isEnabledKey = "is-equalizer-enabled"
    private val bandFrequenciesKey = "band-frequencies"
    private val lowerLevelLimitKey = "lower-level-limit"
    private val upperLevelLimitKey = "upper-level-limit"
    private val bandLevelsKey = "band-levels"

    var isEnabled: Boolean
        get() = sharedPreferences.getBoolean(isEnabledKey, false)
        set(value) {
            with(sharedPreferences.edit()) {
                putBoolean(isEnabledKey, value)
                apply()
            }
        }

    var bandFrequencies: List<String>?
        get() = sharedPreferences.getString(bandFrequenciesKey, null)
            ?.let { Json.decodeFromString(it) }
        set(value) {
            with(sharedPreferences.edit()) {
                putString(bandFrequenciesKey, value?.let { Json.encodeToString(it) })
                apply()
            }
        }

    var lowerLevelLimit: Int
        get() = sharedPreferences.getInt(lowerLevelLimitKey, 0)
        set(value) {
            with(sharedPreferences.edit()) {
                putInt(lowerLevelLimitKey, value)
                apply()
            }
        }

    var upperLevelLimit: Int
        get() = sharedPreferences.getInt(upperLevelLimitKey, 0)
        set(value) {
            with(sharedPreferences.edit()) {
                putInt(upperLevelLimitKey, value)
                apply()
            }
        }

    var bandLevels: List<Short>?
        get() = sharedPreferences.getString(bandLevelsKey, null)
            ?.let { Json.decodeFromString(it) }
        set(value) {
            with(sharedPreferences.edit()) {
                putString(bandLevelsKey, value?.let { Json.encodeToString(it) })
                apply()
            }
        }
}

class EqualizerController(context: Context) {
    private val _settings = EqualizerSettings(context)
    private var _audioSessionId: Int? = null
    var equalizer: Equalizer? = null
        private set

    init {
        firstLaunchInit()
    }

    private val _isEqEnabled = MutableStateFlow(_settings.isEnabled)
    val isEqEnabled = _isEqEnabled.asStateFlow()
    fun updateIsEqEnabled(value: Boolean) {
        _isEqEnabled.update { value }
        _settings.isEnabled = value

        _audioSessionId?.let {
            updateEqualizer(it)
        }
    }

    private val _bandFrequencies = MutableStateFlow<List<String>?>(_settings.bandFrequencies)
    val bandFrequencies = _bandFrequencies.asStateFlow()

    private val _lowerLevelLimit = MutableStateFlow<Short>(_settings.lowerLevelLimit.toShort())
    val lowerLevelLimit = _lowerLevelLimit.asStateFlow()

    private val _upperLevelLimit = MutableStateFlow<Short>(_settings.upperLevelLimit.toShort())
    val upperLevelLimit = _upperLevelLimit.asStateFlow()

    private val _bandLevels = MutableStateFlow(_settings.bandLevels)
    val bandLevels = _bandLevels.asStateFlow()
    fun updateBandLevels(levels: List<Short>) {
        _bandLevels.update { levels }
        _settings.bandLevels = levels

        _audioSessionId?.let {
            updateEqualizer(it)
        }
    }

    fun updateEqualizer(audioSessionId: Int) {
        equalizer?.release()

        _audioSessionId = audioSessionId

        if (!_settings.isEnabled) return

        equalizer = Equalizer(Int.MAX_VALUE, audioSessionId).apply {
            enabled = true

            (0 until numberOfBands).forEach {
                println("BAND FREQ RANGE: ${getBandFreqRange(it.toShort()).joinToString("..")}")
            }
            _settings.bandLevels?.forEachIndexed { band, level ->
                setBandLevel(band.toShort(), level)
            }
        }
    }

    fun releaseEqualizer() {
        equalizer?.release()
        equalizer = null
    }

    fun resetBandLevels() {
        _settings.bandLevels?.let { bandLevels ->
            val defaultLevels = List<Short>(bandLevels.size) { 0 }
            _settings.bandLevels = defaultLevels
            _bandLevels.update {
                defaultLevels
            }
        }

        _audioSessionId?.let {
            updateEqualizer(it)
        }
    }

    private fun firstLaunchInit() {
        if (_settings.bandFrequencies == null || _settings.bandLevels == null) {
            try {
                val audioTrack = AudioTrack.Builder()
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setSampleRate(44100)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(AudioTrack.getMinBufferSize(44100, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT))
                    .build()

                Equalizer(0, audioTrack.audioSessionId).run {
                    enabled = false

                    val defaultLevels = List<Short>(numberOfBands.toInt()) { 0 }
                    _settings.bandLevels = defaultLevels

                    val bandFreqs = List(numberOfBands.toInt()) {
                        getBandFreqRange(it.toShort()).joinToString("-") {
                            (it / 1000).toString()
                        } + "Hz"
                    }
                    _settings.bandFrequencies = bandFreqs

                    val lowerLevelLimit = bandLevelRange[0]
                    _settings.lowerLevelLimit = lowerLevelLimit.toInt()

                    val upperLevelLimit = bandLevelRange[1]
                    _settings.upperLevelLimit = upperLevelLimit.toInt()

                    release()
                }

                audioTrack.release()
            } catch (e: Exception) {
                Log.e("FIRST LAUNCH EQUALIZER INITIALIZATION", e.message, e)
            }
        }
    }
}

class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private val equalizerController = get<EqualizerController>()

    override fun onCreate() {
        super.onCreate()

        val shouldHandleAudioFocus = get<Settings>().handleAudioFocus
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, shouldHandleAudioFocus)
            .setHandleAudioBecomingNoisy(true)
            .build()

        player.addListener(object : Player.Listener {
            @OptIn(UnstableApi::class)
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY || playbackState == Player.STATE_BUFFERING) {
                    val audioSessionId = player.audioSessionId
                    if (audioSessionId != C.AUDIO_SESSION_ID_UNSET) {
                        equalizerController.updateEqualizer(audioSessionId)
                    }
                }
            }
        })

        SleepTimer.addOnFinishCallback {
            player.stop()
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(pendingIntent)
            .build()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player ?: return

        if (!player.playWhenReady
            || player.mediaItemCount == 0
            || player.playbackState == Player.STATE_ENDED
        ) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        equalizerController.releaseEqualizer()
        SleepTimer.stop()
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

}

object SleepTimer {
    var timer: CountDownTimer? = null
    private val finishCallbacks = mutableListOf<() -> Unit>()

    private val _minutesLeft = MutableStateFlow(1)
    val minutesLeft = _minutesLeft.asStateFlow()
    fun updateMinutesLeft(value: Int) {
        _minutesLeft.update { value }
    }

    private val _isRunning = MutableStateFlow(false)
    val isRunning = _isRunning.asStateFlow()

    fun start() {
        stop()
        timer = object : CountDownTimer(_minutesLeft.value.toLong() * 60000L, 60000L) {
            override fun onTick(millisUntilFinished: Long) {
                _minutesLeft.update { millisUntilFinished.toInt() / 60000 + 1 }
            }

            override fun onFinish() {
                finishCallbacks.fastForEach { it.invoke() }
                _isRunning.update { false }
            }

        }.start()

        _isRunning.update { true }
    }

    fun stop() {
        timer?.cancel()

        _isRunning.update { false }
    }

    fun addOnFinishCallback(callback: () -> Unit) {
        finishCallbacks += callback
    }
}