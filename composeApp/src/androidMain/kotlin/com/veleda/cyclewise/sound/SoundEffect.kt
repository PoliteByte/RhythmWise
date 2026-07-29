package com.veleda.cyclewise.sound

import androidx.annotation.RawRes
import com.veleda.cyclewise.R

/**
 * The app's UI sound vocabulary, mapping each interaction category to its raw asset.
 *
 * The set is one tonal family (D-pentatonic blips, pitchless noise whooshes) generated
 * by `tools/generate_ui_sounds.py` — re-run that script to re-tune, never edit the
 * WAV files by hand. Pick the effect by *meaning*, not by sound: stateful actions are
 * directional (on rises, off falls), transitions are whooshes, outcomes are melodic.
 */
enum class SoundEffect(@RawRes val resId: Int) {
    /** Neutral press on a button, nav tab, or other momentary control. */
    TAP(R.raw.snd_tap),

    /** Quieter tap for low-emphasis or high-frequency presses (coach marks, steppers). */
    TAP_LIGHT(R.raw.snd_tap_light),

    /** Detent click for discrete snaps: drag crossing a day, a stepped slider notch. */
    TICK(R.raw.snd_tick),

    /** Switch or chip turning on. */
    TOGGLE_ON(R.raw.snd_toggle_on),

    /** Switch or chip turning off. */
    TOGGLE_OFF(R.raw.snd_toggle_off),

    /** Choosing a value in a selector row (mood face, score, flow intensity). */
    SELECT(R.raw.snd_select),

    /** Clearing a previously selected value. */
    DESELECT(R.raw.snd_deselect),

    /** Horizontal page change — pager swipe or tab-driven page animation. */
    SWIPE(R.raw.snd_swipe),

    /** Bottom sheet or overlay sliding open. */
    OPEN(R.raw.snd_open),

    /** Bottom sheet or overlay dismissing. */
    CLOSE(R.raw.snd_close),

    /** Positive outcome: period saved, setup complete, export finished. */
    SUCCESS(R.raw.snd_success),

    /** Negative outcome: failed unlock, save error. */
    ERROR(R.raw.snd_error),
}
