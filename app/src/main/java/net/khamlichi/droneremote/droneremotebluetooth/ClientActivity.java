package net.khamlichi.droneremote.droneremotebluetooth;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.GeomagneticField;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class ClientActivity extends AppCompatActivity {

    private static final String TAG = "ClientActivity";

    private boolean isFollowing = false;
    private Thread followThread;
    private TextView mDisplayTextView;


    private Button followButton;
    private Location userLoc;
    private GeomagneticField geoField;
    private LocationManager locManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client);
        mDisplayTextView = findViewById(R.id.tv_display);


        followButton = (Button) findViewById(R.id.button_follow);

        locManager = (LocationManager) getApplicationContext().getSystemService(LOCATION_SERVICE);
    }

    public void sendToAccessory(String command) {
        new WriteBluetoothTask().execute(command);

    }

    public void onClickUp(View view) {
        sendToAccessory(Constants.UP);
    }

    public void onClickForward(View view) {
        sendToAccessory(Constants.FORWARD);
    }

    public void onClickTurnLeft(View view) {
        sendToAccessory(Constants.TURN_LEFT);
    }

    public void onClickTurnRight(View view) {
        sendToAccessory(Constants.TURN_RIGHT);
    }

    public void onClickHover(View view) {
        sendToAccessory(Constants.HOVER);
    }

    public void onClickMoveLeft(View view) {
        sendToAccessory(Constants.MOVE_LEFT);
    }

    public void onClickMoveRight(View view) {
        sendToAccessory(Constants.MOVE_RIGHT);
    }

    public void onClickBackward(View view) {
        sendToAccessory(Constants.BACKWARD);
    }

    public void onClickDown(View view) {
        sendToAccessory(Constants.DOWN);
    }

    public void onClickAdjust(View view) {
        sendToAccessory(Constants.ADJUST);
    }

    public void onClickTakeOff(View view) {
        sendToAccessory(Constants.TAKE_OFF);

    }

    public void onClickLand(View view) {
        sendToAccessory(Constants.LAND);
    }

    public void onClickEnd(View view) {
        sendToAccessory(Constants.END);
    }

    public void onClickEmergency(View view) {
        sendToAccessory(Constants.EMERGENCY);
    }

    public void onClickFollow(View view) {
        updateLocation();

        if (isFollowing) {
            isFollowing = false;
            followButton.setText(R.string.follow_me);
            followThread.interrupt();
        } else {
            isFollowing = true;
            followButton.setText(R.string.following_you);
            followThread = new FollowMeThread(Constants.INTERVAL_SEND);
            followThread.start();
        }


    }


    public class WriteBluetoothTask extends AsyncTask<String, Void, Void> {


        @Override
        protected Void doInBackground(String... strings) {
            BluetoothService.write(strings[0]);
            return null;
        }


    }

    public void updateLocation() {
        userLoc = getLastKnownLocation();
        Log.i(TAG, "lat= " + userLoc.getLatitude() + " // long= " + userLoc.getLongitude());
        geoField = new GeomagneticField(
                Double.valueOf(userLoc.getLatitude()).floatValue(),
                Double.valueOf(userLoc.getLongitude()).floatValue(),
                Double.valueOf(userLoc.getAltitude()).floatValue(),
                System.currentTimeMillis()
        );
    }

    private Location getLastKnownLocation() {
        locManager = (LocationManager) getApplicationContext().getSystemService(LOCATION_SERVICE);
        List<String> providers = locManager.getProviders(true);
        Location bestLocation = null;
        for (String provider : providers) {

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return null;
            }
            Location l = locManager.getLastKnownLocation(provider);

            if (l == null) {
                continue;
            }

            if (bestLocation == null || l.getAccuracy() < bestLocation.getAccuracy()) {
                // Found best last known location: %s", l);
                bestLocation = l;
            }
        }
        return bestLocation;
    }


    public float bearTo(Location dest) {
        float head = geoField.getDeclination(); // converts magnetic north into true north

        float bearing = userLoc.bearingTo(dest);


        if (bearing < 0)
            bearing = bearing + 360;

        float direction = bearing - head;


        // If the direction is smaller than 0, add 360 to get the rotation clockwise.
        if (direction < 0) {
            direction = direction + 360;
        }

        mDisplayTextView.setText("Heading: " + Float.toString(direction) + " degrees" );


        return direction;
    }


    public class FollowMeThread extends Thread {
        private int mmPeriodicity;

        public FollowMeThread(int periodicity) {
            mmPeriodicity = periodicity;
        }

        @Override
        public void run() {
            while(!isInterrupted()) {

                try {
                    Thread.sleep(mmPeriodicity);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                updateLocation();
                sendDistance();
            }

            Log.i(TAG, "FollowMe thread interrompu");
        }


        private void sendDistance() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    double distance = distanceToDrone();
                    send(String.valueOf(distance));
                    mDisplayTextView.setText("Distance sent: " + distance + "\nlat: " + userLoc.getLatitude() + "\nlongi: " + userLoc.getLongitude());

                }
            });
        }

        public void send(String command) {
            BluetoothService.write(command);

        }


        @TargetApi(Build.VERSION_CODES.LOLLIPOP) //TODO target kitkat devices too
        private double distanceToDrone() {
            WifiManager wifiMan = (WifiManager) ClientActivity.this.getApplicationContext().getSystemService(Context.WIFI_SERVICE); //TODO add as private members
            WifiInfo wifiInf = wifiMan.getConnectionInfo();


            int rssi = wifiInf.getRssi();
            int freqInMHz = wifiInf.getFrequency();
            double exp = (27.55 - (20 * Math.log10(freqInMHz)) + Math.abs(rssi)) / 20.0;

            double distance = Math.pow(10.0, exp);
            return distance;

        }
    }





}
