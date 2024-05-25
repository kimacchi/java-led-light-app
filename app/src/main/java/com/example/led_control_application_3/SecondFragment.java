package com.example.led_control_application_3;

import static androidx.core.content.ContextCompat.getSystemService;
import static androidx.core.content.ContextCompat.registerReceiver;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
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
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public class SecondFragment extends Fragment {

    private static final int REQUEST_ENABLE_BT = 1;

    private BluetoothSocket mmSocket = null;
    private BluetoothDevice mmDevice = null;

    protected static final String PREFS_FILE = "device_id.xml";
    protected static final String PREFS_DEVICE_ID = "device_id";
    private FragmentSecondBinding binding;


    private static final String TAG = "BluetoothCommActivity";
    private static final UUID SERVICE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // Use the same UUID as the connected device
    private OutputStream outputStream;
    private InputStream inputStream;


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


        BluetoothDevice device_ = bluetoothAdapter.getRemoteDevice("E0:08:71:51:73:40");

        binding.connectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
                if (!pairedDevices.isEmpty()) {
                    for (BluetoothDevice device : pairedDevices) {
                        // Assume you know the device's name or address
                        if (device.getAddress().equals("E0:08:71:51:73:40")) {
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
                if (!message.isEmpty()) {
                    try {
                        sendMessage(message);
                        Toast.makeText(context, "Message sent", Toast.LENGTH_SHORT).show();
                    } catch (IOException e) {
                        binding.statusTextView.setText("Failed to send message: " + e.getMessage());
                    }
                } else {
                    Toast.makeText(context, "Message is empty", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


    private void connectToDevice() throws IOException {
        if (ActivityCompat.checkSelfPermission(getContext(), android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        UUID uuid = mmDevice.getUuids()[0].getUuid(); // Replace with the correct UUID
        mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}