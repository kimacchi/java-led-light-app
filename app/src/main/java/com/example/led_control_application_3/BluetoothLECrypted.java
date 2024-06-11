package com.example.led_control_application_3;

import static androidx.core.content.ContextCompat.getSystemService;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.security.Key;
import java.util.Arrays;
import java.util.Base64;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class BluetoothLECrypted<context> {

    private static final String TAG = "PAIRED DEVICES";
    BluetoothManager bluetoothManager;

    BluetoothAdapter bluetoothAdapter;
    BluetoothLeScanner bleScanner;
    private BluetoothGattCharacteristic mmCharacteristic;
    private BluetoothGatt mmGatt;

    android.content.Context context;

    private static final String KEY = "2b7e151628aed2a6abf7158809cf4f3c";
    private static final String ALGORITHM = "AES";

    private static Key generateKey() {
        return new SecretKeySpec(KEY.getBytes(), ALGORITHM);
    }

    public static String encrypt(String message) {
        try {
            //Key key = generateKey();
            SecretKeySpec secretKey = new SecretKeySpec(KEY.getBytes(), ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encryptedBytes = cipher.doFinal(message.getBytes());
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            // Handle the exception (e.g., log it, throw a custom exception, etc.)
            Log.e("Encryption error", e.getMessage());
            return ""; // or handle the error in a different way
        }
    }
    public static String decrypt(String encryptedText){
        try{
            SecretKeySpec secretKey = new SecretKeySpec(KEY.getBytes(), ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(encryptedText));
            return new String(decryptedBytes);
        }
        catch(Exception e){
            Log.e("Decryption error", e.getMessage());
        }
        return "";
    }

    public BluetoothLECrypted(android.content.Context context){
        this.context = context;
        bluetoothManager = getSystemService(context, BluetoothManager.class);
        bluetoothAdapter = bluetoothManager.getAdapter();
    }

    ScanCallback scanCallback = new ScanCallback() {
        @SuppressLint("MissingPermission")
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
//            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
//                return;
//            }
            super.onScanResult(callbackType, result);
            BluetoothDevice device = result.getDevice();
            if(device.getName() != null){
                Log.d("PAIRED DEVICES", "scan result: " + device.getName() + " " + device.getAddress());
            }
            // Check if this is the device you're looking for
            if (device.getAddress().equals("74:2A:8A:11:ED:73")) {
                // Connect to the device
                Log.d("PAIRED DEVICES", "found");
                connectToLeDevice(device);
            }
        }
    };

    public void scan(){
        BluetoothLeScanner bluetoothScanner = bluetoothAdapter.getBluetoothLeScanner();
        bleScanner = bluetoothScanner;
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        bluetoothScanner.startScan(scanCallback);
    }

    @SuppressLint("MissingPermission")
    private void connectToLeDevice(BluetoothDevice device) {
        // Implement BluetoothGattCallback for handling connection events
        BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
            // Override methods such as onConnectionStateChange, onServicesDiscovered, etc.
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    gatt.discoverServices();
                    Log.d("PAIRED DEVICES", "scanning stopped");
                    bleScanner.stopScan(scanCallback);
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                super.onServicesDiscovered(gatt, status);
                Log.d("PAIRED DEVICES", "in function onServicesDiscovered: " + status);
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    mmGatt = gatt;
//                    String message = encrypt(binding.messageEditText.getText().toString());
//                    Log.d("PAIRED DEVICES", "sent message: "+ message );
                    UUID uniqueId = UUID.randomUUID();
                    BluetoothGattService service = gatt.getService(UUID.fromString("19b10000-e8f2-537e-4f6c-d104768a1214"));
                    BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString("19b10001-e8f2-537e-4f6c-d104768a1214"));
                    mmCharacteristic = characteristic;
                    //characteristic.setValue("0".getBytes()); // Set the data you want to send
                    //gatt.writeCharacteristic(characteristic);

                }
            }

            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicRead(gatt, characteristic, status);
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    byte[] data = characteristic.getValue();
                    // Process the received data
                    Log.d(TAG, "Received data: " + Arrays.toString(data));
                } else {
                    Log.e(TAG, "Characteristic read failed with status: " + status);
                }
            }
        };
//        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
//            return;
//        }
        @SuppressLint("MissingPermission") BluetoothGatt gatt = device.connectGatt(context, false, gattCallback);
        Log.d("PAIRED DEVICES", "device connected");
//        binding.statusTextView.setText("Connected to device: " + device.getName());
    }
}
