package com.example.alarm.core.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.util.Log
import com.example.alarm.data.db.AlarmDatabase
import com.example.alarm.data.db.entity.CustomSoundEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

data class Sound(
    val id: String,
    val name: String,
    val uri: Uri,
    val isCustom: Boolean = false
)

@Singleton
class AudioPlaybackManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AlarmDatabase
) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    // Single mutex protects every transition on mediaPlayer / audio focus so a
    // play() that overlaps a stop() can't leak a player or leave focus stuck.
    private val playbackLock = Mutex()

    @Volatile
    private var mediaPlayer: MediaPlayer? = null

    @Volatile
    private var audioFocusRequest: AudioFocusRequest? = null

    @Volatile
    private var hasAudioFocus = false

    private var currentVolume = 0.5f

    private val bundledSounds = listOf(
        Sound("default", "Default Alarm", RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)),
        Sound("notification", "Notification", RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)),
        Sound("ringtone", "Ringtone", RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE))
    )

    private val customSounds = mutableListOf<Sound>()

    init {
        runBlocking {
            val customSoundEntities = database.customSoundDao().getAll()
            customSounds.addAll(customSoundEntities.map { entity ->
                val soundFile = File(entity.uri)
                Sound(
                    id = entity.id,
                    name = entity.name,
                    uri = Uri.fromFile(soundFile),
                    isCustom = true
                )
            })
        }
    }

    fun getAvailableSounds(): List<Sound> = bundledSounds + customSounds

    fun addCustomSound(name: String, uri: Uri): Sound {
        val soundId = "custom_${System.currentTimeMillis()}"
        val soundFile = File(context.filesDir, "$soundId.m4a")

        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                soundFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw RuntimeException("Failed to save custom sound", e)
        }

        val sound = Sound(
            id = soundId,
            name = name,
            uri = Uri.fromFile(soundFile),
            isCustom = true
        )
        customSounds.add(sound)

        runBlocking {
            database.customSoundDao().insert(
                CustomSoundEntity(
                    id = sound.id,
                    name = sound.name,
                    uri = soundFile.absolutePath
                )
            )
        }
        return sound
    }

    fun removeCustomSound(soundId: String) {
        customSounds.removeAll { it.id == soundId }
        runBlocking {
            database.customSoundDao().deleteById(soundId)
            val soundFile = File(context.filesDir, "$soundId.m4a")
            if (soundFile.exists()) {
                soundFile.delete()
            }
        }
    }

    suspend fun play(soundId: String, loop: Boolean = true) = withContext(Dispatchers.Default) {
        playbackLock.withLock {
            releaseLocked()

            val sound = getAvailableSounds().find { it.id == soundId }
                ?: bundledSounds.first()

            val streamType = if (loop) AudioManager.STREAM_ALARM else AudioManager.STREAM_MUSIC
            val usage = if (loop) AudioAttributes.USAGE_ALARM else AudioAttributes.USAGE_MEDIA

            requestAudioFocusLocked(usage)

            val attributes = AudioAttributes.Builder()
                .setUsage(usage)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val player = MediaPlayer()
            try {
                player.setAudioAttributes(attributes)
                @Suppress("DEPRECATION")
                player.setAudioStreamType(streamType)
                player.setDataSource(context, sound.uri)
                player.setVolume(currentVolume, currentVolume)
                player.isLooping = loop
                player.setOnErrorListener { _, what, extra ->
                    Log.e("AudioPlaybackManager", "MediaPlayer error what=$what extra=$extra")
                    true
                }
                player.prepare()
                player.start()
                mediaPlayer = player
                Log.d("AudioPlaybackManager", "Playing sound $soundId loop=$loop")
            } catch (e: Exception) {
                Log.e("AudioPlaybackManager", "Failed to start sound $soundId", e)
                try { player.release() } catch (_: Throwable) {}
                mediaPlayer = null
                abandonAudioFocusLocked()
            }
        }
    }

    suspend fun preview(soundId: String) {
        play(soundId, loop = false)
    }

    suspend fun stop() = withContext(Dispatchers.Default) {
        playbackLock.withLock {
            releaseLocked()
            abandonAudioFocusLocked()
        }
    }

    /** Synchronous stop for service.onDestroy and other contexts where we cannot suspend. */
    fun stopBlocking() {
        runBlocking { stop() }
    }

    private fun releaseLocked() {
        val player = mediaPlayer ?: return
        mediaPlayer = null
        try {
            if (player.isPlaying) player.stop()
        } catch (e: Throwable) {
            Log.w("AudioPlaybackManager", "stop() threw", e)
        }
        try {
            player.reset()
        } catch (e: Throwable) {
            Log.w("AudioPlaybackManager", "reset() threw", e)
        }
        try {
            player.release()
        } catch (e: Throwable) {
            Log.w("AudioPlaybackManager", "release() threw", e)
        }
    }

    private fun requestAudioFocusLocked(usage: Int) {
        if (hasAudioFocus) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attributes = AudioAttributes.Builder()
                .setUsage(usage)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(attributes)
                .setOnAudioFocusChangeListener { /* alarm keeps playing through focus changes */ }
                .build()
            audioFocusRequest = request
            val result = audioManager.requestAudioFocus(request)
            hasAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            val result = audioManager.requestAudioFocus(
                null,
                AudioManager.STREAM_ALARM,
                AudioManager.AUDIOFOCUS_GAIN
            )
            hasAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun abandonAudioFocusLocked() {
        if (!hasAudioFocus) {
            audioFocusRequest = null
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
            audioFocusRequest = null
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
        hasAudioFocus = false
    }

    suspend fun setVolume(volume: Float) = withContext(Dispatchers.Default) {
        playbackLock.withLock {
            currentVolume = volume.coerceIn(0f, 1f)
            try {
                mediaPlayer?.setVolume(currentVolume, currentVolume)
            } catch (e: Throwable) {
                Log.w("AudioPlaybackManager", "setVolume threw", e)
            }
        }
    }

    suspend fun rampVolume(durationMs: Long, targetVolume: Float = 1f) = withContext(Dispatchers.Default) {
        val steps = 20
        val stepDuration = durationMs / steps
        val volumeIncrement = (targetVolume - currentVolume) / steps

        repeat(steps) {
            // Bail out if playback has been stopped.
            if (mediaPlayer == null) return@withContext
            setVolume(currentVolume + volumeIncrement)
            Thread.sleep(stepDuration)
        }
    }

    suspend fun isPlaying(): Boolean = withContext(Dispatchers.Default) {
        try {
            mediaPlayer?.isPlaying ?: false
        } catch (e: Throwable) {
            false
        }
    }
}
