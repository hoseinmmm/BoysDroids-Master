package com.example.android.rescuerobot;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Handler;
import android.os.Vibrator;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

import static android.R.attr.switchMinWidth;
import static android.R.attr.value;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private IntentFilter filter;
    private ResponseReceiver broadcastReceiver;
    private Intent broadcastIntent;
    private ImageView uploadBackPhoto;

    private ToneGenerator toneGenerator;


    public double lat, lng;

    public Location loc1 = new Location("");

    private LocationManager manager;
    private LocationListener listener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Create broadcast Intent to talk to Network Server Service
        broadcastIntent = new Intent();

        // Register Service Broadcast Receiver to receive broadcast from Network Service
        registerBroadcastReceiver();

        uploadBackPhoto = (ImageView) findViewById(R.id.backIV);

        final Button sendForwardButton = (Button) findViewById(R.id.send_forward_tx);
        sendForwardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMoveForwardMsg();
                // broadcastIntent.putExtra(SEND_PACKET, editText.getText().toString());
            }
        });
        final Button sendBackwardButton = (Button) findViewById(R.id.send_backward_tx);
        sendBackwardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMoveBackwardMsg();
            }
        });
        final Button sendRightButton = (Button) findViewById(R.id.send_right_tx);
        sendRightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMoveRightMsg();
            }
        });
        final Button sendLeftButton = (Button) findViewById(R.id.send_left_tx);
        sendLeftButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMoveLeftMsg();
            }
        });

        toneGenerator = new ToneGenerator(AudioManager.STREAM_ALARM,100);


        //]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]
        manager = (LocationManager) getSystemService(LOCATION_SERVICE);
        listener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {


                lat = location.getLatitude();
                lng = location.getLongitude();

            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {
                Intent i = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(i);

            }
        };
        configure_loc();


    }



    void configure_loc() {
        // first check for permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.INTERNET}
                        , 10);
            }
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                    1
            );
            //return;
        }
    }


   // TextView t1 = (TextView) findViewById(R.id.textView);

    void obstacle() {
        Log.d(TAG,"ready to show!! obstacle");
        TextView t1 = (TextView) findViewById(R.id.textView);

        t1.setText("Obstacle is detected!!!");

        toneGenerator.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD,1000);
        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(1500);



    }


    void showlocation(String loc)
    {

        Log.d(TAG,"ready to show location!!");


        String[] splited = loc.split(" ");

        double lat_start = 0;
        double lon_start = 0;
        int inc1 = 0;
        try {
            lat_start = Double.parseDouble(splited[0]);
            lon_start = Double.parseDouble(splited[1]);
            inc1 = Integer.parseInt(splited[2]);


            if(inc1<5)
            {
                Log.d("a", "inclined");
            }

        } catch (NumberFormatException e) {
            Log.d("a", "exception");
        }
        Location locationA = new Location("point A");
        locationA.setLatitude(lat_start);
        locationA.setLongitude(lon_start);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        manager.requestLocationUpdates("gps", 0, 0, listener);
        loc1.setLatitude(lat);
        loc1.setLongitude(lng);

        double distance;
        distance = locationA.distanceTo(loc1);
        TextView t2 = (TextView) findViewById(R.id.textView2);
       // t2.setText("Distance of robot from base is: " + String.valueOf(distance));

       // showinclination(inc1);


    }

    void showinclination(int inc)
    {

        TextView t3 = (TextView) findViewById(R.id.textView3);
        String incstr = " ";
        switch (inc) {
            case 1:
                incstr = "Downward";
                break;
            case 2:
                incstr = "Upward";
                break;
            case 3:
                incstr = "Right";
                break;
            case 4:
                incstr = "Left";
                break;
            case 5:
                incstr = "Uninclined";
                break;
            default:
                incstr = String.valueOf(inc);
                break;
        }

        t3.setText("Inclination is: " + incstr);

    }


    // Define Broadcast receiver class to receive received network packets from the Network Server Service
    public class ResponseReceiver extends BroadcastReceiver {

        // Define the Action to use with the Intent filter
        public static final String RECEIVED_NEW_NETWORK_MESSAGE = "RECEIVED_NEW_NETWORK_MESSAGE";
        public static final String RECEIVED_PICTURE = "RECEIVED_PICTURE";
        public static final String RECEIVED_HEART_BEAT = "RECEIVED_HEART_BEAT";
        public static final String OBSTACLE = "OBSTACLE";
        public static final String LOCATION_RX = "LOCATION_RX";
        public static final String INCLINATION_RX = "INCLINATION_RX";
        //public static final String RECEIVED_OBSTACLE = "RECEIVED_OBSTACLE";



        // Read data sent by the IntentService
        @Override
        public void onReceive(Context context, Intent intent) {

            // We need to check what type of the received broadcast message is
            String action = intent.getAction();
            //Log.d(TAG,"Received broadcast: "+action);
            if (action.equals(RECEIVED_NEW_NETWORK_MESSAGE)) {
                String msg = intent.getStringExtra(NetworkServerService.RECEIVED_MESSAGE);

            }
            if (action.equals(RECEIVED_PICTURE)) {
                TextView t1 = (TextView) findViewById(R.id.textView);
               t1.setText("No Obstacle!!!!");
                String url = intent.getStringExtra("ImageURL");
                Log.d("ServiceBroadcastRx","Picture url in main activity: "+url);
                File imgFile = new File(url);
                if (imgFile.exists()) {
                    Bitmap myImg = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                    int width = myImg.getWidth();
                    int height = myImg.getHeight();
                    Log.d(TAG,"Width: "+width+" Height: "+height);
                    uploadBackPhoto.setImageBitmap(myImg);
                }
            }
            if (action.equals(RECEIVED_HEART_BEAT)) {
                Log.d(TAG,"Received heart beat");
            }
            if (action.equals(OBSTACLE))
            {
                Log.d(TAG,"Received OBSTACLE in main activity");
                obstacle();
            }
            if (action.equals(LOCATION_RX))
            {
                Log.d(TAG,"Received LOCATION");
                String location = intent.getStringExtra("location");
                showlocation(location);
            }
            if (action.equals(INCLINATION_RX))
            {
                Log.d(TAG,"Received inclination");
                int incl = intent.getIntExtra("inclination",5);
                showinclination(incl);
            }
        }
    }

    private void registerBroadcastReceiver() {
        // Register Local Service Broadcast Receiver
        filter = new IntentFilter();
        // Define type of Broadcast to filter
        filter.addAction(ResponseReceiver.RECEIVED_NEW_NETWORK_MESSAGE);
        filter.addAction(ResponseReceiver.RECEIVED_PICTURE);
        filter.addAction(ResponseReceiver.RECEIVED_HEART_BEAT);
        filter.addAction(ResponseReceiver.LOCATION_RX);
        filter.addAction(ResponseReceiver.INCLINATION_RX);
        filter.addAction(ResponseReceiver.OBSTACLE);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        broadcastReceiver = new ResponseReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, filter);
    }

    /*
    Network API starts here
     */

    private void sendMoveForwardMsg(){
        broadcastIntent.setAction(NetworkServerService.ServiceBroadcastReceiver.SEND_FORWARD_TX);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent);
    }
    private void sendMoveBackwardMsg(){
        broadcastIntent.setAction(NetworkServerService.ServiceBroadcastReceiver.SEND_BACKWARDS_TX);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent);
    }
    private void sendMoveRightMsg(){
        broadcastIntent.setAction(NetworkServerService.ServiceBroadcastReceiver.SEND_RIGHT_TX);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent);
    }
    private void sendMoveLeftMsg(){
        broadcastIntent.setAction(NetworkServerService.ServiceBroadcastReceiver.SEND_LEFT_TX);
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent);
    }

    /*
    Network API ends here
     */



     /*
    Override methods related to the App's Option Menu
     */

    // Inflate Option Menu methods
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.settings_menu_id:
                Intent intent = new Intent(this, ServerNetworkSettingsActivity.class);
                startActivity(intent);
                return true;
        }
        return false;
    }


    // Use onPause method to unregister the Broadcast receiver to avoid "leaks"
    @Override
    protected void onPause() {
        /* we should unregister BroadcastReceiver here*/
        unregisterReceiver(broadcastReceiver);
        super.onPause();
    }

    // Use onResume method to re-register the Broadcast receiver

    @Override
    protected void onResume() {
		/* we should register BroadcastReceiver here*/
        registerReceiver(broadcastReceiver, filter);
        super.onResume();
    }


}
