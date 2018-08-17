package net.khamlichi.droneremote.droneremotebluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.net.Socket;
import java.util.UUID;

/**
 * Created by ikhamlic on 15/05/18.
 */

public class BluetoothService {

    private static BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private static BluetoothSocket socket; // Server-side or Client-side socket

    public static final String TAG = "BluetoothService";

    public static final UUID SERVICE_UUID = UUID.fromString("306c806e-5841-11e8-9c2d-fa7ae01bbebc");
    public static final String NAME = "DRONE";

    public static BluetoothSocket getSocket() {
        return socket;
    }

    public static void setSocket(BluetoothSocket pSocket) {
        socket = pSocket;
    }

    public static BluetoothAdapter getAdapter() {
        return bluetoothAdapter;
    }

    public static void setAdapter(BluetoothAdapter adapter) {
        bluetoothAdapter = adapter;
    }


    public static void write(String str) {
        byte[] bytes = str.getBytes();
        write(bytes);
    }

    // Call this from the main activity to send data to the remote device.
    public static void write(byte[] bytes) {
        try {
            BluetoothService.getSocket().getOutputStream().write(bytes); //TODO messy

        } catch (IOException e) {
            Log.e(TAG, "Error occurred when sending data", e);

        }
    }


}
