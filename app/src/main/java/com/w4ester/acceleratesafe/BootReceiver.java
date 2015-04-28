package com.w4ester.acceleratesafe;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

/**
 * Restarts the monitoring service after phone reboot if it was armed.
 * Nobody wants to remember to re-enable their safety app every time
 * their phone restarts.
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            SharedPreferences prefs = context.getSharedPreferences(
                    "SafeAcceleratePrefs", Context.MODE_PRIVATE);

            if (prefs.getBoolean("service_armed", false)) {
                Intent serviceIntent = new Intent(context, SafeAccelerateService.class);
                context.startService(serviceIntent);
            }
        }
    }
}
