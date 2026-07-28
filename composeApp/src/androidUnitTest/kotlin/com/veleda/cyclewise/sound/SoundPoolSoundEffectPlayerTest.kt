package com.veleda.cyclewise.sound

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Unit tests for the [soundVolumeGain] perceptual volume curve.
 *
 * The [SoundPoolSoundEffectPlayer] itself wraps Android's SoundPool, whose playback
 * cannot be observed under Robolectric; its enabled/volume gating funnels through
 * [soundVolumeGain], which is covered here.
 */
class SoundPoolSoundEffectPlayerTest {

    private val floatTolerance = 0.0001f

    @Test
    fun soundVolumeGain_WHEN_zeroPercent_THEN_silent() {
        assertEquals(0f, soundVolumeGain(0), floatTolerance)
    }

    @Test
    fun soundVolumeGain_WHEN_fullVolume_THEN_unityGain() {
        assertEquals(1f, soundVolumeGain(100), floatTolerance)
    }

    @Test
    fun soundVolumeGain_WHEN_defaultVolume_THEN_perceptualCurveApplied() {
        // GIVEN the 80% default — THEN the x² curve yields 0.64 linear gain,
        // the level the raw assets are authored against.
        assertEquals(0.64f, soundVolumeGain(80), floatTolerance)
    }

    @Test
    fun soundVolumeGain_WHEN_halfVolume_THEN_quarterGain() {
        assertEquals(0.25f, soundVolumeGain(50), floatTolerance)
    }

    @Test
    fun soundVolumeGain_WHEN_outOfRange_THEN_coercedToBounds() {
        assertEquals(0f, soundVolumeGain(-10), floatTolerance)
        assertEquals(1f, soundVolumeGain(140), floatTolerance)
    }

    @Test
    fun playOnChange_WHEN_valueDiffers_THEN_playsEffect() {
        // GIVEN a recording player
        val played = mutableListOf<SoundEffect>()
        val player = object : SoundEffectPlayer {
            override fun play(effect: SoundEffect) {
                played.add(effect)
            }

            override fun preview(effect: SoundEffect, volumePercent: Int) = Unit
        }

        // WHEN the new value crosses into a different step
        player.playOnChange(SoundEffect.TICK, current = 3, new = 4)

        // THEN the effect plays once
        assertEquals(listOf(SoundEffect.TICK), played)
    }

    @Test
    fun playOnChange_WHEN_valueUnchanged_THEN_staysSilent() {
        // GIVEN a recording player
        val played = mutableListOf<SoundEffect>()
        val player = object : SoundEffectPlayer {
            override fun play(effect: SoundEffect) {
                played.add(effect)
            }

            override fun preview(effect: SoundEffect, volumePercent: Int) = Unit
        }

        // WHEN a drag sample stays within the current step
        player.playOnChange(SoundEffect.TICK, current = 3, new = 3)

        // THEN nothing plays
        assertEquals(emptyList<SoundEffect>(), played)
    }
}
