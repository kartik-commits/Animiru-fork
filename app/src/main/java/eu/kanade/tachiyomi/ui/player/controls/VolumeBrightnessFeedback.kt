package eu.kanade.tachiyomi.ui.player.controls

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import eu.kanade.tachiyomi.ui.player.PlayerViewModel
import eu.kanade.tachiyomi.ui.player.controls.components.BrightnessSlider
import eu.kanade.tachiyomi.ui.player.controls.components.VolumeSlider
import kotlinx.coroutines.delay
import tachiyomi.presentation.core.components.material.padding
import kotlin.time.Duration.Companion.seconds

@Composable
fun BoxScope.VolumeBrightnessFeedback(
    stateData: PlayerViewModel.PlayerStateData,
    uiData: PlayerViewModel.PlayerUiData,
    playbackData: PlayerViewModel.PlayerPlaybackData,
    mpvVolume: Int? = null,
    onVolumeVisibilityChange: (Boolean) -> Unit,
    onBrightnessVisibilityChange: (Boolean) -> Unit,
) {
    LaunchedEffect(playbackData.currentVolume, mpvVolume, uiData.isVolumeSliderShown) {
        delay(2.seconds)
        if (uiData.isVolumeSliderShown) onVolumeVisibilityChange(false)
    }
    LaunchedEffect(playbackData.currentBrightness, uiData.isBrightnessSliderShown) {
        delay(2.seconds)
        if (uiData.isBrightnessSliderShown) onBrightnessVisibilityChange(false)
    }

    AnimatedVisibility(
        visible = uiData.isBrightnessSliderShown,
        enter = if (uiData.reduceMotion) {
            fadeIn(playerControlsEnterAnimationSpec())
        } else {
            slideInHorizontally(playerControlsEnterAnimationSpec()) {
                if (uiData.swapVolumeAndBrightness) it else -it
            } + fadeIn(playerControlsEnterAnimationSpec())
        },
        exit = if (uiData.reduceMotion) {
            fadeOut(playerControlsExitAnimationSpec())
        } else {
            slideOutHorizontally(playerControlsExitAnimationSpec()) {
                if (uiData.swapVolumeAndBrightness) it else -it
            } + fadeOut(playerControlsExitAnimationSpec())
        },
        modifier = Modifier
            .align(if (uiData.swapVolumeAndBrightness) Alignment.CenterEnd else Alignment.CenterStart)
            .padding(horizontal = MaterialTheme.padding.medium),
    ) {
        BrightnessSlider(
            brightness = playbackData.currentBrightness,
            positiveRange = 0f..1f,
            negativeRange = 0f..0.75f,
        )
    }

    AnimatedVisibility(
        visible = uiData.isVolumeSliderShown,
        enter = if (uiData.reduceMotion) {
            fadeIn(playerControlsEnterAnimationSpec())
        } else {
            slideInHorizontally(playerControlsEnterAnimationSpec()) {
                if (uiData.swapVolumeAndBrightness) -it else it
            } + fadeIn(playerControlsEnterAnimationSpec())
        },
        exit = if (uiData.reduceMotion) {
            fadeOut(playerControlsExitAnimationSpec())
        } else {
            slideOutHorizontally(playerControlsExitAnimationSpec()) {
                if (uiData.swapVolumeAndBrightness) -it else it
            } + fadeOut(playerControlsExitAnimationSpec())
        },
        modifier = Modifier
            .align(if (uiData.swapVolumeAndBrightness) Alignment.CenterStart else Alignment.CenterEnd)
            .padding(horizontal = MaterialTheme.padding.medium),
    ) {
        VolumeSlider(
            volume = playbackData.currentVolume,
            mpvVolume = mpvVolume ?: 100,
            range = 0..stateData.maxVolume,
            boostRange = mpvVolume?.let { if (uiData.boostCap > 0) 0..uiData.boostCap else null },
            displayAsPercentage = uiData.displayVolumeAsPercentage,
        )
    }
}
