package eu.kanade.tachiyomi.ui.player.controls

import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import kotlin.math.abs

/** Adapts Animiru's volume/brightness vertical gesture to an Android View surface. */
class VolumeBrightnessTouchListener(
    private val touchSlop: Float,
    private val enabled: () -> Boolean,
    private val swapSides: () -> Boolean,
    private val currentVolume: () -> Int,
    private val maxVolume: () -> Int,
    private val currentBrightness: () -> Float,
    private val systemBrightness: () -> Float,
    private val onVolumeChanged: (Int) -> Unit,
    private val onBrightnessChanged: (Float) -> Unit,
) : View.OnTouchListener {
    private var startX = 0f
    private var startY = 0f
    private var startVolume = 0
    private var startBrightness = 0f
    private var verticalGesture = false

    override fun onTouch(view: View, event: MotionEvent): Boolean {
        if (!enabled()) return false

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.x
                startY = event.y
                startVolume = currentVolume()
                startBrightness = currentBrightness()
                    .takeUnless { it == WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE }
                    ?: systemBrightness()
                verticalGesture = false
            }

            MotionEvent.ACTION_MOVE -> {
                val verticalDistance = startY - event.y
                val horizontalDistance = event.x - startX
                if (!verticalGesture) {
                    verticalGesture = abs(verticalDistance) > touchSlop &&
                        abs(verticalDistance) > abs(horizontalDistance)
                }
                if (!verticalGesture) return false

                val brightnessSide = if (swapSides()) startX >= view.width / 2f else startX < view.width / 2f
                if (brightnessSide) {
                    onBrightnessChanged(startBrightness + verticalDistance * BRIGHTNESS_SENSITIVITY)
                } else {
                    onVolumeChanged(
                        startVolume + (verticalDistance * VOLUME_SENSITIVITY * maxVolume()).toInt(),
                    )
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> verticalGesture = false
        }

        // PlayerView retains taps, its seek bar, and built-in controls.
        return false
    }

    private companion object {
        const val BRIGHTNESS_SENSITIVITY = 0.001f
        const val VOLUME_SENSITIVITY = 0.001f
    }
}
