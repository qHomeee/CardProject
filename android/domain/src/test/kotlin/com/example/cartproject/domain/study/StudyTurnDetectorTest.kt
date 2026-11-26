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
    private val faceUp = StudyScreenDirection(0f, 0f, 1f)

    @Test
    fun deliversOneRevealAfterTheScreenReturns() {
        assertFalse(hold(toward))
        assertFalse(hold(side))
        assertFalse(hold(away, duration = 650))
        assertTrue(hold(toward))
        assertFalse(hold(toward))
    }

    @Test
    fun turnsFromVerticalHorizontalAndInclinedStartingPositions() {
        val starts = listOf(
            toward,
            faceUp,
            StudyScreenDirection(0.62f, 0.48f, 0.62f),
            StudyScreenDirection(-0.34f, 0.83f, -0.44f),
        )
        for (start in starts) {
            assertFalse(hold(start))
            assertFalse(hold(StudyScreenDirection(-start.x, -start.y, -start.z)))
            assertTrue(hold(start))
        }
    }

    @Test
    fun adaptsToSteadyNewGripBeforeTheTurn() {
        assertFalse(hold(faceUp))
        assertFalse(hold(toward, duration = 550))
        assertFalse(hold(away))
        assertTrue(hold(toward))
    }

    @Test
    fun halfTurnWorksAtTheSameComfortablePitch() {
        val towardAtPitch = StudyScreenDirection(0f, 0.866f, 0.50f)
        val awayAtPitch = StudyScreenDirection(0f, -0.80f, 0.60f)
        assertFalse(hold(towardAtPitch))
        assertFalse(hold(awayAtPitch))
        assertTrue(hold(StudyScreenDirection(0f, 0.75f, 0.66f)))
    }

    @Test
    fun quarterTurnAndSmallChangesInPitchDoNotReveal() {
        hold(toward)
        assertFalse(hold(side))
        assertFalse(hold(StudyScreenDirection(0f, 0.80f, 0.60f)))
        assertFalse(hold(StudyScreenDirection(0f, 0.94f, -0.34f)))
        assertFalse(hold(toward))
    }

    @Test
    fun spinningAFaceUpPhoneDoesNotReveal() {
        val slightlyInclined = StudyScreenDirection(0.15f, 0f, 0.988f)
        hold(slightlyInclined)
        assertFalse(hold(StudyScreenDirection(-0.15f, 0f, 0.988f)))
        assertFalse(hold(slightlyInclined))
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
    fun revealsAfterSixtyMillisecondsOnReturnFromAConfirmedTurn() {
        hold(toward)
        hold(away)
        assertFalse(detector.accept(toward, clock))
        assertFalse(detector.accept(toward, clock + 59))
        assertTrue(detector.accept(toward, clock + 60))
    }

    private fun hold(direction: StudyScreenDirection, duration: Long = 210): Boolean {
        var recognized = detector.accept(direction, clock)
        var elapsed = 0L
        while (elapsed < duration) {
            val step = minOf(30, duration - elapsed)
            elapsed += step
            clock += step
            recognized = detector.accept(direction, clock) || recognized
        }
        clock++
        return recognized
    }
}
