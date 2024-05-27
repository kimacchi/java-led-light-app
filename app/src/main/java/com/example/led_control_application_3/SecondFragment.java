package com.example.led_control_application_3;

import static androidx.core.content.ContextCompat.getSystemService;
import static androidx.core.content.ContextCompat.registerReceiver;

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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.led_control_application_3.databinding.FragmentSecondBinding;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import java.security.Key;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

public class SecondFragment extends Fragment {

    private static final int REQUEST_ENABLE_BT = 1;

    private BluetoothSocket mmSocket = null;
    private BluetoothDevice mmDevice = null;

    protected static final String PREFS_FILE = "device_id.xml";
    protected static final String PREFS_DEVICE_ID = "device_id";
    private FragmentSecondBinding binding;


    private static final String TAG = "PAIRED DEVICES";
    private static final UUID SERVICE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // Use the same UUID as the connected device
    private OutputStream outputStream;
    private InputStream inputStream;

    BluetoothLeScanner bleScanner;
    private BluetoothGattCharacteristic mmCharacteristic;
    private BluetoothGatt mmGatt;


//    public SecondFragment(BluetoothSocket mmSocket, BluetoothDevice mmDevice) {
//        this.mmSocket = mmSocket;
//        this.mmDevice = mmDevice;
//    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {

        binding = FragmentSecondBinding.inflate(inflater, container, false);
        return binding.getRoot();

    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Context context = this.getContext();

        binding.buttonSecond.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavHostFragment.findNavController(SecondFragment.this)
                        .navigate(R.id.action_SecondFragment_to_FirstFragment);
            }
        });


        BluetoothManager bluetoothManager = getSystemService(this.getContext(), BluetoothManager.class);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            // Device doesn't support Bluetooth
            Log.d("BLUETOOTH", "device does not support bluetooth");
        } else {
            Log.d("BLUETOOTH", "device does support bluetooth");
        }


        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }


        @SuppressLint("MissingPermission") Set<BluetoothDevice> pairedDevices_ = bluetoothAdapter.getBondedDevices();

        if (pairedDevices_.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices_) {
                @SuppressLint("MissingPermission") String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                Log.d("PAIRED DEVICES", deviceName + " " + deviceHardwareAddress);
            }
        } else {
            Log.d("PAIRED DEVICES", "There are no devices paired.");
        }


        BluetoothDevice device_ = bluetoothAdapter.getRemoteDevice("E0:5A:1B:77:E5:D2");

        binding.connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
                if (!pairedDevices.isEmpty()) {
                    for (BluetoothDevice device : pairedDevices) {
                        // Assume you know the device's name or address
                        if (device.getAddress().equals("E0:5A:1B:77:E5:D2")) {
                            mmDevice = device;
                            break;
                        }
                    }
                }
                // Connect to the device
                if (mmDevice != null) {
                    try {
                        connectToDevice();
                        binding.statusTextView.setText("Connected to " + mmDevice.getName());
                    } catch (IOException e) {
                        binding.statusTextView.setText("Connection failed: " + e.getMessage());
                    }
                } else {
                    binding.statusTextView.setText("Device not found");
                }
            }
        });

        binding.sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = binding.messageEditText.getText().toString();
                String encryptedMessage = encrypt(message);
                Log.d("deneme", "encrypted message:" + encryptedMessage);
                String decryptedMessage = decrypt(encryptedMessage);
                Log.d("deneme", "decrypted message: " + decryptedMessage);
                if (!encryptedMessage.isEmpty()) {
                    try {
                        //sendMessage(encryptedMessage);
                        sendLeMessage();
                        Toast.makeText(context, "Message sent", Toast.LENGTH_SHORT).show();
                    } catch (IOException e) {
                        binding.statusTextView.setText("Failed to send message: " + e.getMessage());
                    }
                } else {
                    Toast.makeText(context, "Message is empty", Toast.LENGTH_SHORT).show();
                }
            }
        });

        binding.connectLeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BluetoothLeScanner bluetoothScanner = bluetoothAdapter.getBluetoothLeScanner();
                bleScanner = bluetoothScanner;
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                bluetoothScanner.startScan(scanCallback);

                //bluetoothScanner.stopScan(scanCallback);
            }
        });


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
            if (device.getAddress().equals("E0:5A:1B:77:E5:D2")) {
                // Connect to the device
                Log.d("PAIRED DEVICES", "found");
                connectToLeDevice(device);
            }
        }
    };

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
                    String message = encrypt(binding.messageEditText.getText().toString());
                    Log.d("PAIRED DEVICES", "sent message: "+ message );
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
        binding.statusTextView.setText("Connected to device: " + device.getName());
    }

    @SuppressLint("MissingPermission")
    private void sendLeMessage() throws IOException{
        Log.d("PAIRED DEVICES", "message sent via ble.");
        mmCharacteristic.setValue(binding.messageEditText.getText().toString().getBytes());
        mmGatt.writeCharacteristic(mmCharacteristic);
    }


    private void connectToDevice() throws IOException {
        if (ActivityCompat.checkSelfPermission(getContext(), android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        UUID uuid = mmDevice.getUuids()[0].getUuid(); // Replace with the correct UUID
        mmSocket = mmDevice.createInsecureRfcommSocketToServiceRecord(uuid);
        mmSocket.connect();
        outputStream = mmSocket.getOutputStream();
        inputStream = mmSocket.getInputStream();
    }

    private void sendMessage(String message) throws IOException {
        if (outputStream != null) {
            outputStream.write(message.getBytes());

        } else {
            throw new IOException("Output stream is null");
        }
    }

    private static Context context;
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


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}