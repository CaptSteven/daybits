package com.wasabi.daybits

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.MotionEvent
import android.view.View
import android.view.animation.DecelerateInterpolator

object ButtonMotion {
    private const val PRESSED_SCALE = 0.965f
    private const val NORMAL_SCALE = 1f
    private const val PRESS_DURATION_MS = 95L
    private const val RELEASE_DURATION_MS = 150L

    @JvmStatic
    fun install(view: View) {
        view.isClickable = true
        view.isFocusable = true
        view.elevation = view.resources.displayMetrics.density * 2f
        view.stateListAnimator = null
        view.setOnTouchListener { touched, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> animateScale(touched, PRESSED_SCALE, PRESS_DURATION_MS)
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> animateScale(touched, NORMAL_SCALE, RELEASE_DURATION_MS)
            }
            false
        }
    }

    private fun animateScale(view: View, scale: Float, duration: Long) {
        view.animate().cancel()
        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(view, View.SCALE_X, scale),
                ObjectAnimator.ofFloat(view, View.SCALE_Y, scale)
            )
            interpolator = DecelerateInterpolator()
            this.duration = duration
            start()
        }
    }
}
