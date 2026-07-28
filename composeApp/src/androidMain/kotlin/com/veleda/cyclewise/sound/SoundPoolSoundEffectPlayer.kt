package com.veleda.cyclewise.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.veleda.cyclewise.settings.AppSettings
import com.veleda.cyclewise.settings.SOUND_VOLUME_DEFAULT_PERCENT
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/** Concurrent streams; enough for a drag tick landing on top of a whoosh + outcome. */
private const val MAX_STREAMS = 4
private const val PRIORITY_DEFAULT = 1
private const val NO_LOOP = 0
private const val RATE_NORMAL = 1f
private const val MAX_VOLUME_PERCENT = 100

/**
 * [SoundPool]-backed player for the app's UI sounds. Singleton-scoped: all effects are
 * preloaded once at construction and the pool lives for the app process.
 *
 * Audio is routed as [AudioAttributes.USAGE_ASSISTANCE_SONIFICATION] so effects follow
 * the system sound volume stream (like keyboard clicks), never the media stream.
 *
 * The enabled flag and volume are mirrored from [AppSettings] into fields on [scope],
 * keeping [play] synchronous and safe to call from composition callbacks. A [play]
 * call before an effect finishes loading is silently dropped — by first user
 * interaction loading has long completed.
 */
class SoundPoolSoundEffectPlayer(
    context: Context,
    appSettings: AppSettings,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) : SoundEffectPlayer {

    private val soundPool = SoundPool.Builder()
        .setMaxStreams(MAX_STREAMS)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private val loadedSampleIds = ConcurrentHashMap.newKeySet<Int>()
    private val sampleIds: Map<SoundEffect, Int>

    // Matches the AppSettings default (off) so no sound sneaks out in the frames
    // before the first DataStore emission lands.
    @Volatile
    private var enabled = false

    @Volatile
    private var volumePercent = SOUND_VOLUME_DEFAULT_PERCENT

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) loadedSampleIds += sampleId
        }
        sampleIds = SoundEffect.entries.associateWith { soundPool.load(context, it.resId, PRIORITY_DEFAULT) }
        appSettings.soundEffectsEnabled.onEach { enabled = it }.launchIn(scope)
        appSettings.soundEffectsVolume.onEach { volumePercent = it }.launchIn(scope)
    }

    override fun play(effect: SoundEffect) {
        if (!enabled) return
        playAt(effect, volumePercent)
    }

    override fun preview(effect: SoundEffect, volumePercent: Int) {
        playAt(effect, volumePercent)
    }

    private fun playAt(effect: SoundEffect, volumePercent: Int) {
        val sampleId = sampleIds[effect] ?: return
        if (sampleId !in loadedSampleIds) return
        val gain = soundVolumeGain(volumePercent)
        if (gain <= 0f) return
        soundPool.play(sampleId, gain, gain, PRIORITY_DEFAULT, NO_LOOP, RATE_NORMAL)
    }
}

/**
 * Maps the 0–100 volume setting to a linear [SoundPool] gain with a perceptual x²
 * curve, so slider movement feels like an even loudness change across the range.
 * The default 80% therefore plays at 0.64 gain — the level the assets are tuned for.
 */
fun soundVolumeGain(volumePercent: Int): Float {
    val fraction = volumePercent.coerceIn(0, MAX_VOLUME_PERCENT) / MAX_VOLUME_PERCENT.toFloat()
    return fraction * fraction
}
