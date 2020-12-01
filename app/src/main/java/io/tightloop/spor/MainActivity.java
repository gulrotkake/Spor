package io.tightloop.spor;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.GridLayout;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.ViewModelProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class MainActivity extends AppCompatActivity {
    private Timer timer;
    private SporService sporService;
    private SporViewModel sporViewModel;

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
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sporViewModel = new ViewModelProvider(this).get(SporViewModel.class);

        this.timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(() -> {
                    if (sporService != null) {
                        sporViewModel.setLocationData(new SporViewModel.LocationData(sporService.lat, sporService.lng, sporService.alt, sporService.distanceInCentimeters, sporService.getSpeedInMetersPerSecond(), sporService.getElapsedNanos()));
                    } else {
                        sporViewModel.setLocationData(new SporViewModel.LocationData(Double.NaN, Double.NaN, Double.NaN, 0, 0, 0));
                    }
                });
            }
        }, 1000, 2500);

        if (SporService.isRunning()) { // Service exists, bind to it immediately.
            final Intent intent = new Intent(this.getApplication(), SporService.class);
            this.getApplication().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
            sporViewModel.setSporingState(true);
        } else {
            sporViewModel.setSporingState(false);
            SporRecorder.recoverRecordings(this.getApplicationContext().getFilesDir());
        }
    }

    @Override
    protected void onDestroy() {
        this.timer.cancel();
        // Spor service is running, only unbind so it keeps running.
        if (sporService != null) {
            this.getApplication().unbindService(serviceConnection);
        }
        super.onDestroy();
    }

    private void startTrackingService() {
        // Targeting lower SDK, stream not available.
        // String[] missingPermissions = Stream.of(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE).filter(permission -> ActivityCompat.checkSelfPermission(this, permission) != PERMISSION_GRANTED).toArray(String[]::new);

        List<String> missingPermissions = new ArrayList<>();
        for (String permission : Collections.singletonList(
                Manifest.permission.ACCESS_FINE_LOCATION
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
            sporViewModel.setSporingState(true);
        } else {
            requestPermissionLauncher.launch(missingPermissions.toArray(new String[0]));
        }
    }

    private void stopTrackingService() {
        this.getApplication().unbindService(serviceConnection);
        final Intent intent = new Intent(this.getApplication(), SporService.class);
        this.getApplication().stopService(intent);
        sporViewModel.setSporingState(false);
    }

    public void toggleTracking() {
        if (sporViewModel.getSporingState().getValue()) {
            stopTrackingService();
        } else {
            startTrackingService();
        }
    }
}