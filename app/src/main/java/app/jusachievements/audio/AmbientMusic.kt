package app.jusachievements.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.concurrent.thread
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sin

/**
 * Background music with no audio files: a soft four-chord synth pad with a sparse, echoing
 * arpeggio, synthesised once at startup into a seamless loop and played quietly.
 */
class AmbientMusic {
    private var track: AudioTrack? = null
    private var building = false
    private var wantPlaying = false

    @Synchronized
    fun setPlaying(play: Boolean) {
        wantPlaying = play
        val t = track
        when {
            t != null -> runCatching { if (play) t.play() else t.pause() }
            play && !building -> {
                building = true
                thread(name = "ja-music", isDaemon = true) {
                    val built = runCatching { buildTrack() }.getOrNull()
                    synchronized(this) {
                        building = false
                        track = built
                        if (wantPlaying) runCatching { built?.play() }
                    }
                }
            }
        }
    }

    private fun buildTrack(): AudioTrack {
        val pcm = synthesise()
        val t = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(pcm.size * 2)
            .build()
        t.write(pcm, 0, pcm.size)
        t.setLoopPoints(0, pcm.size, -1)
        t.setVolume(VOLUME)
        return t
    }

    private fun synthesise(): ShortArray {
        val chordLen = (CHORD_SECONDS * SAMPLE_RATE).toInt()
        val fade = (FADE_SECONDS * SAMPLE_RATE).toInt()
        val total = chordLen * CHORDS.size
        val mix = FloatArray(total)
        val twoPi = 2.0 * PI

        // Pad: each chord fades in, holds, and fades out over the start of the next one. The
        // write index wraps, so the last chord's tail lands on the first chord: a seamless loop.
        CHORDS.forEachIndexed { c, notes ->
            val start = c * chordLen
            for (s in 0 until chordLen + fade) {
                val env = when {
                    s < fade -> s.toFloat() / fade
                    s >= chordLen -> 1f - (s - chordLen).toFloat() / fade
                    else -> 1f
                }
                val t = s.toDouble() / SAMPLE_RATE
                var v = 0.0
                for (f in notes) {
                    v += sin(twoPi * f * t) + 0.5 * sin(twoPi * f * 1.004 * t) + 0.2 * sin(twoPi * f * 2 * t)
                }
                val tremolo = 0.85 + 0.15 * sin(twoPi * 0.2 * t)
                mix[(start + s) % total] += (v * tremolo * env / notes.size).toFloat() * 0.5f
            }
        }

        // Arpeggio: one soft pluck per beat from the current chord, two octaves up, with an echo.
        val beat = (BEAT_SECONDS * SAMPLE_RATE).toInt()
        val pluckLen = (1.2 * SAMPLE_RATE).toInt()
        val echo = beat / 2
        val pattern = intArrayOf(0, 2, 1, 3, 2, 1, 3, 0)
        var step = 0
        var pos = 0
        while (pos < total) {
            val notes = CHORDS[(pos / chordLen) % CHORDS.size]
            val f = notes[pattern[step % pattern.size] % notes.size] * 4
            for (s in 0 until pluckLen) {
                val t = s.toDouble() / SAMPLE_RATE
                val v = (sin(twoPi * f * t) * exp(-t * 4.5)).toFloat() * 0.16f
                mix[(pos + s) % total] += v
                mix[(pos + s + echo) % total] += v * 0.45f
                mix[(pos + s + echo * 2) % total] += v * 0.2f
            }
            pos += beat
            step++
        }

        val peak = mix.maxOf { abs(it) }.coerceAtLeast(1e-6f)
        val gain = 0.8f / peak * Short.MAX_VALUE
        return ShortArray(total) { (mix[it] * gain).toInt().toShort() }
    }

    private companion object {
        const val SAMPLE_RATE = 22_050
        const val CHORD_SECONDS = 6.0
        const val FADE_SECONDS = 2.0
        const val BEAT_SECONDS = 0.75
        const val VOLUME = 0.3f

        // Am7 – Fmaj7 – Cmaj7 – G6
        val CHORDS = arrayOf(
            doubleArrayOf(110.00, 164.81, 196.00, 261.63),
            doubleArrayOf(87.31, 130.81, 164.81, 220.00),
            doubleArrayOf(130.81, 196.00, 246.94, 329.63),
            doubleArrayOf(98.00, 146.83, 246.94, 329.63),
        )
    }
}
