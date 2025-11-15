package com.example.myapplication

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import kotlin.concurrent.thread

class BluetoothHelper(
    private val activity: Activity,
    private val listener: Listener
) {
    companion object {
        private const val TAG = "BluetoothHelper"
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
        const val REQ_PERMISSION_CODE = 1001
    }

    interface Listener {
        fun onPermissionRequired(permissions: Array<String>, requestCode: Int = REQ_PERMISSION_CODE)
        fun onPairedDevices(devices: List<BluetoothDevice>)
        fun onConnecting(device: BluetoothDevice)
        fun onConnected(device: BluetoothDevice)
        fun onDisconnected()
        fun onConnectionFailed(reason: String)
        fun onDataReceived(text: String)
    }

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var socket: BluetoothSocket? = null
    private var readThreadRunning = false
    private var inStream: InputStream? = null
    private var outStream: OutputStream? = null
    private var connectedDevice: BluetoothDevice? = null


    // ---------------------------------------------------------
    // PERMISSION HELPERS
    // ---------------------------------------------------------

    private fun hasBtConnectPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(
                activity, Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    private fun hasBtScanPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(
                activity, Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    fun ensurePermissions() {
        val permList = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!hasBtConnectPermission()) permList.add(Manifest.permission.BLUETOOTH_CONNECT)
            if (!hasBtScanPermission()) permList.add(Manifest.permission.BLUETOOTH_SCAN)
        } else {
            if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH)
                != PackageManager.PERMISSION_GRANTED
            ) permList.add(Manifest.permission.BLUETOOTH)

            if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_ADMIN)
                != PackageManager.PERMISSION_GRANTED
            ) permList.add(Manifest.permission.BLUETOOTH_ADMIN)

            if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
            ) permList.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (permList.isNotEmpty()) {
            listener.onPermissionRequired(permList.toTypedArray(), REQ_PERMISSION_CODE)
        }
    }


    // ---------------------------------------------------------
    // LIST PAIRED DEVICES (FIX WARNING)
    // ---------------------------------------------------------

    fun listPairedDevices() {
        val adapter = bluetoothAdapter ?: run {
            listener.onConnectionFailed("Bluetooth tidak tersedia")
            return
        }

        if (!hasBtConnectPermission()) {
            listener.onConnectionFailed("Permission BLUETOOTH_CONNECT belum diberikan")
            return
        }

        try {
            val paired = adapter.bondedDevices?.toList() ?: emptyList()
            listener.onPairedDevices(paired)
        } catch (e: SecurityException) {
            listener.onConnectionFailed("Akses perangkat yang terpasang ditolak oleh sistem.")
        }
    }


    // ---------------------------------------------------------
    // CONNECT BY MAC
    // ---------------------------------------------------------

    fun connect(macAddress: String) {
        val adapter = bluetoothAdapter ?: run {
            listener.onConnectionFailed("Bluetooth tidak tersedia")
            return
        }

        if (!adapter.isEnabled) {
            listener.onConnectionFailed("Bluetooth mati")
            return
        }

        if (!hasBtConnectPermission()) {
            listener.onConnectionFailed("Izin BLUETOOTH_CONNECT belum diberikan")
            return
        }

        val device = try {
            adapter.getRemoteDevice(macAddress)
        } catch (e: SecurityException) {
            listener.onConnectionFailed("Tidak boleh akses perangkat Bluetooth")
            return
        }

        connectDevice(device)
    }


    // ---------------------------------------------------------
    // CONNECT TO DEVICE (FIX WARNING cancelDiscovery)
    // ---------------------------------------------------------

    private fun connectDevice(device: BluetoothDevice) {
        if (!hasBtConnectPermission()) {
            listener.onConnectionFailed("BLUETOOTH_CONNECT permission belum diberikan")
            return
        }

        listener.onConnecting(device)

        thread {
            try {
                socket?.close()
            } catch (_: Exception) {}

            try {
                val tmp = device.createRfcommSocketToServiceRecord(SPP_UUID)

                if (hasBtScanPermission()) {
                    try {
                        bluetoothAdapter?.cancelDiscovery()
                    } catch (e: SecurityException) {
                        Log.e(TAG, "cancelDiscovery ditolak: ${e.message}")
                    }
                }

                tmp.connect()

                socket = tmp
                inStream = tmp.inputStream
                outStream = tmp.outputStream
                connectedDevice = device

                activity.runOnUiThread { listener.onConnected(device) }

                startReadingLoop()

            } catch (e: IOException) {
                Log.e(TAG, "connectDevice error: ${e.message}")
                activity.runOnUiThread {
                    listener.onConnectionFailed("Gagal terhubung: ${e.message}")
                }
                try { socket?.close() } catch (_: Exception) {}
                socket = null
            }
        }
    }


    // ---------------------------------------------------------
    // READ LOOP
    // ---------------------------------------------------------

    private fun startReadingLoop() {
        if (inStream == null) return

        readThreadRunning = true

        thread {
            val buffer = ByteArray(1024)
            val sb = StringBuilder()

            while (readThreadRunning) {
                try {
                    val bytes = inStream?.read(buffer) ?: -1
                    if (bytes > 0) {
                        val text = String(buffer, 0, bytes)
                        sb.append(text)

                        var newLine = sb.indexOf("\n")
                        while (newLine != -1) {
                            val line = sb.substring(0, newLine).trim()
                            if (line.isNotEmpty()) {
                                activity.runOnUiThread {
                                    listener.onDataReceived(line)
                                }
                            }
                            sb.delete(0, newLine + 1)
                            newLine = sb.indexOf("\n")
                        }
                    } else break

                } catch (e: IOException) {
                    Log.e(TAG, "read error: ${e.message}")
                    break
                }
            }

            disconnect()
        }
    }


    // ---------------------------------------------------------
    // SEND DATA
    // ---------------------------------------------------------

    fun write(text: String) {
        try {
            outStream?.write(text.toByteArray())
            outStream?.flush()
        } catch (e: Exception) {
            Log.e(TAG, "write failed: ${e.message}")
        }
    }


    // ---------------------------------------------------------
    // DISCONNECT
    // ---------------------------------------------------------

    fun disconnect() {
        readThreadRunning = false
        try { inStream?.close() } catch (_: Exception) {}
        try { outStream?.close() } catch (_: Exception) {}
        try { socket?.close() } catch (_: Exception) {}
        socket = null
        connectedDevice = null

        activity.runOnUiThread { listener.onDisconnected() }
    }

    fun isConnected(): Boolean =
        socket != null && socket!!.isConnected
}
