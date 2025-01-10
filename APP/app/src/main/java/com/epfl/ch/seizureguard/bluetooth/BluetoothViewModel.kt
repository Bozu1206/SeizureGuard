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
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
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
    private val CONFIG_CHARACTERISTIC = "bb54968e-e5f8-c6ad-eb11-982cdfb71a46"

    val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
    private var scanning = false
    private val handler = Handler(Looper.getMainLooper())

    private val _lastValues = MutableLiveData<DataSample>()
    val lastValues: LiveData<DataSample>
        get() = _lastValues
    private var lastValuesIndex = 0
    private val floatsPerLastSample = 18 * 128
    private val lastValuesBuffer = FloatArray(floatsPerLastSample)

    private val _isConnected = MutableLiveData<Boolean>(false) // is the device connected?
    val isConnected: LiveData<Boolean>
        get() = _isConnected

    private var deviceFound: Boolean = false // have i finished scanning?

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

    private var powerMode : Byte = 0x00
    fun setPowerMode(newPowerMode : Byte){
        powerMode = newPowerMode
    }

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
                _isConnected.postValue(false)
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

                val configCharacteristic = gattService?.getCharacteristic(UUID.fromString(CONFIG_CHARACTERISTIC))
                if (configCharacteristic != null) {
                    configCharacteristic.value = byteArrayOf(powerMode)
                    val writeSuccess = bluetoothGatt?.writeCharacteristic(configCharacteristic) ?: false
                    if (writeSuccess) {
                        Log.d("BluetoothGattCallback", "Wrote config byte: $powerMode")
                    } else {
                        Log.e("BluetoothGattCallback", "Failed to write config byte!")
                    }
                } else {
                    Log.w("BluetoothGattCallback", "Config characteristic not found!")
                }
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
                // --- Handle lastDataSample accumulation --- for plotting faster
                for (floatValue in floatValues) {
                    if (lastValuesIndex < floatsPerLastSample) {
                        lastValuesBuffer[lastValuesIndex] = floatValue
                        lastValuesIndex++
                    } else {
                        Log.w("BluetoothViewModel", "Last sample buffer overflow. Resetting buffer.")
                        lastValuesIndex = 0
                        lastValuesBuffer[lastValuesIndex] = floatValue
                        lastValuesIndex++
                    }
                }
                // Check if the lastDataSample batch is complete
                if (lastValuesIndex == floatsPerLastSample) {
                    val newLastDataSample = DataSample(
                        data = lastValuesBuffer.clone(), // Clone to prevent overwriting
                        label = 0 // Assign a default label or handle accordingly
                    )
                    _lastValues.postValue(newLastDataSample)
                    Log.i("BluetoothViewModel", "Complete LastDataSample Received: ${newLastDataSample.data.size} floats")
                    lastValuesIndex = 0
                }
            } else {
                Log.e("BluetoothViewModel", "Failed to parse floats from characteristic.")
            }
        }
        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (characteristic.uuid == UUID.fromString(CONFIG_CHARACTERISTIC)) {
                    Log.d("BluetoothGattCallback", "Config byte write successful!")
                    // Now enable notifications:
                    val eegChar = gatt.getService(UUID.fromString(EEG_SERVICE))
                        ?.getCharacteristic(UUID.fromString(EEG_MEASUREMENT))
                    if (eegChar != null) {
                        setEEGCharacteristicNotification(eegChar, true)
                    }
                }
            } else {
                Log.e("BluetoothGattCallback", "Characteristic write failed with status=$status")
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
        deviceFound = false
        if (!scanning) { // if not already scanning
            handler.postDelayed({ // do the following after SCAN_PERIOD time
                scanning = false // set local scanning flag to false
                bluetoothLeScanner?.stopScan(leScanCallback)
                Log.d("ScanLeDevice", "Scanning is over")
                if (deviceFound == false){
                    Toast.makeText(context, "No Devices found!", Toast.LENGTH_LONG).show()
                }
            }, SCAN_PERIOD)
            scanning = true // set scanning to true
            bluetoothLeScanner?.startScan(getScanFilters(), getScanSettings(), leScanCallback) // start scanning
        } else { // if already scanning, stop
            scanning = false
            bluetoothLeScanner?.stopScan(leScanCallback)
        }
    }
    private fun getScanFilters(): List<ScanFilter> {
        return listOf(
            ScanFilter.Builder()
                .setDeviceName(myDeviceName)
                .build()
        )
    }
    private fun getScanSettings(): ScanSettings {
        return ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY) // Adjust as needed
            .build()
    }
    fun stopScanning() {
        if (scanning) {
            scanning = false
            bluetoothLeScanner?.stopScan(leScanCallback)
            handler.removeCallbacksAndMessages(null) // Remove any pending callbacks
            Log.d("BluetoothScan", "Scanning stopped")
        } else {
            Log.d("BluetoothScan", "Scanning was not active")
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
                deviceFound = true
                Log.d("Device found", "Device found")
                Toast.makeText(context, "Device found!", Toast.LENGTH_SHORT).show()
                bluetoothLeScanner?.stopScan(this) // stop scanning
                result.device.connectGatt(context, false, gattCallback) // connect to device's GATT
                stopScanning()
            }
        }
    }

    fun stopBLE() {
        val gattService = bluetoothGatt?.getService(UUID.fromString(EEG_SERVICE))  // get the EEG service using its specific UUID
        val gattCharacteristics = gattService?.getCharacteristic(UUID.fromString(EEG_MEASUREMENT)) // get measurement characteristic from the EEG service
        if (gattCharacteristics != null) setEEGCharacteristicNotification(gattCharacteristics, false)
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        _isConnected.postValue(false)
    }
}
