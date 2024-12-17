package com.example.seizureguard.bluetooth

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.os.Handler
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import com.example.seizureguard.dl.DataSample

@SuppressLint("MissingPermission")
class BluetoothViewModel(application: Application) : AndroidViewModel(application) {

    private val SCAN_PERIOD: Long = 20000
    private val myDeviceName = "EEG Sensor"
    private val EEG_SERVICE = "0000745D-0000-1100-8800-00605f9c34ca" // GATT service UUID for EEG
    private val EEG_MEASUREMENT = "00002a27-0000-4300-8900-00805a4a21fb" // EEG measure characteristic
    private val CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb"

    val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
    private var scanning = false
    private val handler = Handler()

    private val _eegValue = MutableLiveData<Int>(0)
    val eegValue: LiveData<Int>
        get() = _eegValue

    private var bluetoothGatt: BluetoothGatt? = null

    private val context = getApplication<Application>().applicationContext

    // MutableList to accumulate channel floats
    private val currentSampleChannels = mutableListOf<Float>()
    private val totalChannels = 18 // Number of channels per sample

    private fun setEEGCharacteristicNotification(
        characteristic: BluetoothGattCharacteristic,
        enabled: Boolean
    ) { //  enable notifications for a characteristic
        bluetoothGatt?.let { gatt ->
            gatt.setCharacteristicNotification(characteristic, enabled) // enable/disable notifications (in the local state)
            val descriptor =
                characteristic.getDescriptor(UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG)) // specific UUID to configure a characteristic
            descriptor.value =
                BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE // set configuration
            gatt.writeDescriptor(descriptor) // send configuration
        } ?: run {
            Log.w("BluetoothCallback", "BluetoothGatt not initialized")
        }
    }

    private val gattCallback = object : BluetoothGattCallback() { //  triggered at every change in GATT connection state, service discovery, characteristic reads, and characteristic notifications
        override fun onConnectionStateChange(
            gatt: BluetoothGatt, // called when the device's GATT changes
            status: Int,
            newState: Int
        ) {
            val deviceAddress = gatt.device.address // get the new GATT
            if (status == BluetoothGatt.GATT_SUCCESS) { // if no errors occurred
                if (newState == BluetoothProfile.STATE_CONNECTED) { // if connected
                    Log.w(
                        "BluetoothGattCallback",
                        "Successfully connected to $deviceAddress"
                    )
                    bluetoothGatt = gatt // save the GATT in the bluetoothGatt variable.
                    bluetoothGatt?.discoverServices() // get information about the services available on the remote device. This will trigger onServicesDiscovered when it's done
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) { // if disconnected
                    Log.w(
                        "BluetoothGattCallback",
                        "Successfully disconnected from $deviceAddress"
                    )
                    gatt.close() // close the GATT
                }
            } else {
                Log.w(
                    "BluetoothGattCallback",
                    "Error $status encountered for $deviceAddress! Disconnecting..."
                )
                gatt.close()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) { // called when the device reports on its available services
            if (status == BluetoothGatt.GATT_SUCCESS) { // if no errors
                val gattService =
                    bluetoothGatt?.getService(UUID.fromString(EEG_SERVICE))  // get the EEG service using its specific UUID
                val gattCharacteristics =
                    gattService?.getCharacteristic(UUID.fromString(EEG_MEASUREMENT)) // get measurement characteristic from the EEG service
                if (gattCharacteristics != null) {
                    setEEGCharacteristicNotification(gattCharacteristics, true) // enable notification for the EEG characteristic
                    Log.d("BluetoothGattCallback", "EEG Characteristic Notification Enabled")
                } else {
                    Log.e(
                        "BluetoothGattCallback",
                        "EEG Measurement Characteristic not found!"
                    )
                }
            } else {
                Log.w(
                    "BluetoothGattCallback",
                    "onServicesDiscovered received: $status"
                )
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt, // once notifications are enabled, called everytime we have a new value in our characteristic
            characteristic: BluetoothGattCharacteristic
        ) {
            // Handle individual channel float
            val floatValue = parseSingleFloatFromCharacteristic(characteristic)
            if (floatValue != null) {
                currentSampleChannels.add(floatValue)
                Log.d("BluetoothViewModel", "Received channel ${currentSampleChannels.size}: $floatValue")

                if (currentSampleChannels.size == totalChannels) {
                    // All channels for the current sample have been received
                    val dataSample = DataSample(
                        data = currentSampleChannels.toFloatArray(),
                        label = 0 // Assign a default label or handle accordingly
                    )
                    Log.i("BluetoothViewModel", "Complete DataSample Received: ${dataSample.data.joinToString(", ")}")
                    // Reset for next sample
                    currentSampleChannels.clear()
                }
            } else {
                Log.e("BluetoothViewModel", "Failed to parse float from characteristic.")
            }
        }
    }

    private fun parseSingleFloatFromCharacteristic(characteristic: BluetoothGattCharacteristic): Float? {
        val value = characteristic.value
        if (value == null || value.size < 4) { // A float is 4 bytes
            Log.e(
                "DataSampleParser",
                "Characteristic value is too short or null: size=${value?.size}"
            )
            return null
        }
        return try {
            // Prepare to read the binary data
            val buffer = ByteBuffer.wrap(value).order(ByteOrder.LITTLE_ENDIAN)
            buffer.float
        } catch (e: Exception) {
            Log.e("DataSampleParser", "Error parsing float: ${e.message}")
            null
        }
    }

    fun scanLeDevice() { // For scanning for Bluetooth low energy devices
        Log.d("BluetoothScan", "Scanning for BLE devices")
        if (!scanning) { // if not already scanning
            handler.postDelayed({ // do the following after SCAN_PERIOD time
                scanning = false // set local scanning flag to false
                bluetoothLeScanner?.stopScan(leScanCallback)
                Log.d("ScanLeDevice", "Scanning is over")
            }, SCAN_PERIOD)
            scanning = true // set scanning to true
            bluetoothLeScanner?.startScan(leScanCallback) // start scanning
        } else { // if already scanning, stop
            scanning = false
            bluetoothLeScanner?.stopScan(leScanCallback)
        }
    }

    private val leScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            Log.i(
                BluetoothViewModel::class.simpleName,
                "Name: ${result.device.name}, " +
                        "Address: ${result.device.address}," +
                        " RSSI: ${result.rssi}"
            )
            if (result.device.name == myDeviceName) { // check if it's the desired device
                Log.d("Device found", "Device found")
                bluetoothLeScanner?.stopScan(this) // stop scanning
                result.device.connectGatt(context, false, gattCallback) // connect to device's GATT
            }
        }
    }

    fun stopBLE() {
        bluetoothGatt?.let { gatt ->
            gatt.close()
            bluetoothGatt = null
        }
    }

}
