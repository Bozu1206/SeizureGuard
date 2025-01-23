package com.tutsplus.bleadvertising;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "BleEEGPeripheral";

    // Request codes
    private static final int REQUEST_PERMISSIONS = 100;
    private static final int REQUEST_ENABLE_BT   = 101;

    private BluetoothManager mBluetoothManager;
    private BluetoothGattServer mGattServer;
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    private BluetoothGattCharacteristic mEEGDataChar;
    private BluetoothGattCharacteristic mConfigChar;

    private TextView mText;
    private Button mStartAdvertiseBtn;
    private Button mStopAdvertiseBtn;

    // Data
    private DataSample[] mDataSamples = null;
    private int mSampleIndex = 0;

    // Service & Characteristic UUIDs
    private UUID EEG_SERVICE_UUID;
    private UUID EEG_CHAR_UUID;
    private UUID CONFIG_CHAR_UUID;

    // CCC Descriptor
    private static final UUID CLIENT_CHARACTERISTIC_CONFIG_UUID =
            UUID.fromString( "00002902-0000-1000-8000-00805f9b34fb");


    // Notification intervals
    private static final long NOTIFY_INTERVAL_NORMAL      = 150;   // ms
    private static final long NOTIFY_INTERVAL_LOW_POWER   = 1000;  // ms
    private static final long NOTIFY_INTERVAL_PERFORMANCE = 80;   // ms

    private long mCurrentNotifyInterval = NOTIFY_INTERVAL_NORMAL;
    private boolean mNotificationsEnabled = false;

    private Handler mNotifyHandler = new Handler();

    int floatsPerChunk = 18;
    int currentOffset = 0;
    int totalFloats = 18*1024;


    // Runnable that periodically sends data to subscribed devices
    private final Runnable mNotifyRunnable = new Runnable() {
        @Override
        public void run() {
            if (mNotificationsEnabled && mEEGDataChar != null && mGattServer != null) {
                if (mDataSamples != null && mDataSamples.length > 0) {
                    if (currentOffset >= totalFloats) {
                        currentOffset = 0;
                    }
                    if (mSampleIndex >= mDataSamples.length) {
                        mSampleIndex = 0;
                    }
                    int floatsLeft = totalFloats - currentOffset;
                    int chunkSize = Math.min(floatsPerChunk, floatsLeft);
                    float[] floats = mDataSamples[mSampleIndex].getData();
                    float[] slice = Arrays.copyOfRange(
                            floats,
                            currentOffset,
                            currentOffset + chunkSize
                    );
                    currentOffset += chunkSize;
                    mSampleIndex++;

                    byte[] dataBytes = floatsToByteArray(slice);
                    mEEGDataChar.setValue(dataBytes);

                    // Notify subscribed devices
                    // Here, BLUETOOTH_CONNECT is required to interact with GATT on Android 12+
                    if (checkBleConnectPermission()) {
                        List<BluetoothDevice> connectedDevices = mBluetoothManager.getConnectedDevices(BluetoothProfile.GATT);
                        for (BluetoothDevice device : connectedDevices) {
                            mGattServer.notifyCharacteristicChanged(device, mEEGDataChar, false);
                            log("Transmitted chunk of " + chunkSize + " floats to " + device.getAddress());
                        }

                    }
                }
            }
            mNotifyHandler.postDelayed(this, mCurrentNotifyInterval);
        }
    };

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Load from strings.xml
        EEG_SERVICE_UUID = UUID.fromString(getString(R.string.ble_uuid));
        EEG_CHAR_UUID    = UUID.fromString(getString(R.string.eeg_char_uuid));
        CONFIG_CHAR_UUID = UUID.fromString(getString(R.string.config_char_uuid));

        mText = findViewById(R.id.text);
        mStartAdvertiseBtn = findViewById(R.id.advertise_btn);
        mStopAdvertiseBtn  = findViewById(R.id.stop_advertise_btn);

        mStartAdvertiseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ensureBlePermissionsGranted(() -> {
                    // Once permissions are ensured, we can start advertising
                    startAdvertising();
                });
            }
        });

        mStopAdvertiseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ensureBlePermissionsGranted(() -> {
                    stopAdvertising();
                });
                mText.setText("");
            }
        });

        // Manager & adapter
        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter adapter = mBluetoothManager.getAdapter();

        if (adapter == null) {
            Toast.makeText(this, "Bluetooth not supported on this device!", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // If BT is off, request to enable it. This requires BLUETOOTH_CONNECT on Android 12+.
        ensureBlePermissionsGranted(() -> {
            // Now we definitely have BLUETOOTH_CONNECT
            if (!adapter.isEnabled()) {
                requestEnableBluetooth();
            } else {
                initBleFeatures();
            }
        });
    }

    @SuppressLint("MissingPermission")
    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAdvertising();
        if (mGattServer != null) {
            mGattServer.close();
        }
    }

    //region BLE/Permissions Management

    /**
     * Prompt user to enable Bluetooth via system dialog
     * Must have BLUETOOTH_CONNECT permission to call this on Android 12+.
     */
    @SuppressLint("MissingPermission")
    private void requestEnableBluetooth() {
        BluetoothAdapter adapter = mBluetoothManager.getAdapter();
        if (adapter != null && !adapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    /**
     * This method will check if the needed BLE permissions are granted.
     * If not granted, it requests them. Once granted, it runs the `onPermissionsGranted` callback.
     */
    private void ensureBlePermissionsGranted(@NonNull Runnable onPermissionsGranted) {
        // On Android 12+ (API 31+), we need BLUETOOTH_CONNECT and/or BLUETOOTH_ADVERTISE
        // On older versions, we may also need location for BLE scanning/advertising
        if (!hasAllRequiredPermissions()) {
            // Request them
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(getRequiredPermissions(), REQUEST_PERMISSIONS);
            }
        } else {
            // Already granted
            onPermissionsGranted.run();
        }
    }

    /**
     * Check if app has all the relevant BLE permissions for scanning/advertising/connecting.
     */
    private boolean hasAllRequiredPermissions() {
        for (String permission : getRequiredPermissions()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Return the list of permissions we might need, depending on OS version.
     */
    private String[] getRequiredPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12 and above
            return new String[] {
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_ADVERTISE
                    // If scanning is involved, also BLUETOOTH_SCAN
                    // (and possibly location for older behaviors)
            };
        } else {
            // Pre-Android 12 might need location for BLE
            return new String[] {
                    Manifest.permission.ACCESS_COARSE_LOCATION
            };
        }
    }

    /**
     * Simple checks to see if we have BLUETOOTH_CONNECT at runtime (Android 12+).
     */
    private boolean checkBleConnectPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return true; // Not required pre-S
        }
        return (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED);
    }

    //endregion

    //region Activity Results / Permissions

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_PERMISSIONS) {
            if (hasAllRequiredPermissions()) {
                // Possibly re-run logic if needed
                Log.d(TAG, "All required BLE permissions granted.");
            } else {
                Toast.makeText(this, "Required permissions not granted.", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    /**
     * Called when the user returns from the "Enable Bluetooth" system dialog.
     */
    @SuppressLint("MissingPermission")
    @Override
    protected void onActivityResult(int requestCode,
                                    int resultCode,
                                    Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            BluetoothAdapter adapter = mBluetoothManager.getAdapter();
            if (adapter != null && adapter.isEnabled()) {
                initBleFeatures();
            } else {
                Toast.makeText(this, "Bluetooth not enabled.", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }
    //endregion

    /**
     * Once BT is on and permissions are granted, do your normal BLE setup:
     * - Check if the device supports multiple advertisement
     * - Create GATT server
     * - Load EEG data
     */
    @SuppressLint("MissingPermission")
    private void initBleFeatures() {
        BluetoothAdapter adapter = mBluetoothManager.getAdapter();
        if (!adapter.isMultipleAdvertisementSupported()) {
            Toast.makeText(this, "BLE Peripheral not supported on this device.", Toast.LENGTH_LONG).show();
            mStartAdvertiseBtn.setEnabled(false);
            return;
        }

        setupGattServer();

        // Load data
        try {
            DataLoader loader = new DataLoader();
            mDataSamples = loader.loadDataAndLabels(this, "data_20.bin");
            log("Loaded " + mDataSamples.length + " samples from asset.");
        } catch (IOException e) {
            log("Error loading data: " + e.getMessage());
        }
    }

    //region GATT / Advertising

    @SuppressLint("MissingPermission")
    private void setupGattServer() {
        if (!checkBleConnectPermission()) {
            log("No BLUETOOTH_CONNECT permission to open GATT Server");
            return;
        }
        mGattServer = mBluetoothManager.openGattServer(this, mGattServerCallback);
        if (mGattServer == null) {
            log("Failed to open GATT Server");
            return;
        }
        log("GATT Server opened successfully");
        // Create our primary EEG Service
        BluetoothGattService eegService = new BluetoothGattService(
                EEG_SERVICE_UUID,
                BluetoothGattService.SERVICE_TYPE_PRIMARY
        );
        // --- EEG Data Characteristic (Notify) ---
        mEEGDataChar = new BluetoothGattCharacteristic(
                EEG_CHAR_UUID,  // Use the 'eeg_char_uuid' from strings.xml
                BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ
        );
        // Add CCC descriptor so the client can enable notifications
        BluetoothGattDescriptor cccDescriptor = new BluetoothGattDescriptor(
                CLIENT_CHARACTERISTIC_CONFIG_UUID,
                (BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE)
        );
        mEEGDataChar.addDescriptor(cccDescriptor);
        // --- Config Characteristic (Write) ---
        mConfigChar = new BluetoothGattCharacteristic(
                CONFIG_CHAR_UUID,  // Use the 'config_char_uuid' from strings.xml
                (BluetoothGattCharacteristic.PROPERTY_WRITE |
                        BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE),
                BluetoothGattCharacteristic.PERMISSION_WRITE
        );
        // Add both characteristics to the service
        eegService.addCharacteristic(mEEGDataChar);
        eegService.addCharacteristic(mConfigChar);
        // Finally, add the service to the GATT server
        boolean serviceAdded = mGattServer.addService(eegService);
        if (serviceAdded) {
            log("EEG Service added successfully to GATT Server");
        } else {
            log("Failed to add EEG Service to GATT Server");
        }
    }

    @SuppressLint("MissingPermission")
    private void startAdvertising() {
        // Must have BLUETOOTH_ADVERTISE permission on Android 12+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
                log("No BLUETOOTH_ADVERTISE permission, cannot start advertising.");
                return;
            }
        }

        BluetoothAdapter adapter = mBluetoothManager.getAdapter();
        adapter.setName("EEG Sensor");
        mBluetoothLeAdvertiser = adapter.getBluetoothLeAdvertiser();
        if (mBluetoothLeAdvertiser == null) {
            log("Failed to get BluetoothLeAdvertiser");
            return;
        }

        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setConnectable(true)
                .build();

        ParcelUuid pUuid = new ParcelUuid(EEG_SERVICE_UUID);

        AdvertiseData data = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .build();

        mBluetoothLeAdvertiser.startAdvertising(settings, data, mAdvertiseCallback);
        log("Started Advertising");
    }

    @SuppressLint("MissingPermission")
    private void stopAdvertising() {
        if (mBluetoothLeAdvertiser != null) {
            // Must have BLUETOOTH_ADVERTISE to stopAdvertising on Android 12+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                    checkSelfPermission(Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
                log("No BLUETOOTH_ADVERTISE permission to stop advertising.");
                return;
            }
            mBluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
        }
        mNotificationsEnabled = false;
        mNotifyHandler.removeCallbacks(mNotifyRunnable);
        log("Stopped Advertising");
    }

    private final AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            log("Advertising onStartSuccess");
        }

        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            log("Advertising onStartFailure: " + errorCode);
        }
    };

    //endregion

    //region GATT Callback

    private final BluetoothGattServerCallback mGattServerCallback = new BluetoothGattServerCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onConnectionStateChange(BluetoothDevice device, final int status, int newState) {
            super.onConnectionStateChange(device, status, newState);
            runOnUiThread(() -> {
                if (status == BluetoothGatt.GATT_SUCCESS &&
                        newState == android.bluetooth.BluetoothProfile.STATE_CONNECTED) {
                    log("Connected to " + device.getAddress());
                } else if (newState == android.bluetooth.BluetoothProfile.STATE_DISCONNECTED) {
                    log("Disconnected from " + device.getAddress());
                }
            });

        }

        @Override
        public void onMtuChanged(@NonNull BluetoothDevice device, int mtu) {
            super.onMtuChanged(device, mtu);
            Log.d(TAG, "MTU changed. Final value: " + mtu);
        }


        @SuppressLint("MissingPermission")
        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device,
                                             int requestId,
                                             BluetoothGattDescriptor descriptor,
                                             boolean preparedWrite,
                                             boolean responseNeeded,
                                             int offset,
                                             byte[] value) {
            super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);

            if (CLIENT_CHARACTERISTIC_CONFIG_UUID.equals(descriptor.getUuid())) {
                // If the central wrote ENABLE_NOTIFICATION_VALUE, enable notifications
                if (Arrays.equals(value, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)) {
                    mNotificationsEnabled = true;
                    log("Notifications enabled by " + device.getAddress());
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        mNotifyHandler.post(mNotifyRunnable);
                    }, 300);
                } else {
                    mNotificationsEnabled = false;
                    log("Notifications disabled by " + device.getAddress());
                }
                if (responseNeeded && checkBleConnectPermission()) {
                    mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null);
                }
            }
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device,
                                                 int requestId,
                                                 BluetoothGattCharacteristic characteristic,
                                                 boolean preparedWrite,
                                                 boolean responseNeeded,
                                                 int offset,
                                                 byte[] value) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);

            if (characteristic.getUuid().equals(CONFIG_CHAR_UUID) && value != null && value.length == 1) {
                byte mode = value[0];
                switch (mode) {
                    case 1:
                        mCurrentNotifyInterval = NOTIFY_INTERVAL_LOW_POWER;
                        log("Config set to Low Power Mode");
                        break;
                    case 2:
                        mCurrentNotifyInterval = NOTIFY_INTERVAL_PERFORMANCE;
                        log("Config set to Performance Mode");
                        break;
                    default:
                        mCurrentNotifyInterval = NOTIFY_INTERVAL_NORMAL;
                        log("Config set to Normal Mode");
                        break;
                }
                // Respond if needed
                if (responseNeeded && checkBleConnectPermission()) {
                    mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null);
                }
            }
        }
    };

    //endregion

    //region Helpers

    private void log(final String msg) {
        runOnUiThread(() -> {
            mText.append(msg + "\n");
            Log.d(TAG, msg);
        });
    }

    // Convert float[] to little-endian byte[]
    private byte[] floatsToByteArray(float[] values) {
        ByteBuffer buffer = ByteBuffer.allocate(values.length * 4).order(ByteOrder.LITTLE_ENDIAN);
        for (float f : values) {
            buffer.putFloat(f);
        }
        return buffer.array();
    }

    //endregion
}