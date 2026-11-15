package app.jusachievements.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri

/**
 * Background music: either the built-in synthesised loop, or any audio file the user picked.
 * If the custom file can't be played (moved, deleted), it falls back to the built-in loop.
 */
class MusicController(private val context: Context) {
    private val builtIn = AmbientMusic()
    private var player: MediaPlayer? = null
    private var playerUri: String? = null

    @Synchronized
    fun update(play: Boolean, customUri: String?) {
        if (customUri != playerUri) {
            runCatching { player?.release() }
            player = null
            playerUri = null
        }
        if (customUri != null && player == null && play) {
            player = runCatching {
                MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_GAME)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    setDataSource(context, Uri.parse(customUri))
                    isLooping = true
                    setVolume(CUSTOM_VOLUME, CUSTOM_VOLUME)
                    prepare()
                }
            }.getOrNull()
            playerUri = customUri
        }

        val custom = player
        if (custom != null) {
            builtIn.setPlaying(false)
            runCatching { if (play) custom.start() else if (custom.isPlaying) custom.pause() }
        } else {
            builtIn.setPlaying(play)
        }
    }

    private companion object {
        const val CUSTOM_VOLUME = 0.45f
    }
}
