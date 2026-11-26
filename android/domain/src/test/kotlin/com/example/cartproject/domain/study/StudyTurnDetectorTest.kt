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

    private fun hold(direction: StudyScreenDirection): Boolean {
        detector.accept(direction, clock)
        clock += 210
        return detector.accept(direction, clock++)
    }
}
