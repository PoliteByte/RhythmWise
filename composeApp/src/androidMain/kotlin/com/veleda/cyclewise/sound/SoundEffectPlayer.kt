package com.veleda.cyclewise.sound

/**
 * Plays short UI feedback sounds. The single app-wide implementation is
 * [SoundPoolSoundEffectPlayer]; composables reach it through [LocalSoundEffects].
 */
interface SoundEffectPlayer {

    /**
     * Plays [effect] at the user's configured sound-effects volume.
     * No-op when the sound-effects setting is disabled or volume is zero.
     */
    fun play(effect: SoundEffect)

    /**
     * Plays [effect] at an explicit [volumePercent] (0–100), ignoring the persisted
     * volume. Used by the settings volume slider so the user hears the level they
     * just chose before the async DataStore write lands.
     */
    fun preview(effect: SoundEffect, volumePercent: Int)
}

/** Silent implementation used as the [LocalSoundEffects] default (previews, tests). */
object NoOpSoundEffectPlayer : SoundEffectPlayer {
    override fun play(effect: SoundEffect) = Unit
    override fun preview(effect: SoundEffect, volumePercent: Int) = Unit
}

/**
 * Plays [effect] only when [new] differs from [current] — the standard guard for
 * detent ticks on stepped sliders and SELECT on exclusive choices, so drag samples
 * within one step and re-taps of the active option stay silent.
 */
fun SoundEffectPlayer.playOnChange(effect: SoundEffect, current: Int, new: Int) {
    if (new != current) play(effect)
}
