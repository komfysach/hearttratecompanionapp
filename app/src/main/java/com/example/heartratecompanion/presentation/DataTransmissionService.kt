package com.example.heartratecompanion.presentation

import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.os.IBinder
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
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    fun connectToSmartphone() {
        CoroutineScope(Dispatchers.IO).launch {
            var retryCount = 0
            val maxRetries = 3 // Adjust as needed
            while (retryCount < maxRetries && bluetoothSocket?.isConnected != true) {
                try {
                    val smartphoneDevice: BluetoothDevice = bluetoothAdapter?.getRemoteDevice(SMARTPHONE_MAC_ADDRESS)!!
                    bluetoothSocket = smartphoneDevice.createRfcommSocketToServiceRecord(MY_UUID)
                    bluetoothSocket?.connect()
                    Log.d(TAG, "Connected to smartphone")
                } catch (e: IOException) {
                    Log.e(TAG, "Error connecting to smartphone: ${e.message}")
                    retryCount++
                    delay(1000) // Wait 1 second before retrying
                }
            }
            if (bluetoothSocket?.isConnected != true) {
                Log.e(TAG, "Failed to connect to smartphone after $maxRetries retries")
                // Handle connection failure (e.g., notify user)
            }
        }
    }

    fun sendHeartRateData(heartRate: Float) {
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
            if (bluetoothSocket?.isConnected== true) {
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
        } catch (e: IOException) {
            Log.e(TAG, "Error closing connection: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopDataTransmission()
    }
}