package com.w4ester.acceleratesafe;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

/**
 * Detects vehicle speed using GPS and accelerometer data.
 *
 * Primary: GPS location speed (most accurate).
 * Secondary: Accelerometer integration for when GPS speed isn't available
 * (tunnels, urban canyons).
 *
 * The 10 mph threshold was chosen because that's about the fastest a person
 * jogs -anything above that, you're in a vehicle.
 */
public class SpeedDetector implements LocationListener, SensorEventListener {

    private static final String TAG = "SpeedDetector";
    private static final float SPEED_THRESHOLD_MPS = 4.47f; // 10 mph in meters/sec
    private static final long GPS_MIN_TIME_MS = 1000;       // 1 second updates
    private static final float GPS_MIN_DISTANCE_M = 5f;     // 5 meter movement

    private Context context;
    private LocationManager locationManager;
    private SensorManager sensorManager;
    private Sensor accelerometer;

    private SpeedListener listener;
    private Location lastLocation;
    private float currentSpeedMps = 0f;
    private boolean isDriving = false;

    // Accelerometer smoothing
    private float[] gravity = new float[3];
    private float[] linearAccel = new float[3];
    private long lastAccelTime = 0;
    private float accelSpeedEstimate = 0f;

    public interface SpeedListener {
        void onSpeedChanged(float speedMph);
        void onDrivingStateChanged(boolean driving);
    }

    public SpeedDetector(Context context, SpeedListener listener) {
        this.context = context;
        this.listener = listener;
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        this.sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        this.accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    public void start() {
        try {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    GPS_MIN_TIME_MS,
                    GPS_MIN_DISTANCE_M,
                    this);
        } catch (SecurityException e) {
            Log.e(TAG, "No GPS permission", e);
        }

        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    public void stop() {
        try {
            locationManager.removeUpdates(this);
        } catch (SecurityException e) {
            Log.e(TAG, "Error removing GPS updates", e);
        }
        sensorManager.unregisterListener(this);
        currentSpeedMps = 0f;
        isDriving = false;
    }

    public float getCurrentSpeedMph() {
        return currentSpeedMps * 2.23694f; // m/s to mph
    }

    public boolean isDriving() {
        return isDriving;
    }

    // --- LocationListener ---

    @Override
    public void onLocationChanged(Location location) {
        if (location.hasSpeed()) {
            currentSpeedMps = location.getSpeed();
        } else if (lastLocation != null) {
            float distance = lastLocation.distanceTo(location);
            float timeDelta = (location.getTime() - lastLocation.getTime()) / 1000f;
            if (timeDelta > 0) {
                currentSpeedMps = distance / timeDelta;
            }
        }

        lastLocation = location;
        float mph = getCurrentSpeedMph();
        listener.onSpeedChanged(mph);

        boolean nowDriving = currentSpeedMps >= SPEED_THRESHOLD_MPS;
        if (nowDriving != isDriving) {
            isDriving = nowDriving;
            listener.onDrivingStateChanged(isDriving);
            Log.d(TAG, "Driving state changed: " + isDriving + " at " + mph + " mph");
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}

    @Override
    public void onProviderEnabled(String provider) {}

    @Override
    public void onProviderDisabled(String provider) {
        Log.w(TAG, "GPS provider disabled");
    }

    // --- SensorEventListener (accelerometer fallback) ---

    @Override
    public void onSensorChanged(SensorEvent event) {
        // High-pass filter to remove gravity
        final float alpha = 0.8f;

        gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
        gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
        gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

        linearAccel[0] = event.values[0] - gravity[0];
        linearAccel[1] = event.values[1] - gravity[1];
        linearAccel[2] = event.values[2] - gravity[2];

        // Only use accelerometer as supplemental -GPS is primary
        float magnitude = (float) Math.sqrt(
                linearAccel[0] * linearAccel[0] +
                linearAccel[1] * linearAccel[1] +
                linearAccel[2] * linearAccel[2]);

        long now = System.currentTimeMillis();
        if (lastAccelTime > 0) {
            float dt = (now - lastAccelTime) / 1000f;
            // Crude integration -decays over time to prevent drift
            accelSpeedEstimate = accelSpeedEstimate * 0.95f + magnitude * dt;
        }
        lastAccelTime = now;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}
