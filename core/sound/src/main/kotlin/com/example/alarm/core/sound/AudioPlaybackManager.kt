package com.example.alarm.core.sound

import android.content.Context
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import com.example.alarm.data.db.AlarmDatabase
import com.example.alarm.data.db.entity.CustomSoundEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
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
    private var mediaPlayer: MediaPlayer? = null
    private var currentVolume = 0.5f

    // Available bundled sounds
    private val bundledSounds = listOf(
        Sound("default", "Default Alarm", RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)),
        Sound("notification", "Notification", RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)),
        Sound("ringtone", "Ringtone", RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE))
    )

    // Custom sounds stored by user (loaded from DB on init)
    private val customSounds = mutableListOf<Sound>()

    init {
        // Load custom sounds from database on initialization
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

        // Copy audio file from content Uri to app private storage
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

        // Persist to database (store file path, not Uri)
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
        // Remove from database
        runBlocking {
            database.customSoundDao().deleteById(soundId)
            // Delete the actual file
            val soundFile = File(context.filesDir, "$soundId.m4a")
            if (soundFile.exists()) {
                soundFile.delete()
            }
        }
    }

    suspend fun play(soundId: String, loop: Boolean = true) = withContext(Dispatchers.Default) {
        stop()

        val sound = getAvailableSounds().find { it.id == soundId }
            ?: bundledSounds.first()

        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(context, sound.uri)
                setVolume(currentVolume, currentVolume)
                isLooping = loop
                prepare()
                start()
            } catch (e: Exception) {
                e.printStackTrace()
                release()
            }
        }
    }

    suspend fun preview(soundId: String) = withContext(Dispatchers.Default) {
        play(soundId, loop = false)
    }

    suspend fun stop() = withContext(Dispatchers.Default) {
        mediaPlayer?.apply {
            try {
                if (isPlaying) stop()
                release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        mediaPlayer = null
    }

    suspend fun setVolume(volume: Float) = withContext(Dispatchers.Default) {
        currentVolume = volume.coerceIn(0f, 1f)
        mediaPlayer?.setVolume(currentVolume, currentVolume)
    }

    suspend fun rampVolume(durationMs: Long, targetVolume: Float = 1f) = withContext(Dispatchers.Default) {
        val steps = 20
        val stepDuration = durationMs / steps
        val volumeIncrement = (targetVolume - currentVolume) / steps

        repeat(steps) {
            setVolume(currentVolume + (volumeIncrement * (it + 1)))
            Thread.sleep(stepDuration)
        }
    }

    suspend fun isPlaying(): Boolean = withContext(Dispatchers.Default) {
        mediaPlayer?.isPlaying ?: false
    }
}

