/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter and
 * https://github.com/android/wear-os-samples/tree/main/ComposeAdvanced to find the most up to date
 * changes to the libraries and their usages.
 */

package com.example.heartratecompanion.presentation

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText

class MainActivity : ComponentActivity() {
    private lateinit var heartRateDataService: HeartRateDataService
    private lateinit var dataTransmissionService: DataTransmissionService

    override fun onCreate(savedInstanceState: Bundle?) {
        // Initialize heart rate data service
        heartRateDataService = HeartRateDataService(this)
        dataTransmissionService = DataTransmissionService(this)
        dataTransmissionService.connectToSmartphone()

        installSplashScreen()

        super.onCreate(savedInstanceState)

        setTheme(android.R.style.Theme_DeviceDefault)

        // Start DataTransmissionService
        Intent(this, DataTransmissionService::class.java).also { intent ->
            startService(intent)
        }
        dataTransmissionService = DataTransmissionService(this)
        dataTransmissionService?.connectToSmartphone()


        setContent {

            HeartRateRecordingScreen()
        }
    }

    @Composable
    fun HeartRateRecordingScreen() {
        var isRecording by remember {
            mutableStateOf(false)
        }
        var heartRate by remember {
            mutableStateOf(0f)
        }

        heartRateDataService.setOnHeartRateDataListener { newHeartRate ->
            heartRate = newHeartRate
        }

        if (isRecording) {
//            dataTransmissionService.sendHeartRateData(heartRate)
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isRecording) {
                Text(text = "Current Heart Rate: $heartRate BPM")
                CircularProgressIndicator()
                // Stop recording
                Button(onClick = { isRecording = false
                    heartRateDataService.stopHeartRateMonitoring()
                    dataTransmissionService.stopDataTransmission()
                    // Initiate data collection to send to smartphone
                }) {
                    Text(text = "Stop")
                }
            } else {
                // Start recording
                Button(onClick = { isRecording = true
                    heartRateDataService.startHeartRateMonitoring()
                    dataTransmissionService.sendHeartRateData(heartRate)
                    // Initiate data collection to send to smartphone
                }) {
                    Text(text = "Start")
                }

            }

        }
    }


    @Preview(device = Devices.WEAR_OS_SMALL_ROUND, showSystemUi = true)
    @Composable
    fun DefaultPreview() {
        MaterialTheme {
            Box(modifier = Modifier.fillMaxSize()) {
                TimeText(modifier = Modifier.align(Alignment.TopCenter))
                HeartRateRecordingScreen()
            }
        }
    }

}
