package com.example.cartproject.domain.study

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StudyTurnDetectorTest {
    private val detector = StudyTurnDetector()
    private var clock = 0L
    private val toward = StudyScreenDirection(0f, 1f, 0f)
    private val away = StudyScreenDirection(0f, -1f, 0f)
    private val side = StudyScreenDirection(1f, 0f, 0f)

    @Test
    fun uprightTurnRevealsOnlyAfterTheScreenReturns() {
        assertFalse(hold(toward))
        assertFalse(hold(side))
        assertFalse(hold(away))
        assertFalse(hold(away))
        assertTrue(hold(toward))
        assertFalse(hold(toward))
    }

    @Test
    fun changesOfDirectionBelowAHalfTurnDoNotReveal() {
        hold(toward)
        assertFalse(hold(side))
        assertFalse(hold(toward))
    }

    @Test
    fun interruptionDiscardsAHalfTurn() {
        hold(toward)
        hold(away)
        clock += 500
        assertFalse(hold(toward))
        assertFalse(hold(away))
        assertTrue(hold(toward))
    }

    @Test
    fun puttingThePhoneOnATableDoesNotSetAnUprightReference() {
        assertFalse(hold(StudyScreenDirection(0f, 0f, 1f)))
        assertFalse(hold(toward))
        assertFalse(hold(away))
        assertTrue(hold(toward))
    }

    @Test
    fun revealsAfterSixtyMillisecondsOnReturnFromAConfirmedTurn() {
        hold(toward)
        hold(away)
        assertFalse(detector.accept(toward, clock))
        assertFalse(detector.accept(toward, clock + 59))
        assertTrue(detector.accept(toward, clock + 60))
    }

    @Test
    fun halfTurnWorksAtTheSameComfortablePitch() {
        val towardAtPitch = StudyScreenDirection(0f, 0.866f, 0.50f)
        val awayAtPitch = StudyScreenDirection(0f, -0.80f, 0.60f)
        assertFalse(hold(towardAtPitch))
        assertFalse(hold(awayAtPitch))
        assertTrue(hold(towardAtPitch))
    }

    @Test
    fun changingPitchWhileFacingTheSameDirectionDoesNotReveal() {
        hold(toward)
        assertFalse(hold(StudyScreenDirection(0f, 0.80f, 0.60f)))
        assertFalse(hold(StudyScreenDirection(0f, 0.94f, -0.34f)))
        assertFalse(hold(toward))
    }

    private fun hold(direction: StudyScreenDirection): Boolean {
        detector.accept(direction, clock)
        clock += 210
        return detector.accept(direction, clock++)
    }
}
