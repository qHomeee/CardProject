package com.example.cartproject.domain.study

import com.example.cartproject.domain.study.StudyGestureSignal.Action
import kotlin.math.abs
import kotlin.math.sqrt

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

    /** Сбрасывает незавершённый жест при смене ориентации экрана. */
    fun reset() {
        step = Step.NeedNeutral
        candidate = Pose.Other
        since = 0L
        previousSampleAt = null
    }

    /** Принимает очередное измерение и возвращает распознанное действие, если оно есть. */
    fun accept(sample: StudyPoseSample): Action? {
        val previous = previousSampleAt
        if (previous != null &&
            (sample.atMillis < previous || sample.atMillis - previous > MAX_SAMPLE_GAP_MILLIS)
        ) {
            reset()
        }
        previousSampleAt = sample.atMillis

        val pose = classify(sample)
        if (pose != candidate) {
            candidate = pose
            since = sample.atMillis
        }
        if (sample.atMillis - since < HOLD_MILLIS) return null
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

    private fun classify(sample: StudyPoseSample): Pose {
        val (x, y, z) = sample
        val magnitude = sqrt(x * x + y * y + z * z)
        if (magnitude !in 6f..13f) return Pose.Other
        val across = x / magnitude
        val up = z / magnitude
        return when {
            up <= -0.80f -> Pose.Down
            up >= 0.80f && abs(across) <= 0.25f -> Pose.Neutral
            up >= 0.40f && across <= -0.50f -> Pose.Right
            up >= 0.40f && across >= 0.50f -> Pose.Left
            else -> Pose.Other
        }
    }

    private companion object {
        const val HOLD_MILLIS = 180L
        const val MAX_SAMPLE_GAP_MILLIS = 300L
    }
}
