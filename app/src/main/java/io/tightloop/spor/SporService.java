package io.tightloop.spor;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class SporService extends Service implements LocationListener {
    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyyMMddHHmmss'Z'", Locale.US);
    private static final int NOTIFICATION_ID = 1725186441;

    public double elevation = Double.NaN;
    public double lat = Double.NaN;
    public double lng = Double.NaN;

    public class SporServiceBinder extends Binder {
        public SporService getService() {
            return SporService.this;
        }
    }

    private LocationManager locationManager;
    private SporServiceBinder bind = new SporServiceBinder();
    private DataOutputStream currentFile = null;

    public SporService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_NOT_STICKY;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onCreate() {
        super.onCreate();
        startForeground(NOTIFICATION_ID, getNotification());
        locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        activate();
    }

    private void activate() {
        // Ensure we have permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e("SporService", "Missing location permissions");
            return;
        }
        File storageDir = new File(Environment.getExternalStorageDirectory().getAbsoluteFile(), "spor");
        if (storageDir.mkdirs()) {
            Log.i("SporService", "Created storage directory "+storageDir);
        }
        String dateString = DATE_FMT.format(new Date());
        File path = new File(storageDir, String.format("%s-%s.spor", getString(R.string.app_name), dateString));
        if (currentFile == null) {
            try {
                currentFile = new DataOutputStream(new FileOutputStream(path));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, TimeUnit.SECONDS.toMillis(5), 5, this);
    }

    private void deactivate() {
        if (currentFile != null) {
            locationManager.removeUpdates(this);
            try {
                currentFile.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                currentFile = null;
            }
        }
    }
    @RequiresApi(api = Build.VERSION_CODES.O)
    private Notification getNotification() {
        NotificationChannel channel = new NotificationChannel("channel_01", "My Channel", NotificationManager.IMPORTANCE_DEFAULT);
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
        Notification.Builder builder = new Notification.Builder(getApplicationContext(), "channel_01").setAutoCancel(true);
        return builder.build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        deactivate();
        lat = lng = elevation = Double.NaN;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return bind;
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        Log.i("SporService", "Received location update");

        if (currentFile == null) {
            return;
        }

        lat = location.getLatitude();
        lng = location.getLongitude();
        elevation = location.getAltitude();

        long timestamp = location.getTime();

        try {
            currentFile.writeDouble(lat);
            currentFile.writeDouble(lng);
            currentFile.writeDouble(elevation);
            currentFile.writeLong(timestamp);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(@NonNull String provider) {
    }

    @Override
    public void onProviderDisabled(@NonNull String provider) {
    }
}
