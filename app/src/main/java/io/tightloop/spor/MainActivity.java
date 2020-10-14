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
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Stream;

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
                if (grantedPermissions.values().stream().allMatch(Boolean.TRUE::equals)) {
                    startTrackingService();
                    updateButtonAppearance();
                }
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
                        updateUILocationLabels(sporService.lat, sporService.lng, sporService.alt, sporService.distanceInCentimeters / 100.);
                    } else {
                        updateUILocationLabels(Double.NaN, Double.NaN, Double.NaN, Double.NaN);
                    }
                });
            }
        }, 1000, 5000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.timer.cancel();
        stopTrackingService();
    }

    private void updateUILocationLabels(double lat, double lng, double alt, double distance) {
        TextView lngView = findViewById(R.id.lng);
        TextView latView = findViewById(R.id.lat);
        TextView altView = findViewById(R.id.alt);
        TextView distanceView = findViewById(R.id.distance);
        lngView.setText(Double.isNaN(lng) ? "-" : String.format(Locale.US, "%.6f", lng));
        latView.setText(Double.isNaN(lat) ? "-" : String.format(Locale.US, "%.6f", lat));
        altView.setText(Double.isNaN(alt) ? "-" : String.format(Locale.US, "%.2f", alt));
        distanceView.setText(Double.isNaN(distance) ? "-" : String.format(Locale.US, "%.0fm", distance));
    }

    private void startTrackingService() {
        String[] missingPermissions = Stream.of(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE).filter(permission -> ActivityCompat.checkSelfPermission(this, permission) != PERMISSION_GRANTED).toArray(String[]::new);

        if (missingPermissions.length == 0) {
            final Intent intent = new Intent(this.getApplication(), SporService.class);
            this.getApplication().startService(intent);
            this.getApplication().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
            sporing = true;
        } else {
            requestPermissionLauncher.launch(missingPermissions);
        }
    }

    private void stopTrackingService() {
        this.getApplication().unbindService(serviceConnection);
        final Intent intent = new Intent(this.getApplication(), SporService.class);
        this.getApplication().stopService(intent);
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
            btn.setText("Deaktiver");
        } else {
            btn.setBackgroundColor(Color.rgb(36, 201, 36));
            btn.setText("Aktiver");
        }
    }
}