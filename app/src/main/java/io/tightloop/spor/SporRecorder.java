package io.tightloop.spor;

import android.os.Environment;
import android.util.Log;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class SporRecorder {
    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyyMMddHHmmss'Z'", Locale.US);
    private static final File STORAGE_DIR = new File(Environment.getExternalStorageDirectory().getAbsoluteFile(), "spor");

    private static class ActiveRecording implements AutoCloseable {
        private final DataOutputStream outputStream;
        private final File gpxFile;
        private final File sporFile;

        ActiveRecording(File storageDir) {
            String dateString = DATE_FMT.format(new Date());
            gpxFile = new File(storageDir, String.format("%s.gpx", dateString));
            sporFile = new File(storageDir, String.format("%s.spor", dateString));

            try {
                this.outputStream = new DataOutputStream(new FileOutputStream(sporFile));
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void close() throws IOException {
            outputStream.close();
            DistanceUtil.spor2Gpx(sporFile, gpxFile);
            if (!sporFile.delete()) {
                Log.w("SporRecorder", String.format("Failed to delete %s", sporFile));
            }
        }
    }

    private final List<ActiveRecording> activeRecordings;

    public SporRecorder() {
        this.activeRecordings = new ArrayList<>(1);
    }

    public void startRecording() {
        if (STORAGE_DIR.mkdirs()) {
            Log.i("SporService", "Created storage directory " + STORAGE_DIR);
        }

        if (activeRecordings.size() > 0) {
            Log.e("SporRecorder", "Attempted to start recorder while recording active");
            throw new RuntimeException("Already recording.");
        }

        activeRecordings.add(new ActiveRecording(STORAGE_DIR));
    }

    public boolean isRecording() {
        return !activeRecordings.isEmpty();
    }

    public void recordDataPoint(long timestamp, double lat, double lng, double alt) {
        ActiveRecording activeRecording = activeRecordings.get(0);

        try {
            activeRecording.outputStream.writeDouble(lat);
            activeRecording.outputStream.writeDouble(lng);
            activeRecording.outputStream.writeDouble(alt);
            activeRecording.outputStream.writeLong(timestamp);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write log.");
        }
    }

    public void stopRecording() {
        try {
            activeRecordings.remove(0).close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void recoverRecordings() {
        if (!STORAGE_DIR.isDirectory()) {
            return;
        }

        // Attempt to recover any .spor files
        for (File sporFile : STORAGE_DIR.listFiles((dir, name) -> dir.equals(STORAGE_DIR) && name.endsWith(".spor"))) {
            try {
                String fileName = sporFile.getName().substring(0, sporFile.getName().lastIndexOf('.'));
                File gpxFile = new File(STORAGE_DIR, String.format("%s.gpx", fileName));
                DistanceUtil.spor2Gpx(sporFile, gpxFile);
                Log.i("SporRecorder", String.format("Recovered %s", gpxFile));
            } catch (IOException e) {
                Log.e("SporRecorder", String.format("Failed to recover %s", sporFile), e);
            } finally {
                if (!sporFile.delete()) {
                    Log.w("SporRecorder", String.format("Failed to delete %s after recovery", sporFile));
                }
            }
        }
    }
}
