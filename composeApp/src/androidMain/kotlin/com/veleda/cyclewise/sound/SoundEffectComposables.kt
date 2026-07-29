package com.veleda.cyclewise.sound

import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.drop

/**
 * App-wide access point for UI sound effects, provided at the root of
 * `CycleWiseAppUI`. Defaults to [NoOpSoundEffectPlayer] so previews and
 * Robolectric tests stay silent without extra setup.
 */
val LocalSoundEffects: ProvidableCompositionLocal<SoundEffectPlayer> =
    staticCompositionLocalOf { NoOpSoundEffectPlayer }

/**
 * Plays [effect] each time [pagerState] crosses onto a new page — covering both user
 * swipes and programmatic `animateScrollToPage` calls (tab clicks), which is why
 * tab click handlers driving a pager must NOT also play a tap sound.
 *
 * Keyed on [PagerState.currentPage], which flips mid-gesture as soon as the new page
 * becomes the closest snap target — waiting for `settledPage` made the sound land
 * noticeably after the swipe. The initial composition is skipped.
 */
@Composable
fun PagerSoundEffect(pagerState: PagerState, effect: SoundEffect = SoundEffect.SWIPE) {
    val sounds = LocalSoundEffects.current
    LaunchedEffect(pagerState, sounds) {
        snapshotFlow { pagerState.currentPage }
            .drop(1)
            .collect { sounds.play(effect) }
    }
}
