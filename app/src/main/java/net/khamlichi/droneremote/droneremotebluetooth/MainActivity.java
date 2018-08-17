package net.khamlichi.droneremote.droneremotebluetooth;

import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    TextView mDisplayTextView;
    TextView mDisplayTextView2;
    Button mClientButton;
    Button mServerButton;



    public static final String TAG = "MainActivity";

    private Handler mHandler; //Server handler

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mClientButton = findViewById(R.id.button_connect);
        mServerButton = findViewById(R.id.button_server);
        mDisplayTextView = findViewById(R.id.tv_display);
        mDisplayTextView2 = findViewById(R.id.tv_display2);
        PendingIntent mPermissionIntent = PendingIntent.getBroadcast(this,0, new Intent(Constants.ACTION_USB_PERMISSION), 0);
        IntentFilter filter = new IntentFilter(Constants.ACTION_USB_PERMISSION);
        UsbManager usbMan = (UsbManager) getSystemService(Context.USB_SERVICE);
        //Creation du service qui intercepte la réponse de l’accessoire
        Intent receiverIntent = registerReceiver(UsbBroadcastReceiver.getInstance(usbMan), filter);
        UsbAccessory[] accessoryList = usbMan.getAccessoryList();
        if (accessoryList == null) {
            //Pas d’accessoire USB connecté
            Toast.makeText(this.getApplicationContext(), "No connected drone", Toast.LENGTH_LONG).show();
        }

        else {
            //On demande la connection
            usbMan.requestPermission(accessoryList[0], mPermissionIntent);
            Toast.makeText(this.getApplicationContext(), "Drone found", Toast.LENGTH_LONG).show();
        }


    }

    public void onClickClient(View view) {
        mServerButton.setText("");
        mServerButton.setEnabled(false);
        mClientButton.setText(R.string.client_connecting);
        connect();
        mDisplayTextView.setText(R.string.waiting_server_connection);
    }

    public void onClickServer(View view) {
        initHandler();
        mClientButton.setText("");
        mClientButton.setEnabled(false);
        mServerButton.setText(R.string.server_connecting);

        acceptAndListen();
        mDisplayTextView.setText(R.string.waiting_client_connection);



    }

    private void acceptAndListen() {
        Thread acceptThread = new AcceptThread();
        acceptThread.start();

    }
    private void listen() {
        ServerConnectedThread listenThread = new ServerConnectedThread();
        listenThread.start();
    }



    public void connect() {
        try {
            Thread connectThread = new ConnectThread();
            connectThread.start();

        } catch (Exception e) {
            System.out.print(e);
        }
    }


    public void initHandler() {
        mHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                byte[] readBuf = (byte[]) msg.obj;
                String message = new String(readBuf).trim();

                sendCommandToUsb(message);
            }
        };
    }

    private void sendCommandToUsb(String message) {


        try {
            double distance = Double.parseDouble(message);
            mDisplayTextView2.setText("Distance received: " + message);
            if (distance > 2.0) {
                sendCommandToUsb(Constants.FORWARD);
            }

            else {
                sendCommandToUsb(Constants.HOVER);
            }

        }


        catch(NumberFormatException e) {
            mDisplayTextView.setText("Command received: " + message); //not a double

            if (UsbBroadcastReceiver.getInstance() != null)
                UsbBroadcastReceiver.getInstance().sendToAccessory(message);
        }



    }




    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread(BluetoothAdapter adapter) {
            // Use a temporary object that is later assigned to mmServerSocket
            // because mmServerSocket is final.
            BluetoothServerSocket tmp = null;
            try {
                // MY_UUID is the app's UUID string, also used by the client code.
                tmp = adapter.listenUsingRfcommWithServiceRecord(BluetoothService.NAME, BluetoothService.SERVICE_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket's listen() method failed", e);
            }
            mmServerSocket = tmp;
        }

        public AcceptThread() {
            this(BluetoothService.getAdapter());
        }

        public void run() {
            BluetoothSocket socket = null;
            // Keep listening until exception occurs or a socket is returned.
            while (true) {
                try {
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    Log.e(TAG, "Socket's accept() method failed", e);
                    break;
                }

                if (socket != null) {
                    // A connection was accepted. Perform work associated with
                    // the connection in a separate thread.
                    BluetoothService.setSocket(socket);
                    connectionSuccessful();
                    try {
                        mmServerSocket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }

        public void connectionSuccessful() {
            runOnUiThread(new Runnable() {
                public void run() {
                    mDisplayTextView.setText(R.string.connection_succeeded);
                    listen();
                }
            });
        }

        // Closes the connect socket and causes the thread to finish.
        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }



    private class ServerConnectedThread extends Thread {

        private BluetoothSocket mmSocket;
        private InputStream mmInStream;
        private byte[] mmBuffer; // mmBuffer store for the stream

        public ServerConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;

            // Get the input and output streams; using temp objects because
            // member streams are final.
            try {
                tmpIn = mmSocket.getInputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating input stream", e);
            }

            mmInStream = tmpIn;
            mmBuffer = new byte[1024];
        }

        public ServerConnectedThread() {
            this(BluetoothService.getSocket());
        }
        public void run() {


            int numBytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs.
            while (true) {
                try {
                    // Read from the InputStream.
                    numBytes = mmInStream.read(mmBuffer);
                    mmBuffer[numBytes] = '\0';
                    for (int i = numBytes + 1; i < 1024; i++) { //TODO messy
                        mmBuffer[i] = ' ';
                    }
                    // Send the obtained bytes to the UI activity.
                    Message readMsg = mHandler.obtainMessage(
                            0, numBytes, -1,
                            mmBuffer);
                    readMsg.sendToTarget();
                } catch (IOException e) {
                    Log.d(TAG, "Input stream was disconnected", e);
                    break;
                }
            }
        }


        // Call this method from the main activity to shut down the connection.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
            }
        }
    }

    private class ConnectThread extends Thread {
        private final BluetoothDevice mmDevice;
        private final BluetoothSocket mmSocket;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
            BluetoothSocket tmp = null;
            mmDevice = device;

            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID string, also used in the server code.
                tmp = mmDevice.createRfcommSocketToServiceRecord(BluetoothService.SERVICE_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }

        public ConnectThread() {
            this(BluetoothService.getAdapter().getBondedDevices().iterator().next());
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            BluetoothService.getAdapter().cancelDiscovery();

            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();
                BluetoothService.setSocket(mmSocket);
            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    Log.e(TAG, "Could not close the client socket", closeException);
                }
                return;
            }

            // The connection attempt succeeded. Perform work associated with
            // the connection in a separate thread.

            connectionSuccessful();
        }

        public void connectionSuccessful() {
            runOnUiThread(new Runnable() {
                public void run() {
                    Intent intent = new Intent(MainActivity.this, ClientActivity.class);
                    startActivity(intent);
                }
            });
        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the client socket", e);
            }
        }
    }





}
