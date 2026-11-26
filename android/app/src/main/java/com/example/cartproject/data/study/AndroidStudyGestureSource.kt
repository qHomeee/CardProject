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
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** Переводит измерения Android в координаты экрана и освобождает датчик при отмене. */
internal class AndroidStudyGestureSource(
    private val sensors: SensorManager,
    private val displays: DisplayManager,
) : GestureSource {
    override fun events(mode: () -> StudyGestureMode): Flow<StudyGestureSignal> = callbackFlow {
        val detector = StudyGestureDetector()
        val display = displays.getDisplay(Display.DEFAULT_DISPLAY)
        var rotation = Surface.ROTATION_0
        var hasSample = false
        val filtered = FloatArray(3)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                filterGravity(
                    event,
                    filtered,
                    smooth = hasSample && event.sensor.type == Sensor.TYPE_ACCELEROMETER,
                )
                hasSample = true
                val currentRotation = display?.rotation ?: Surface.ROTATION_0
                if (currentRotation != rotation) {
                    rotation = currentRotation
                    detector.reset()
                }
                val pose = screenPose(filtered, rotation, event.timestamp / 1_000_000)
                detector.accept(pose, mode())?.let { trySend(it) }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        val registered = sequenceOf(Sensor.TYPE_GRAVITY, Sensor.TYPE_ACCELEROMETER)
            .mapNotNull(sensors::getDefaultSensor)
            .any { sensor ->
                sensors.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
            }
        val availability =
            if (registered) GestureAvailability.Available else GestureAvailability.SensorUnavailable
        trySend(StudyGestureSignal.Availability(availability))
        if (!registered) close()
        awaitClose { if (registered) sensors.unregisterListener(listener) }
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
