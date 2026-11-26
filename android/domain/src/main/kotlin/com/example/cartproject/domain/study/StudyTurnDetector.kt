package com.example.cartproject.domain.study

import kotlin.math.abs
import kotlin.math.sqrt

/** Направление наружу от экрана в общей для измерений системе координат. */
data class StudyScreenDirection(val x: Float, val y: Float, val z: Float) {
    internal fun dot(other: StudyScreenDirection): Float = x * other.x + y * other.y + z * other.z
}

/** Распознаёт разворот экрана от пользователя и возврат в исходное положение. */
class StudyTurnDetector {
    private var lastSample: Long? = null
    private var settling: StudyScreenDirection? = null
    private var reference: StudyScreenDirection? = null
    private var holdStarted: Long? = null
    private var turnedAway = false

    /** Сбрасывает направление при смене карточки, ориентации или прерывании потока. */
    fun reset() {
        lastSample = null
        settling = null
        reference = null
        holdStarted = null
        turnedAway = false
    }

    /** Возвращает true после устойчивого разворота и возврата к исходному направлению. */
    fun accept(raw: StudyScreenDirection, atMillis: Long): Boolean {
        val length = sqrt(raw.dot(raw))
        if (length !in 0.8f..1.2f) {
            holdStarted = null
            settling = null
            return false
        }
        val direction = StudyScreenDirection(raw.x / length, raw.y / length, raw.z / length)
        if (abs(direction.z) > MAX_VERTICAL_COMPONENT) {
            reset()
            return false
        }
        val last = lastSample
        if (last != null && (atMillis < last || atMillis - last > MAX_GAP_MILLIS)) reset()
        lastSample = atMillis

        val initial = reference
        if (initial == null) {
            settleReference(direction, atMillis)
            return false
        }

        val alignment = initial.dot(direction)
        val reached = if (turnedAway) alignment >= RETURN_ALIGNMENT else alignment <= AWAY_ALIGNMENT
        if (!reached) {
            holdStarted = null
            return false
        }
        if (!held(atMillis)) return false

        if (turnedAway) {
            reset()
            return true
        }
        turnedAway = true
        holdStarted = null
        return false
    }

    private fun settleReference(direction: StudyScreenDirection, atMillis: Long) {
        val candidate = settling
        if (candidate == null || candidate.dot(direction) < SETTLING_ALIGNMENT) {
            settling = direction
            holdStarted = atMillis
        } else if (held(atMillis)) {
            reference = candidate
            holdStarted = null
        }
    }

    private fun held(atMillis: Long): Boolean {
        val since = holdStarted ?: atMillis.also { holdStarted = it }
        return atMillis - since >= HOLD_MILLIS
    }

    private companion object {
        const val MAX_VERTICAL_COMPONENT = 0.70f
        const val SETTLING_ALIGNMENT = 0.98f
        const val AWAY_ALIGNMENT = -0.80f
        const val RETURN_ALIGNMENT = 0.80f
        const val HOLD_MILLIS = 180L
        const val MAX_GAP_MILLIS = 300L
    }
}
