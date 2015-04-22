package com.w4ester.acceleratesafe;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

/**
 * Background service that monitors speed and manages drive mode.
 *
 * Runs as a foreground service with a persistent notification so Android
 * won't kill it while the user is driving. When speed crosses 10 mph,
 * it activates drive mode. When speed drops below threshold for 30 seconds
 * (to handle stoplights), it deactivates.
 */
public class SafeAccelerateService extends Service implements SpeedDetector.SpeedListener {

    private static final String TAG = "SafeAccelService";
    private static final int NOTIFICATION_ID = 1001;
    private static final long DEACTIVATION_DELAY_MS = 30000; // 30 sec grace at stops

    private SpeedDetector speedDetector;
    private DriveModeManager driveModeManager;
    private Handler handler;
    private boolean driveModeActive = false;

    private Runnable deactivateRunnable = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "Deactivation timer fired -leaving drive mode");
            deactivateDriveMode();
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler();
        speedDetector = new SpeedDetector(this, this);
        driveModeManager = new DriveModeManager(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(NOTIFICATION_ID, buildNotification("Monitoring speed..."));
        speedDetector.start();
        Log.d(TAG, "SafeAccelerate monitoring started");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        speedDetector.stop();
        if (driveModeActive) {
            deactivateDriveMode();
        }
        handler.removeCallbacks(deactivateRunnable);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // --- SpeedDetector.SpeedListener ---

    @Override
    public void onSpeedChanged(float speedMph) {
        // Broadcast to UI
        Intent uiIntent = new Intent(MainActivity.ACTION_SPEED_UPDATE);
        uiIntent.putExtra(MainActivity.EXTRA_SPEED_MPH, speedMph);
        sendBroadcast(uiIntent);
    }

    @Override
    public void onDrivingStateChanged(boolean driving) {
        if (driving && !driveModeActive) {
            // Cancel any pending deactivation (was at a stoplight, now moving again)
            handler.removeCallbacks(deactivateRunnable);
            activateDriveMode();
        } else if (!driving && driveModeActive) {
            // Don't deactivate immediately -could be a red light
            // Wait 30 seconds of sub-threshold speed before disabling
            handler.removeCallbacks(deactivateRunnable);
            handler.postDelayed(deactivateRunnable, DEACTIVATION_DELAY_MS);
        }
    }

    private void activateDriveMode() {
        driveModeActive = true;
        driveModeManager.activate();

        // Update notification
        startForeground(NOTIFICATION_ID, buildNotification("DRIVE MODE ACTIVE -stay safe"));

        // Broadcast to UI
        Intent uiIntent = new Intent(MainActivity.ACTION_DRIVEMODE_CHANGED);
        uiIntent.putExtra(MainActivity.EXTRA_DRIVEMODE_ACTIVE, true);
        sendBroadcast(uiIntent);

        // Launch the overlay lock screen
        Intent overlayIntent = new Intent(this, DriveModeOverlay.class);
        overlayIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(overlayIntent);

        Log.d(TAG, "Drive mode ACTIVATED");
    }

    private void deactivateDriveMode() {
        driveModeActive = false;
        driveModeManager.deactivate();

        startForeground(NOTIFICATION_ID, buildNotification("Monitoring speed..."));

        Intent uiIntent = new Intent(MainActivity.ACTION_DRIVEMODE_CHANGED);
        uiIntent.putExtra(MainActivity.EXTRA_DRIVEMODE_ACTIVE, false);
        sendBroadcast(uiIntent);

        // Tell overlay to close
        Intent closeOverlay = new Intent(DriveModeOverlay.ACTION_CLOSE_OVERLAY);
        sendBroadcast(closeOverlay);

        Log.d(TAG, "Drive mode DEACTIVATED");
    }

    private Notification buildNotification(String text) {
        Intent notifyIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        return new NotificationCompat.Builder(this)
                .setContentTitle("SafeAccelerate")
                .setContentText(text)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }
}
