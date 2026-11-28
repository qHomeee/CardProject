package com.example.cartproject.domain.study

import kotlin.math.sqrt

/** Направление наружу от экрана в общей для измерений системе координат. */
data class StudyScreenDirection(val x: Float, val y: Float, val z: Float) {
    /** Вычисляет скалярное произведение двух направлений экрана. */
    internal fun dot(other: StudyScreenDirection): Float = x * other.x + y * other.y + z * other.z

    /** Сравнивает горизонтальные проекции, если обе выражены достаточно. */
    internal fun horizontalAlignment(other: StudyScreenDirection): Float? {
        val horizontal = sqrt(x * x + y * y)
        val otherHorizontal = sqrt(other.x * other.x + other.y * other.y)
        if (horizontal < 0.45f || otherHorizontal < 0.45f) return null
        return (x * other.x + y * other.y) / (horizontal * otherHorizontal)
    }
}

/** Узнаёт разворот экрана относительно того положения, из которого началось движение. */
class StudyTurnDetector {
    /** Определяет, сравнивается ли разворот в пространстве или по горизонтали. */
    private enum class TurnPlane { Space, Horizontal }

    /** Устойчивое направление и начало его наблюдения. */
    private data class SteadyDirection(val direction: StudyScreenDirection, val since: Long)

    private var lastSample: Long? = null
    private var steady: SteadyDirection? = null
    private var reference: StudyScreenDirection? = null
    private var holdStarted: Long? = null
    private var turnedAway: TurnPlane? = null

    /** Сбрасывает исходное положение при смене карточки или прерывании потока. */
    fun reset() {
        lastSample = null
        steady = null
        reference = null
        holdStarted = null
        turnedAway = null
    }

    /** Подтверждает раскрытие ответа после разворота и возврата экрана к пользователю. */
    fun accept(raw: StudyScreenDirection, atMillis: Long): Boolean {
        val length = sqrt(raw.dot(raw))
        if (length !in 0.8f..1.2f) {
            holdStarted = null
            steady = null
            return false
        }
        val direction = StudyScreenDirection(raw.x / length, raw.y / length, raw.z / length)
        val last = lastSample
        if (last != null && (atMillis < last || atMillis - last > GESTURE_MAX_GAP_MILLIS)) reset()
        lastSample = atMillis

        val initial = reference
        if (initial == null) {
            updateReference(direction, atMillis, GESTURE_HOLD_MILLIS)
            return false
        }

        val alignment = initial.dot(direction)
        val horizontal = initial.horizontalAlignment(direction)
        val plane = turnedAway
        if (plane != null) {
            val returned = alignment >= RETURN_ALIGNMENT ||
                (
                    plane == TurnPlane.Horizontal && horizontal != null &&
                        horizontal >= RETURN_ALIGNMENT
                    )
            if (!returned) {
                holdStarted = null
                return false
            }
            if (!held(atMillis, GESTURE_RETURN_MILLIS)) return false
            reset()
            return true
        }

        val opposite = when {
            alignment <= AWAY_ALIGNMENT -> TurnPlane.Space
            horizontal != null && horizontal <= AWAY_ALIGNMENT -> TurnPlane.Horizontal
            else -> null
        }
        if (opposite == null) {
            holdStarted = null
            updateReference(direction, atMillis, REFERENCE_UPDATE_MILLIS)
            return false
        }

        steady = null
        if (held(atMillis, GESTURE_HOLD_MILLIS)) {
            turnedAway = opposite
            holdStarted = null
        }
        return false
    }

    private fun updateReference(direction: StudyScreenDirection, atMillis: Long, waitMillis: Long) {
        val candidate = steady
        if (candidate == null || candidate.direction.dot(direction) < STEADY_ALIGNMENT) {
            steady = SteadyDirection(direction, atMillis)
        } else if (atMillis - candidate.since >= waitMillis) {
            reference = candidate.direction
        }
    }

    private fun held(atMillis: Long, waitMillis: Long): Boolean {
        val since = holdStarted ?: atMillis.also { holdStarted = it }
        return atMillis - since >= waitMillis
    }

    private companion object {
        const val STEADY_ALIGNMENT = 0.995f
        const val AWAY_ALIGNMENT = -0.72f
        const val RETURN_ALIGNMENT = 0.74f
        const val REFERENCE_UPDATE_MILLIS = 420L
    }
}
