package com.example.heartratecompanion.presentation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var heartRateDataService: HeartRateDataService
    private lateinit var dataTransmissionService: DataTransmissionService

    private var isConnected by mutableStateOf(false) // Make isConnected accessible

    private val connectionStatusReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == DataTransmissionService.ACTION_CONNECTED_STATUS) {
                isConnected = intent.getBooleanExtra(DataTransmissionService.EXTRA_CONNECTED, false)
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        // Initialize heart rate data service
        heartRateDataService = HeartRateDataService(this)
        dataTransmissionService = DataTransmissionService(this)
        connectionStatusReceiver.onReceive(
            this,
            Intent(DataTransmissionService.ACTION_CONNECTED_STATUS).apply {
                putExtra(DataTransmissionService.EXTRA_CONNECTED, isConnected)
            }
        )

        installSplashScreen()

        super.onCreate(savedInstanceState)

        setTheme(android.R.style.Theme_DeviceDefault)

        // Start DataTransmissionService
        Intent(this, DataTransmissionService::class.java).also { intent ->
            startService(intent)
        }
        dataTransmissionService = DataTransmissionService(this)

        setContent {

            HeartRateRecordingScreen()
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onResume() {
        super.onResume()
        registerReceiver(connectionStatusReceiver, IntentFilter(DataTransmissionService.ACTION_CONNECTED_STATUS),
            RECEIVER_NOT_EXPORTED
        )
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(connectionStatusReceiver)
    }

    @Composable
    fun HeartRateRecordingScreen() {
        var isRecording by remember { mutableStateOf(false) }
        var heartRate by remember { mutableStateOf(0f) }

        heartRateDataService.setOnHeartRateDataListener { newHeartRate ->
            heartRate = newHeartRate
        }

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (isRecording && isConnected) {
                    Text(text = "Heart Rate: $heartRate")
                    CircularProgressIndicator()
                    // Stop recording
                    Button(onClick = {
                        isRecording = false
                        heartRateDataService.stopHeartRateMonitoring()
                        dataTransmissionService.stopDataTransmission()
                    }) {
                        Text(text = "Stop")
                    }
                    LaunchedEffect(key1 = isRecording) { // Only launch when isRecording changes
                        if (isRecording) {
                            snapshotFlow { heartRate }
                                .collect { hr ->
                                    dataTransmissionService.sendHeartRateData(hr)
                                }
                        }
                    }
                }
                else if (!isRecording && isConnected) { // Check connection status
                    Text(text = "Connected to Smartphone")
                    // Start recording
                    Button(onClick = {
                        isRecording = true
                        heartRateDataService.startHeartRateMonitoring()

                    }) {
                        Text(text = "Start")
                    }
                } else {
                    Text(text = "Not connected to Smartphone")
                    // Connect button
                    Button(onClick = {
                        // Call a function to initiate connection to the smartphone
                         dataTransmissionService.connectToSmartphone()
                    },
                        colors = ButtonDefaults.buttonColors(
                            contentColor = MaterialTheme.colors.onPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {

                        Text(text = "Connect")
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
