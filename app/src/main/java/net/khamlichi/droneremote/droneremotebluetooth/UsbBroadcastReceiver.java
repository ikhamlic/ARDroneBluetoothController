package net.khamlichi.droneremote.droneremotebluetooth;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by ikhamlic on 15/05/18.
 */

class UsbBroadcastReceiver extends BroadcastReceiver {
    private UsbAccessory mAccessory;
    private ParcelFileDescriptor mFileDescriptor;
    private InputStream mInputStream;
    private OutputStream mOutputStream;

    private Context appContext;
    private static UsbBroadcastReceiver instance;
    private UsbManager usbMan;

    public UsbBroadcastReceiver(UsbManager usbMan) {
        super();
        this.usbMan = usbMan;
    }
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        appContext = context;

        if (Constants.ACTION_USB_PERMISSION.equals(action)) {
            //Le drone a accepte la connection USB
            synchronized (this) {
                mAccessory = intent.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
                //Je me souviens du socket
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    if (mAccessory != null){
                        mFileDescriptor = usbMan.openAccessory(mAccessory);
                        FileDescriptor fd = mFileDescriptor.getFileDescriptor();
                        mInputStream = new FileInputStream(fd);
                        mOutputStream = new FileOutputStream(fd);
                    }
                }
                else {
                    Log.d("USB onReceive", "permission denied for accessory " + mAccessory);
                }
            }
        }
    }

    public void sendToAccessory(final String message) {
        if (mFileDescriptor != null) {
            new Thread(new Runnable() {
                public void run() {
                    try {
                        Log.d("USB", "Writing data: " + message);
                        mOutputStream.write(message.getBytes());
                        Log.d("USB", "Done writing: " + message);
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }
    }

    public static UsbBroadcastReceiver getInstance(UsbManager usbMan) {
        if (instance == null) {
            instance = new UsbBroadcastReceiver(usbMan);
        }

        return instance;
    }

    public static UsbBroadcastReceiver getInstance() {
        if (instance == null)
            throw new RuntimeException("No instance of UsbBroadcast receiver found");

        return instance;

    }
}
