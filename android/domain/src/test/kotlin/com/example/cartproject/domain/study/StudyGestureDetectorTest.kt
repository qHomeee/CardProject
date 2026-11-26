package com.example.cartproject.domain.study

import com.example.cartproject.domain.study.StudyGestureSignal.Action
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StudyGestureDetectorTest {
    private val sensor = StudyGestureDetector()
    private var time = 0L
    private var mode = StudyGestureMode.Reveal

    @Test
    fun revealRequiresACompleteFlip() {
        assertNull(hold(z = -9.8f))
        assertNull(hold())
        assertNull(hold(z = -9.8f))
        assertNull(hold(z = -9.8f))
        assertEquals(Action.Reveal, hold())
        assertNull(hold())
    }

    @Test
    fun heldTiltGradesOnlyOnceUntilNeutral() {
        mode = StudyGestureMode.Grade
        assertNull(hold(x = -6f, z = 7.8f))
        assertNull(hold())
        assertEquals(Action.Remembered, hold(x = -6f, z = 7.8f))
        assertNull(hold(x = -6f, z = 7.8f))
        assertNull(hold(x = 6f, z = 7.8f))
        assertNull(hold())
        assertEquals(Action.NeedsReview, hold(x = 6f, z = 7.8f))
    }

    @Test
    fun rotationDiscardsTheFirstHalfOfAFlip() {
        hold()
        hold(z = -9.8f)
        sensor.reset()
        assertNull(hold())
        assertNull(hold(z = -9.8f))
        assertEquals(Action.Reveal, hold())
    }

    @Test
    fun unstableTiltAndShakingDoNotCount() {
        mode = StudyGestureMode.Grade
        hold()
        assertNull(sensor.accept(StudyPoseSample(-6f, 0f, 7.8f, time), mode))
        time += 50
        assertNull(hold(x = -17f, z = 19f))
        assertNull(hold())
        assertNull(hold(x = -3.8f, z = 9f))
        assertEquals(Action.Remembered, hold(x = -6f, z = 7.8f))
    }

    @Test
    fun isolatedTiltDoesNotUseTimeBetweenPacketsAsAHold() {
        mode = StudyGestureMode.Grade
        hold()
        assertNull(sensor.accept(StudyPoseSample(-6f, 0f, 7.8f, time), mode))
        time += 500
        assertNull(hold(x = -6f, z = 7.8f))
        assertNull(hold())
        assertEquals(Action.Remembered, hold(x = -6f, z = 7.8f))
    }

    @Test
    fun streamGapDiscardsUnfinishedFlip() {
        hold()
        hold(z = -9.8f)
        time += 500
        assertNull(hold())
        assertNull(hold(z = -9.8f))
        assertEquals(Action.Reveal, hold())
    }

    @Test
    fun flipThroughTheSideAngleDoesNotDisarmReveal() {
        hold()
        assertNull(hold(x = -6f, z = 7.8f))
        assertNull(hold(z = -9.8f))
        assertNull(hold(x = 6f, z = 7.8f))
        assertEquals(Action.Reveal, hold())
    }

    @Test
    fun switchingToGradeRequiresANewNeutral() {
        hold()
        mode = StudyGestureMode.Grade
        assertNull(hold(x = 6f, z = 7.8f))
        assertNull(hold())
        assertEquals(Action.NeedsReview, hold(x = 6f, z = 7.8f))
    }

    @Test
    fun uprightNeutralAllowsBothGrades() {
        mode = StudyGestureMode.Grade
        assertNull(hold(y = 9.8f, z = 0f))
        assertEquals(Action.Remembered, hold(x = -6f, y = 7.8f, z = 0f))
        assertNull(hold(x = -6f, y = 7.8f, z = 0f))
        assertNull(hold(y = 9.8f, z = 0f))
        assertEquals(Action.NeedsReview, hold(x = 6f, y = 7.8f, z = 0f))
    }

    private fun hold(x: Float = 0f, y: Float = 0f, z: Float = 9.8f): Action? {
        sensor.accept(StudyPoseSample(x, y, z, time), mode)
        time += 220
        return sensor.accept(StudyPoseSample(x, y, z, time++), mode)
    }
}
