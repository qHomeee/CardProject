package com.example.cartproject.data.study

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.display.DisplayManager
import android.view.Display
import android.view.Surface
import com.example.cartproject.domain.study.GestureAvailability
import com.example.cartproject.domain.study.GestureSource
import com.example.cartproject.domain.study.StudyGestureDetector
import com.example.cartproject.domain.study.StudyGestureMode
import com.example.cartproject.domain.study.StudyGestureSignal
import com.example.cartproject.domain.study.StudyPoseSample
import com.example.cartproject.domain.study.StudyScreenDirection
import com.example.cartproject.domain.study.StudyTurnDetector
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** Переводит измерения Android в жесты и освобождает датчики при отмене подписки. */
internal class AndroidStudyGestureSource(
    private val sensors: SensorManager,
    private val displays: DisplayManager,
) : GestureSource {
    override fun events(mode: () -> StudyGestureMode): Flow<StudyGestureSignal> = callbackFlow {
        val detector = StudyGestureDetector()
        val turn = StudyTurnDetector()
        val display = displays.getDisplay(Display.DEFAULT_DISPLAY)
        var rotation = Surface.ROTATION_0
        var lastMode: StudyGestureMode? = null
        var revealSent = false
        var hasGravity = false
        val gravity = FloatArray(3)
        val matrix = FloatArray(9)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val currentRotation = display?.rotation ?: Surface.ROTATION_0
                val currentMode = mode()
                if (currentRotation != rotation || currentMode != lastMode) {
                    rotation = currentRotation
                    lastMode = currentMode
                    revealSent = false
                    detector.reset()
                    turn.reset()
                }
                val atMillis = event.timestamp / 1_000_000
                val action = when (event.sensor.type) {
                    Sensor.TYPE_ROTATION_VECTOR, Sensor.TYPE_GAME_ROTATION_VECTOR -> {
                        if (currentMode != StudyGestureMode.Reveal) return
                        SensorManager.getRotationMatrixFromVector(matrix, event.values)
                        val normal = StudyScreenDirection(matrix[2], matrix[5], matrix[8])
                        StudyGestureSignal.Action.Reveal.takeIf { turn.accept(normal, atMillis) }
                    }
                    else -> {
                        filterGravity(
                            event,
                            gravity,
                            smooth = hasGravity && event.sensor.type == Sensor.TYPE_ACCELEROMETER,
                        )
                        hasGravity = true
                        detector.accept(screenPose(gravity, rotation, atMillis), currentMode)
                    }
                } ?: return

                if (action == StudyGestureSignal.Action.Reveal) {
                    if (revealSent) return
                    revealSent = trySend(action).isSuccess
                } else {
                    trySend(action)
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        val gravityRegistered = register(
            listener,
            Sensor.TYPE_GRAVITY,
            Sensor.TYPE_ACCELEROMETER,
        )
        val turnRegistered = gravityRegistered && register(
            listener,
            Sensor.TYPE_ROTATION_VECTOR,
            Sensor.TYPE_GAME_ROTATION_VECTOR,
        )
        val availability = when {
            turnRegistered -> GestureAvailability.Available
            gravityRegistered -> GestureAvailability.GravityOnly
            else -> GestureAvailability.SensorUnavailable
        }
        trySend(StudyGestureSignal.Availability(availability))
        if (!gravityRegistered) close()
        awaitClose { if (gravityRegistered) sensors.unregisterListener(listener) }
    }

    private fun register(listener: SensorEventListener, vararg types: Int): Boolean =
        types.asSequence().mapNotNull(sensors::getDefaultSensor).any { sensor ->
            sensors.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        }

    private fun filterGravity(event: SensorEvent, filtered: FloatArray, smooth: Boolean) {
        for (axis in filtered.indices) {
            val next = event.values[axis]
            filtered[axis] = if (smooth) filtered[axis] + 0.2f * (next - filtered[axis]) else next
        }
    }
}

/** Переносит X и Y датчика в оси текущего экрана. Ось Z при этом не меняется. */
internal fun screenPose(vector: FloatArray, rotation: Int, atMillis: Long): StudyPoseSample {
    val (x, y, z) = vector
    return when (rotation) {
        Surface.ROTATION_90 -> StudyPoseSample(-y, x, z, atMillis)
        Surface.ROTATION_180 -> StudyPoseSample(-x, -y, z, atMillis)
        Surface.ROTATION_270 -> StudyPoseSample(y, -x, z, atMillis)
        else -> StudyPoseSample(x, y, z, atMillis)
    }
}
