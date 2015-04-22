package com.w4ester.acceleratesafe;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.util.Log;

/**
 * Handles the system-level changes when drive mode engages:
 * - Mutes notification sounds (don't want dings while driving)
 * - Stores previous audio state so we can restore it cleanly
 * - Sets a flag that SmsAutoResponder checks
 *
 * We don't block calls entirely because 911 needs to work.
 * Instead, we let calls ring through but mute notification sounds
 * and block the screen with the overlay.
 */
public class DriveModeManager {

    private static final String TAG = "DriveModeManager";
    private static final String PREFS_NAME = "SafeAcceleratePrefs";
    private static final String KEY_DRIVEMODE = "drivemode_active";
    private static final String KEY_PREV_RINGER = "prev_ringer_mode";
    private static final String KEY_PREV_VOLUME = "prev_notif_volume";

    private Context context;
    private AudioManager audioManager;

    public DriveModeManager(Context context) {
        this.context = context;
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    public void activate() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Save current audio state
        int currentRinger = audioManager.getRingerMode();
        int currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION);

        prefs.edit()
                .putBoolean(KEY_DRIVEMODE, true)
                .putInt(KEY_PREV_RINGER, currentRinger)
                .putInt(KEY_PREV_VOLUME, currentVolume)
                .apply();

        // Silence notifications (but not alarms -keep alarm channel for emergencies)
        audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, 0, 0);
        audioManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);

        Log.d(TAG, "Drive mode activated -notifications silenced");
    }

    public void deactivate() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // Restore previous audio state
        int prevRinger = prefs.getInt(KEY_PREV_RINGER, AudioManager.RINGER_MODE_NORMAL);
        int prevVolume = prefs.getInt(KEY_PREV_VOLUME, 5);

        audioManager.setRingerMode(prevRinger);
        audioManager.setStreamVolume(AudioManager.STREAM_NOTIFICATION, prevVolume, 0);

        prefs.edit().putBoolean(KEY_DRIVEMODE, false).apply();

        Log.d(TAG, "Drive mode deactivated -audio restored");
    }

    public static boolean isDriveModeActive(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_DRIVEMODE, false);
    }
}
