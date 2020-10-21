package io.tightloop.spor;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class MainActivity extends AppCompatActivity {
    private Timer timer;
    private SporService sporService;
    private boolean sporing = false;

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            String name = className.getClassName();
            if (name.endsWith("SporService")) {
                Log.i("main", "SporService connected");
                sporService = ((SporService.SporServiceBinder) service).getService();
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            if (className.getClassName().equals("SporService")) {
                sporService = null;
                Log.i("main", "SporService disconnected");
            }
        }
    };

    private final ActivityResultLauncher<String[]> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), grantedPermissions -> {
                boolean allMatch = true;
                for (Boolean granted : grantedPermissions.values()) {
                    allMatch &= granted;
                }
                if (allMatch) {
                    startTrackingService();
                    updateButtonAppearance();
                }
                // Missing stream, targeting low SDK
                /*
                if (grantedPermissions.values().stream().allMatch(Boolean.TRUE::equals)) {
                    startTrackingService();
                    updateButtonAppearance();
                }
                */
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        updateButtonAppearance();

        this.timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(() -> {
                    if (sporService != null) {
                        updateUILocationLabels(sporService.lat, sporService.lng, sporService.alt, sporService.distanceInCentimeters, sporService.getSpeedInMetersPerSecond(), sporService.getElapsedNanos());
                    } else {
                        updateUILocationLabels(Double.NaN, Double.NaN, Double.NaN, 0, 0, 0);
                    }
                });
            }
        }, 1000, 2500);

        if (SporService.isRunning()) { // Service exists, bind to it immediately.
            final Intent intent = new Intent(this.getApplication(), SporService.class);
            this.getApplication().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
            sporing = true;
            GridLayout layout = findViewById(R.id.grid);
            layout.setVisibility(View.VISIBLE);
            updateButtonAppearance();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.timer.cancel();
        // Spor service is running, only unbind so it keeps running.
        if (sporService != null) {
            this.getApplication().unbindService(serviceConnection);
        }
    }

    private void updateUILocationLabels(double lat, double lng, double alt, long distanceInCm, double speedInMetersPerSecond, long durationNanos) {
        TextView lngView = findViewById(R.id.lng);
        TextView latView = findViewById(R.id.lat);
        TextView altView = findViewById(R.id.alt);
        TextView distanceView = findViewById(R.id.dst);
        TextView velocityView = findViewById(R.id.vel);
        TextView durationView = findViewById(R.id.dur);
        lngView.setText(Double.isNaN(lng) ? "-" : String.format(Locale.US, "%.6f", lng));
        latView.setText(Double.isNaN(lat) ? "-" : String.format(Locale.US, "%.6f", lat));
        altView.setText(Double.isNaN(alt) ? "-" : String.format(Locale.US, "%.0fm", alt));
        distanceView.setText(String.format(Locale.US, "%.0fm", distanceInCm / 100.));
        velocityView.setText(String.format(Locale.US, "%.1fkm/h", 3.6 * speedInMetersPerSecond));
        durationView.setText(String.format(Locale.US, "%dh%dm", durationNanos / TimeUnit.HOURS.toNanos(1), (durationNanos % TimeUnit.HOURS.toNanos(1)) / TimeUnit.MINUTES.toNanos(1)));
    }

    private void startTrackingService() {
        // Targeting lower SDK, stream not available.
        // String[] missingPermissions = Stream.of(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE).filter(permission -> ActivityCompat.checkSelfPermission(this, permission) != PERMISSION_GRANTED).toArray(String[]::new);

        List<String> missingPermissions = new ArrayList<>();
        for (String permission : Arrays.asList(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        )) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PERMISSION_GRANTED) {
                missingPermissions.add(permission);
            }
        }

        if (missingPermissions.isEmpty()) {
            final Intent intent = new Intent(this.getApplication(), SporService.class);
            this.getApplication().startService(intent);
            this.getApplication().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
            GridLayout layout = findViewById(R.id.grid);
            layout.setVisibility(View.VISIBLE);
            sporing = true;
        } else {
            requestPermissionLauncher.launch(missingPermissions.toArray(new String[0]));
        }
    }

    private void stopTrackingService() {
        this.getApplication().unbindService(serviceConnection);
        final Intent intent = new Intent(this.getApplication(), SporService.class);
        this.getApplication().stopService(intent);
        GridLayout layout = findViewById(R.id.grid);
        layout.setVisibility(View.INVISIBLE);
        sporing = false;
    }

    public void onTrackingButtonClicked(View view) {
        if (sporing) {
            stopTrackingService();
        } else {
            startTrackingService();
        }
        updateButtonAppearance();
    }

    private void updateButtonAppearance() {
        Button btn = findViewById(R.id.toggle);
        btn.setTextColor(Color.WHITE);

        if (sporing) {
            btn.setBackgroundColor(Color.rgb(228, 48, 33));
            btn.setText(R.string.DeactivateButtonText);
        } else {
            btn.setBackgroundColor(Color.rgb(36, 201, 36));
            btn.setText(R.string.ActivateButtonText);
        }
    }
}