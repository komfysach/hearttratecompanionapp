package com.example.heartratecompanion.presentation

import android.annotation.SuppressLint
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.UUID

class DataTransmissionService(private val context: Context) : Service() {

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var bluetoothSocket: BluetoothSocket? = null
    private var dataTransferJob: Job? = null

    companion object {
        private const val TAG = "DataTransmissionService"
        private const val SMARTPHONE_MAC_ADDRESS = "54:92:09:AB:9B:F4" // Replace with your smartphone's MAC address
        private val MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // Replace with your app's UUID
        const val ACTION_CONNECTED_STATUS = "com.example.heartratecompanion.ACTION_CONNECTED_STATUS"
        const val EXTRA_CONNECTED = "com.example.heartratecompanion.EXTRA_CONNECTED"
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    @SuppressLint("MissingPermission")
    fun connectToSmartphone() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
                if (pairedDevices != null) {
                    for (device in pairedDevices) {
                        Log.d(TAG, "Checking device: ${device.name}")
                        if (device.name == "HUAWEI P30 Pro") { // Replace with your smartphone's name

                            bluetoothSocket = device.createRfcommSocketToServiceRecord(MY_UUID)
                            bluetoothSocket?.connect()

                            Log.d(TAG, "Connected to ${device.name}")
                            updateConnectionStatus(true)
                            break
                        }
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "Connection to smartphone failed", e)
            }
        }
    }

    suspend fun sendHeartRateData(heartRate: Float) {
        if (bluetoothSocket?.isConnected == true) {
            dataTransferJob = CoroutineScope(Dispatchers.IO).launch {
                while (true) {
                    try {
                        val heartRateData = heartRate.toString().toByteArray()
                        bluetoothSocket?.outputStream?.write(heartRateData)
                        Log.d(TAG, "Heart rate data sent: $heartRate")
                        delay(1000) // Send data every 1 second (adjust as needed)
                    } catch (e: IOException) {
                        Log.e(TAG, "Error sending data: ${e.message}")
                        // Handle error (e.g., close connection and retry)
                        break
                    }
                }
            }
        } else  {
            Log.e(TAG, "Not connected to smartphone")
            // Attempt to reconnect
            connectToSmartphone()
            // You might want to add a delay here before checking the connection again
            delay(1000) // Wait 1 second before checking the connection again
            if (bluetoothSocket?.isConnected == true) {
                // Retry sending data if the connection was successful
                sendHeartRateData(heartRate)
            } else {
                // Handle the case where reconnection failed
                // (e.g., notify the user, stop data transmission)
            }
        }
    }

    fun stopDataTransmission() {
        dataTransferJob?.cancel()
        try {
            bluetoothSocket?.close()
            Log.d(TAG, "Connection closed")
            updateConnectionStatus(false)
        } catch (e: IOException) {
            Log.e(TAG, "Error closing connection: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopDataTransmission()
    }

    private fun updateConnectionStatus(isConnected: Boolean) {
        val intent = Intent(ACTION_CONNECTED_STATUS).apply {
            putExtra(EXTRA_CONNECTED, isConnected)
        }
        if (context != null) {
            Handler(Looper.getMainLooper()).post {
                context.applicationContext.sendBroadcast(intent)
            }
        }
    }

}