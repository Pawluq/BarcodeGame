package com.pawel.myapplication;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.UUID;

public class ArduinoCommunication {
    private static final String BT_ADDRESS = "98:D3:61:FD:39:EB";  // Placeholder; replace with actual Bluetooth address
    private static final UUID BT_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");  // Unused as of now
    private static final String BT_TAG = "Bluetooth";

    private BluetoothAdapter adapter;
    private BluetoothSocket socket;
    private BluetoothDevice device;

    private InputStream input;  // Unused as of now
    private OutputStream output;

    public void initialize() {
        Log.wtf(BT_TAG, "Attempting to initialize bluetooth");
        adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null || !adapter.isEnabled()) {
            Log.wtf(BT_TAG, "ERROR - No bluetooth device found.");
        } else {
            connect();
        }
    }

    private void connect() {
        BluetoothSocket tmp = null;
        device = adapter.getRemoteDevice(BT_ADDRESS);
        Log.wtf(BT_TAG, "Connecting to: " + device.getName());

        try {
            // tmp = device.createRfcommSocketToServiceRecord(BT_UUID);
            tmp = (BluetoothSocket) device.getClass().getMethod("createRfcommSocket", new Class[] {int.class}).invoke(device, 1);
            socket = tmp;
            socket.connect();
        } catch (IOException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            Log.wtf(BT_TAG, "ERROR - Failed to connect to " + device.getName());
            e.printStackTrace();
            return;
        }

        Log.wtf(BT_TAG, "Attempting to initialize input and output streams");
        InputStream tmpInput = null;
        OutputStream tmpOutput = null;

        try {
            tmpInput = socket.getInputStream();
            tmpOutput = socket.getOutputStream();
            // output.flush();
        } catch (IOException e) {
            Log.wtf(BT_TAG, "ERROR - Failed to initialize input and output streams");
        }
        input = tmpInput;
        output = tmpOutput;
    }

    public void send(String colorCode) {
        if (output != null) {
            byte[] message = colorCode.getBytes();
            try {
                output.write(message);
                Log.wtf(BT_TAG, "Sent following message: " + colorCode);
            } catch (IOException e) {
                Log.wtf(BT_TAG, "ERROR - Failed to send message");
            }
        }
    }

    public void disconnect() {
        try {
            output.flush();
            socket.close();
            Log.wtf(BT_TAG, "Disconnected from " + device.getName());
        } catch (IOException e) {
            Log.wtf(BT_TAG, "ERROR - Failed to disconnect");
        }
    }
}
