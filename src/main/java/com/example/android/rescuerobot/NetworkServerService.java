package com.example.android.rescuerobot;

import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;
import android.provider.ContactsContract;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;

import static android.R.attr.data;
import static android.R.attr.filter;

/**
 * Created by colon on 3/16/17.
 */

public class NetworkServerService extends IntentService {

    private static final String TAG = "NetworkServerService";
    // Server's listening port
    private static final int serverPort = 28800;
    private IntentFilter filter;
    private ServiceBroadcastReceiver broadcastReceiver;
    private Intent broadcastIntent;
    Toast toast;

    //private PrintWriter dataOut;
    private DataOutputStream dataOut;
    private DataInputStream dataIn;

    // Define broadcast Intent TAG name
    public static final String RECEIVED_MESSAGE = "RECEIVED_MESSAGE";
    public static final String REMOTE_IP_ADDRESS = "REMOTE_IP_ADDRESS";
    File file;

    // Default constructor that calls super with the name of the "Service"/class
    public NetworkServerService() {
        super("NetworkServerService");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "Started Service onCreate() method");
        // Display a toast message when the IntentService is created
        toast = Toast.makeText(this, "Network Server Service Started",
                Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();

        // Create Intent to pass received messages to the Main Activity through local broadcast
        broadcastIntent = new Intent();

        // Register Service Broadcast Receiver
        registerBroadcastReceiver();

        // Get directory to store pictures
        file = new File(Environment.getExternalStorageDirectory() + "/" + "0_pic.jpg");

    }

    @Override
    protected void onHandleIntent(final Intent intent) {

        try (
                ServerSocket serverSocket = new ServerSocket(serverPort);
                Socket clientSocket = serverSocket.accept();
                DataOutputStream dataOutputStream = new DataOutputStream(clientSocket.getOutputStream());
                DataInputStream dataInputStream = new DataInputStream(clientSocket.getInputStream());
                ObjectInputStream ois = new ObjectInputStream(clientSocket.getInputStream());
        ) {
            dataOut = dataOutputStream;
            dataIn = dataInputStream;
            int msgID;
            while (true) {
                try {
                    msgID = dataIn.readInt();

                    switch (msgID) {
                        case 100:
                            Log.d(TAG, "RECEIVED from network " + msgID + ": HEART_BEAT");
                            broadcastIntent.setAction(MainActivity.ResponseReceiver.RECEIVED_HEART_BEAT);
                            sendBroadcast(broadcastIntent);
                            break;
                        case 200:
                            Log.d(TAG, "RECEIVED from network " + msgID + ": obstacle");
                            broadcastIntent.setAction(MainActivity.ResponseReceiver.OBSTACLE);
                            sendBroadcast(broadcastIntent);
                            break;
                        case 300:
                            Log.d(TAG, "RECEIVED from network " + msgID + ": location");
                            try {
                                byte[] buffer1 = (byte[]) ois.readObject();
                                String locationstr = buffer1.toString();
                                broadcastIntent.setAction(MainActivity.ResponseReceiver.LOCATION_RX);
                                broadcastIntent.putExtra("location",locationstr);
                                sendBroadcast(broadcastIntent);
                            } catch (ClassNotFoundException e) {
                                Log.e(TAG, "IOException error: ", e);
                            }

                            break;

                        case 400:
                            Log.d(TAG, "RECEIVED from network " + msgID + ": inclination");
                            try {
                                byte[] buffer1 = (byte[]) ois.readObject();
                                int i2 = buffer1[0] & 0xFF;
                                broadcastIntent.setAction(MainActivity.ResponseReceiver.INCLINATION_RX);
                                broadcastIntent.putExtra("inclination",i2);
                                sendBroadcast(broadcastIntent);
                            } catch (ClassNotFoundException e) {
                                Log.e(TAG, "IOException error: ", e);
                            }
                            break;

                        case 105:
                            Log.d(TAG, "RECEIVED from network "+ msgID + ": PICTURE");
                            try {
                                byte[] buffer = (byte[]) ois.readObject();
                                FileOutputStream fos = new FileOutputStream(file);
                                fos.write(buffer);
                                broadcastIntent.setAction(MainActivity.ResponseReceiver.RECEIVED_PICTURE);
                                broadcastIntent.putExtra("ImageURL",file.getPath());
                                sendBroadcast(broadcastIntent);

                            } catch (ClassNotFoundException e) {
                                Log.e(TAG, "IOException error: ", e);
                            }
                            break;
                        default:
                            Log.d(TAG,"from network Message ID Not Found" + msgID);
                            break;

                    }
                } catch (IOException e){
                    Log.e(TAG, "IOException error: ", e);
                }

                broadcastIntent.setAction(MainActivity.ResponseReceiver.RECEIVED_NEW_NETWORK_MESSAGE);
                broadcastIntent.putExtra(REMOTE_IP_ADDRESS, clientSocket.getRemoteSocketAddress().toString().replace("/", ""));
                sendBroadcast(broadcastIntent);
            }
            // If connection is closed, we need to reset the remote IP address
         //   broadcastIntent.putExtra(REMOTE_IP_ADDRESS, "Remote Client Not Connected");
          //  LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent);
        } catch (IOException e) {
            Log.d(TAG, "Exception caught when trying to listen on port "
                    + serverPort + " or listening for a connection");
        }
    }

