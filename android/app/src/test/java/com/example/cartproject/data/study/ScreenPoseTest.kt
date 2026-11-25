package com.example.cartproject.data.study

import android.view.Surface
import com.example.cartproject.domain.study.StudyPoseSample
import org.junit.Assert.assertEquals
import org.junit.Test

class ScreenPoseTest {
    @Test
    fun screenRightRetainsItsSignForEveryDisplayRotation() {
        val expected = StudyPoseSample(-6f, 0f, 7.8f, 10)
        assertEquals(expected, screenPose(floatArrayOf(-6f, 0f, 7.8f), Surface.ROTATION_0, 10))
        assertEquals(expected, screenPose(floatArrayOf(0f, 6f, 7.8f), Surface.ROTATION_90, 10))
        assertEquals(
            -6f,
            screenPose(floatArrayOf(6f, 0f, 7.8f), Surface.ROTATION_180, 10).horizontal,
        )
        assertEquals(
            -6f,
            screenPose(floatArrayOf(0f, -6f, 7.8f), Surface.ROTATION_270, 10).horizontal,
        )
    }
}
