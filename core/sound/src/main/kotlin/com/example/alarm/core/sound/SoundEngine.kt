package com.example.alarm.core.sound

import android.content.Context
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SoundEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private var mediaPlayer: MediaPlayer? = null
    private var currentVolume = 0.5f

    suspend fun play(soundId: String) = withContext(Dispatchers.Default) {
        stop()

        val soundUri = getSoundUri(soundId)
        mediaPlayer = MediaPlayer().apply {
            setDataSource(context, soundUri)
            setVolume(currentVolume, currentVolume)
            isLooping = true
            prepare()
            start()
        }
    }

    suspend fun stop() = withContext(Dispatchers.Default) {
        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
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

    private fun getSoundUri(soundId: String): Uri {
        return when (soundId) {
            "default" -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            else -> RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        }
    }
}
