package com.example.android.rescuerobot;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import static android.R.attr.filter;
import static android.os.Build.VERSION_CODES.M;
import static com.example.android.rescuerobot.MainActivity.ResponseReceiver.RECEIVED_NEW_NETWORK_MESSAGE;

/**
 * Created by colon on 3/17/17.
 */

public class ServerNetworkSettingsActivity extends AppCompatActivity {

    private static final String TAG = "NetworkSettingsActivity";
    private IntentFilter filter;
    private ResponseReceiver broadcastReceiver;
    private WifiManager wifiManager;
    private String ssid;
    private int rssi;
    private String localIPAddress;
    private String remoteIP;
    private boolean isConnected;
    private TimerTask timerTask;
    private int activityRefreshRate = 2000;
    private Intent netServerIntent;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server_network_settings);

        // Define Intent filter and register broadcast receiver
        filter = new IntentFilter(ResponseReceiver.RECEIVED_NEW_NETWORK_MESSAGE);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        broadcastReceiver = new ResponseReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, filter);

        // First check if we have network connection
        final ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        // Define/Initiate Network Server Service Intent
        netServerIntent = new Intent(this, NetworkServerService.class);

        final Button startNetService = (Button) findViewById(R.id.start_network_server_button);
        startNetService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startService(netServerIntent);
            }
        });


        Timer timerObj = new Timer();
        timerTask = new TimerTask() {
            @Override
            public void run() {
                NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
                if (isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting()) {
                    // Make sure connection is WIFI
                    if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                        Log.i(TAG, "Device is connected to Wi-Fi network");

                        // This run on UI thread method is needed to update the Views
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                refreshNetworkStatusViews();
                            }
                        });
                    } else {
                        Log.d(TAG, "Device connected to mobile network");
                    }
                } else {
                    // This run on UI thread method is needed to update the Views
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            networkStatusDefaultViews();
                        }
                    });
                }

            }
        };
        timerObj.schedule(timerTask, 0, activityRefreshRate);

    }

    // Define Broadcast receiver class to get remote IP address from the Network Server Service
    public class ResponseReceiver extends BroadcastReceiver {

        // Define the Action to use with the Intent filter
        public static final String RECEIVED_NEW_NETWORK_MESSAGE = "RECEIVED_NEW_NETWORK_MESSAGE";


        // Read data sent by the IntentService
        @Override
        public void onReceive(Context context, Intent intent) {

            // We need to check what type of the received broadcast message is
            String action = intent.getAction();
            if (action.equals(RECEIVED_NEW_NETWORK_MESSAGE)) {
                remoteIP = intent.getStringExtra(NetworkServerService.REMOTE_IP_ADDRESS);
            }

        }
    }


    private int getRSSI(WifiInfo wifiInfo) {
        return wifiInfo.getRssi();
    }

    private String getSSID(WifiInfo wifiInfo) {
        return wifiInfo.getSSID();
    }

    private String getLocalIPAddress(WifiInfo wifiInfo) {

        int ipAddress = wifiInfo.getIpAddress();
        return String.format(Locale.getDefault(), "%d.%d.%d.%d",
                (ipAddress & 0xff), (ipAddress >> 8 & 0xff),
                (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
    }

    public void networkStatusDefaultViews() {

        TextView textViewNetStatus = (TextView) findViewById(R.id.network_status_textview);
        textViewNetStatus.setText("No Connection");
        TextView textViewSSID = (TextView) findViewById(R.id.network_ssid_textview);
        textViewSSID.setText("N/A");
        TextView textViewRSSI = (TextView) findViewById(R.id.network_rssi_textview);
        textViewRSSI.setText("N/A");
        TextView textViewLocalIP = (TextView) findViewById(R.id.local_ip_textview);
        textViewLocalIP.setText("N/A");
        TextView textViewRemoteIP = (TextView) findViewById(R.id.remote_ip_textview);
        textViewRemoteIP.setText("N/A");

    }

    private void refreshNetworkStatusViews() {
        // WIFIManager must use Application Context and not Activity Context
        wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        WifiInfo w = wifiManager.getConnectionInfo();

        ssid = getSSID(w);
        rssi = getRSSI(w);
        localIPAddress = getLocalIPAddress(w);

        Log.d(TAG, "Wifi SSID: " + ssid);
        Log.d(TAG, "Wifi RSSI: " + rssi);
        Log.d(TAG, "Wifi Local IP:" + localIPAddress);
        TextView textViewNetStatus = (TextView) findViewById(R.id.network_status_textview);
        textViewNetStatus.setText("Connected to Wi-Fi");
        TextView textViewSSID = (TextView) findViewById(R.id.network_ssid_textview);
        textViewSSID.setText(ssid);
        TextView textViewRSSI = (TextView) findViewById(R.id.network_rssi_textview);
        textViewRSSI.setText(String.valueOf(rssi) + " dBm");
        TextView textViewLocalIP = (TextView) findViewById(R.id.local_ip_textview);
        textViewLocalIP.setText(localIPAddress);
        TextView textViewRemoteIP = (TextView) findViewById(R.id.remote_ip_textview);
        if (remoteIP != null) {
            textViewRemoteIP.setText(remoteIP);
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        /* we should unregister BroadcastReceiver here*/
        unregisterReceiver(broadcastReceiver);
        timerTask.cancel(); // stop timer when View is paused
    }

    @Override
    protected void onResume(){
        super.onResume();
        /* we should register BroadcastReceiver here*/
        registerReceiver(broadcastReceiver,filter);
        timerTask.run();
    }


}
