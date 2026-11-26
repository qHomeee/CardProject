package com.example.cartproject.domain.study

import com.example.cartproject.domain.study.StudyGestureSignal.Action
import kotlin.math.abs
import kotlin.math.sqrt

internal const val GESTURE_HOLD_MILLIS = 180L
internal const val GESTURE_RETURN_MILLIS = 60L
internal const val GESTURE_MAX_GAP_MILLIS = 300L

/** Вектор ускорения в осях экрана и время датчика в миллисекундах. */
data class StudyPoseSample(
    val horizontal: Float,
    val vertical: Float,
    val normal: Float,
    val atMillis: Long,
)

/** Выдаёт один жест после устойчивого положения и возврата к нейтрали. */
class StudyGestureDetector {
    private enum class Pose { Neutral, Down, Right, Left, Other }

    private enum class Step { NeedNeutral, Ready, TurnedDown }

    private var step = Step.NeedNeutral
    private var candidate = Pose.Other
    private var since = 0L
    private var previousSampleAt: Long? = null
    private var previousMode: StudyGestureMode? = null

    /** Сбрасывает незавершённый жест при смене ориентации экрана. */
    fun reset() {
        step = Step.NeedNeutral
        candidate = Pose.Other
        since = 0L
        previousSampleAt = null
        previousMode = null
    }

    /** Принимает очередное измерение и возвращает распознанное действие, если оно есть. */
    fun accept(sample: StudyPoseSample, mode: StudyGestureMode): Action? {
        val previous = previousSampleAt
        val interrupted = previous != null &&
            (sample.atMillis < previous || sample.atMillis - previous > GESTURE_MAX_GAP_MILLIS)
        if (previousMode != mode || interrupted) {
            reset()
        }
        previousMode = mode
        previousSampleAt = sample.atMillis

        val pose = classify(sample, mode)
        if (pose != candidate) {
            candidate = pose
            since = sample.atMillis
        }
        val waitMillis = if (pose == Pose.Neutral && step == Step.TurnedDown) {
            GESTURE_RETURN_MILLIS
        } else {
            GESTURE_HOLD_MILLIS
        }
        if (sample.atMillis - since < waitMillis) return null
        return when (pose) {
            Pose.Neutral -> {
                val reveal = step == Step.TurnedDown
                step = Step.Ready
                if (reveal) Action.Reveal else null
            }
            Pose.Down -> {
                if (step == Step.Ready) step = Step.TurnedDown
                null
            }
            Pose.Right -> tilt(Action.Remembered)
            Pose.Left -> tilt(Action.NeedsReview)
            Pose.Other -> null
        }
    }

    private fun tilt(action: Action): Action? {
        if (step != Step.Ready) return null
        step = Step.NeedNeutral
        return action
    }

    private fun classify(sample: StudyPoseSample, mode: StudyGestureMode): Pose {
        val (x, y, z) = sample
        val magnitude = sqrt(x * x + y * y + z * z)
        if (magnitude !in 6f..13f) return Pose.Other
        val across = x / magnitude
        val up = z / magnitude
        if (mode == StudyGestureMode.Reveal) {
            return when {
                up >= 0.80f && abs(across) <= 0.25f -> Pose.Neutral
                up <= -0.80f -> Pose.Down
                else -> Pose.Other
            }
        }
        val upward = y / magnitude
        if (up < -0.25f || (up < 0.40f && upward < 0.40f)) return Pose.Other
        return when {
            abs(across) <= 0.25f -> Pose.Neutral
            across <= -0.50f -> Pose.Right
            across >= 0.50f -> Pose.Left
            else -> Pose.Other
        }
    }
}
