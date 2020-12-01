package io.tightloop.spor;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class Spor extends Fragment {
    private SporViewModel sporViewModel;

    public Spor() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_spor, container, false);
        sporViewModel = new ViewModelProvider(requireActivity()).get(SporViewModel.class);
        sporViewModel.getLocationData().observe(getViewLifecycleOwner(), locationData -> {
            updateUILocationLabels(view, locationData.lat, locationData.lng, locationData.alt, locationData.distanceInCm, locationData.speedInMetersPerSecond, locationData.durationNano);
        });

        final Button btn = view.findViewById(R.id.toggle);
        btn.setOnClickListener(this::onTrackingButtonClicked);
        btn.setTextColor(Color.WHITE);

        sporViewModel.getSporingState().observe(getViewLifecycleOwner(), sporing -> {
            GridLayout layout = view.findViewById(R.id.grid);
            if (sporing) {
                layout.setVisibility(View.VISIBLE);
                btn.setBackgroundColor(Color.rgb(228, 48, 33));
                btn.setText(R.string.DeactivateButtonText);
            } else {
                layout.setVisibility(View.INVISIBLE);
                btn.setBackgroundColor(Color.rgb(36, 201, 36));
                btn.setText(R.string.ActivateButtonText);
            }
        });

        return view;
    }

    private void updateUILocationLabels(View view, double lat, double lng, double alt, long distanceInCm, double speedInMetersPerSecond, long durationNanos) {
        TextView lngView = view.findViewById(R.id.lng);
        TextView latView = view.findViewById(R.id.lat);
        TextView altView = view.findViewById(R.id.alt);
        TextView distanceView = view.findViewById(R.id.dst);
        TextView velocityView = view.findViewById(R.id.vel);
        TextView durationView = view.findViewById(R.id.dur);
        lngView.setText(Double.isNaN(lng) ? "-" : String.format(Locale.US, "%.6f", lng));
        latView.setText(Double.isNaN(lat) ? "-" : String.format(Locale.US, "%.6f", lat));
        altView.setText(Double.isNaN(alt) ? "-" : String.format(Locale.US, "%.0fm", alt));
        distanceView.setText(String.format(Locale.US, "%.0fm", distanceInCm / 100.));
        velocityView.setText(String.format(Locale.US, "%.1fkm/h", 3.6 * speedInMetersPerSecond));
        durationView.setText(String.format(Locale.US, "%dh%dm", durationNanos / TimeUnit.HOURS.toNanos(1), (durationNanos % TimeUnit.HOURS.toNanos(1)) / TimeUnit.MINUTES.toNanos(1)));
    }

    public void onTrackingButtonClicked(View view) {
        ((MainActivity) requireActivity()).toggleTracking();
    }
}