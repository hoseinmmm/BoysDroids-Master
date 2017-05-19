package com.example.android.rescuerobot;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import android.media.MediaPlayer;

import android.support.annotation.Nullable;

public class ServiceActivity extends Service {

    private MediaPlayer Alert;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public ServiceActivity() {
        super();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        //return super.onStartCommand(intent, flags, startId);

        Alert.setLooping(true);
        Alert.start();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Alert.stop();
    }
}

