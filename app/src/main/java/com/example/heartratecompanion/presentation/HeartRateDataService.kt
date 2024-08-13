package com.example.heartratecompanion.presentation

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log

class HeartRateDataService(private val context: Context) {

    private val sensorManager by lazy {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
    private val heartRateSensor by lazy {
        sensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE)
    }


    private var heartRateListener: ((Float) -> Unit)? = null

    fun startHeartRateMonitoring() {
        if (heartRateSensor != null) {
            Log.d("HeartRateDataService", "Heart rate sensor: $heartRateSensor")
            Log.d("HeartRateDataService", "Starting heart rate monitoring...")
           sensorManager.registerListener(heartRateSensorListener, heartRateSensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
        else {
            Log.e("HeartRateDataService", "Heart rate sensor not available")
        }
    }

    fun stopHeartRateMonitoring() {
        Log.d("HeartRateDataService", "Stopping heart rate monitoring...")
        sensorManager.unregisterListener(heartRateSensorListener)
    }

    fun setOnHeartRateDataListener(listener: (Float) -> Unit) {
        heartRateListener = listener
    }

    private val heartRateSensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type == Sensor.TYPE_HEART_RATE) {
                Log.d("HeartRateDataService", "Heart rate data received: ${event.values[0]}")
                val heartRate = event.values[0]
                heartRateListener?.invoke(heartRate)
            } else {
                Log.w("HeartRateDataService", "Unexpected sensor type: ${event.sensor.type}")
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            // Handle accuracy changes if needed
            Log.d("HeartRateDataService", "Sensor accuracy changed: $accuracy")
        }
    }
}
