package org.rivertea.traffician;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import androidx.annotation.Nullable;

public class TrafficianService extends Service {
    private static final String TAG = "Traffician";
    private static final String CHANNEL_ID = "Traffician";
    private final TrafficianServer trafficianServer = new TrafficianServer();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