    private void registerBroadcastReceiver() {
        // Register Local Service Broadcast Receiver
        filter = new IntentFilter();
        // Define type of Broadcast to filter
        filter.addAction(ServiceBroadcastReceiver.SEND_FORWARD_TX);
        filter.addAction(ServiceBroadcastReceiver.SEND_BACKWARDS_TX);
        filter.addAction(ServiceBroadcastReceiver.SEND_RIGHT_TX);
        filter.addAction(ServiceBroadcastReceiver.SEND_LEFT_TX);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        broadcastReceiver = new ServiceBroadcastReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, filter);
    }

    public class ServiceBroadcastReceiver extends BroadcastReceiver {

        // Define the Action to use with the Intent filter
        public static final String SEND_FORWARD_TX = "SEND_FORWARD_TX";
        public static final String SEND_BACKWARDS_TX = "SEND_BACKWARDS_TX";
        public static final String SEND_RIGHT_TX = "SEND_RIGHT_TX";
        public static final String SEND_LEFT_TX = "SEND_LEFT_TX";


        // Read data sent by the IntentService
        @Override
        public void onReceive(Context context, Intent intent) {

            // We need to check what type of the received broadcast message it is
            String action = intent.getAction();
            if (action.equals(SEND_FORWARD_TX)) {
                Log.d("ServiceBroadcastRx", "Received move forward broadcast");
               // String msg = intent.getStringExtra(MainActivity.SEND_PACKET);
             //   double msg = intent.getDoubleExtra(MainActivity.SEND_PACKET,0);
                sendCommand(101);

                //remoteIP = intent.getStringExtra(NetworkServerService.REMOTE_IP_ADDRESS);
            }
            if (action.equals(SEND_BACKWARDS_TX)) {
                Log.d("ServiceBroadcastRx", "Received move backward broadcast");
                // String msg = intent.getStringExtra(MainActivity.SEND_PACKET);
                //   double msg = intent.getDoubleExtra(MainActivity.SEND_PACKET,0);
                sendCommand(103);
            }
            if (action.equals(SEND_RIGHT_TX)) {
                Log.d("ServiceBroadcastRx", "Received move right broadcast");
                // String msg = intent.getStringExtra(MainActivity.SEND_PACKET);
                //   double msg = intent.getDoubleExtra(MainActivity.SEND_PACKET,0);
                sendCommand(102);
            }
            if (action.equals(SEND_LEFT_TX)) {
                Log.d("ServiceBroadcastRx", "Received move left broadcast");
                sendCommand(104);
            }


        }
    }


    private void sendCommand(int data) {

        switch (data) {
            case 101:
                try {
                    dataOut.writeInt(101);
                    //Log.d(TAG,"Packet data: "+data);
                   // Log.d(TAG,"Packet size: "+dataOut.size());
                    dataOut.flush();

                } catch (IOException e) {
                    Log.e(TAG,"IO Exception Error");
                }
                break;
            case 102:
                try {
                    dataOut.writeInt(102);
                   // Log.d(TAG,"Packet data: "+data);
                  //  Log.d(TAG,"Packet size: "+dataOut.size());
                    dataOut.flush();

                } catch (IOException e) {
                    Log.e(TAG,"IO Exception Error");
                }
                break;
            case 103:
                try {
                    dataOut.writeInt(103);
                   // Log.d(TAG,"Packet data: "+data);
                   // Log.d(TAG,"Packet size: "+dataOut.size());
                    dataOut.flush();

                } catch (IOException e) {
                    Log.e(TAG,"IO Exception Error");
                }
                break;
            case 104:
                try {
                    dataOut.writeInt(104);
                 //   Log.d(TAG,"Packet data: "+data);
                   // Log.d(TAG,"Packet size: "+dataOut.size());
                    dataOut.flush();

                } catch (IOException e) {
                    Log.e(TAG,"IO Exception Error");
                }
                break;
            default:
                Log.d(TAG,"wrong command to send");
                break;
        }



    }
}
