package com.epfl.ch.seizureguard.bluetooth

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
import com.epfl.ch.seizureguard.dl.DataSample

@SuppressLint("MissingPermission")
class BluetoothViewModel(application: Application) : AndroidViewModel(application) {

    private val SCAN_PERIOD: Long = 20000
    val myDeviceName = "EEG Sensor"
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

    private val _isConnected = MutableLiveData<Boolean>(false) // is the device connected?
    val isConnected: LiveData<Boolean>
        get() = _isConnected

    private val _dataSample = MutableLiveData<DataSample>()
    val dataSample: LiveData<DataSample>
        get() = _dataSample

    private var bluetoothGatt: BluetoothGatt? = null

    private val context = getApplication<Application>().applicationContext

    // Define batch processing parameters
    private val totalSamplesPerBatch = 1024
    private val totalFloatsPerSample = 18
    private val totalFloatsPerBatch = totalSamplesPerBatch * totalFloatsPerSample // 18,432 floats

    // Buffer to accumulate floats
    private val currentBatchChannels = FloatArray(totalFloatsPerBatch)
    private var floatIndex = 0 // Tracks the current position in the batch buffer

    private fun setEEGCharacteristicNotification(
        characteristic: BluetoothGattCharacteristic,
        enabled: Boolean
    ) { //  enable notifications for a characteristic
        bluetoothGatt?.let { gatt ->
            gatt.setCharacteristicNotification(characteristic, enabled)
            val descriptor = characteristic.getDescriptor(UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG))
            descriptor.value = if (enabled) {
                BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            } else {
                BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
            }
            gatt.writeDescriptor(descriptor)
        } ?: run {
            Log.w("BluetoothCallback", "BluetoothGatt not initialized")
        }
    }

    public fun enableEEGNotifications() { // Public function that external code can call to enable notifications.
        val gattService = bluetoothGatt?.getService(UUID.fromString(EEG_SERVICE))
        val gattCharacteristic = gattService?.getCharacteristic(UUID.fromString(EEG_MEASUREMENT))
        if (gattCharacteristic != null) {
            setEEGCharacteristicNotification(gattCharacteristic, true)
            Log.d("BluetoothViewModel", "EEG Characteristic Notification Enabled via enableEEGNotifications()")
        } else {
            Log.e("BluetoothViewModel", "EEG Measurement Characteristic not found in enableEEGNotifications()")
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
                    bluetoothGatt?.requestMtu(512)
                    bluetoothGatt?.discoverServices() // get information about the services available on the remote device. This will trigger onServicesDiscovered when it's done
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) { // if disconnected
                    Log.w(
                        "BluetoothGattCallback",
                        "Successfully disconnected from $deviceAddress"
                    )
                    _isConnected.postValue(false)
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

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            super.onMtuChanged(gatt, mtu, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i("BluetoothViewModel", "MTU successfully changed to $mtu")
            } else {
                Log.w("BluetoothViewModel", "MTU change failed with status $status")
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) { // called when the device reports on its available services
            Log.d("onServicesDiscovered", "onServicesDiscovered called")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val gattService = bluetoothGatt?.getService(UUID.fromString(EEG_SERVICE))  // get the EEG service using its specific UUID
                val gattCharacteristics = gattService?.getCharacteristic(UUID.fromString(EEG_MEASUREMENT)) // get measurement characteristic from the EEG service
                if (gattCharacteristics != null) {
                    setEEGCharacteristicNotification(
                        gattCharacteristics,
                        true
                    ) // enable notification for the EEG characteristic
                    Log.d("BluetoothGattCallback", "EEG Characteristic Notification Enabled")
                    _isConnected.postValue(true)
                }
            } else {
                Log.w(
                    "BluetoothGattCallback",
                    "onServicesDiscovered received: $status"
                )
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            // Handle multiple floats per notification
            val floatValues = parseFloatsFromCharacteristic(characteristic)
            if (floatValues != null) {
                Log.i("BluetoothViewModel", "Notification Received with ${floatValues.size} floats, index: $floatIndex")
                // Accumulate the floats into the batch buffer
                for (floatValue in floatValues) {
                    if (floatIndex < totalFloatsPerBatch) {
                        currentBatchChannels[floatIndex] = floatValue
                        floatIndex++
                        // Log.d("BluetoothViewModel", "Received float $floatIndex: $floatValue")
                    } else {
                        Log.w("BluetoothViewModel", "Batch buffer overflow. Resetting buffer.")
                        // Handle buffer overflow by resetting
                        floatIndex = 0
                        currentBatchChannels[floatIndex] = floatValue
                        floatIndex++
                    }
                }
                // Check if the batch is complete
                if (floatIndex == totalFloatsPerBatch) {
                    // Create a new DataSample with the accumulated floats
                    val newDataSample = DataSample(
                        data = currentBatchChannels.clone(), // Clone to prevent overwriting
                        label = 0 // Assign a default label or handle accordingly
                    )
                    // Post the complete batch to LiveData
                    _dataSample.postValue(newDataSample)
                    Log.i("BluetoothViewModel", "Complete DataSample Received: ${newDataSample.data.size} floats")

                    // Reset the batch buffer for the next batch
                    floatIndex = 0
                }
            } else {
                Log.e("BluetoothViewModel", "Failed to parse floats from characteristic.")
            }
        }
    }

    private fun parseFloatsFromCharacteristic(characteristic: BluetoothGattCharacteristic): List<Float>? {
        val value = characteristic.value
        if (value == null || value.size < 4) { // At least one float
            Log.e(
                "DataSampleParser",
                "Characteristic value is too short or null: size=${value?.size}"
            )
            return null
        }
        return try {
            val floats = mutableListOf<Float>()
            val buffer = ByteBuffer.wrap(value).order(ByteOrder.LITTLE_ENDIAN)
            while (buffer.remaining() >= 4) { // Each float is 4 bytes
                floats.add(buffer.float)
            }
            floats
        } catch (e: Exception) {
            Log.e("DataSampleParser", "Error parsing floats: ${e.message}")
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
