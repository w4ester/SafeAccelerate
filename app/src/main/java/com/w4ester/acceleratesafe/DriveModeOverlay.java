package com.w4ester.acceleratesafe;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Full-screen overlay that blocks phone interaction while driving.
 *
 * Shows a "DRIVE SAFE" message with current speed.
 * Only escape hatch: the Maps button (because you need navigation)
 * and emergency unlock (long-press the unlock area for 3 seconds).
 *
 * The overlay sits on top of everything and captures back button
 * presses so the user can't dismiss it while moving.
 */
public class DriveModeOverlay extends Activity {

    public static final String ACTION_CLOSE_OVERLAY = "com.w4ester.acceleratesafe.CLOSE_OVERLAY";

    private TextView tvOverlaySpeed;
    private long unlockPressStart = 0;
    private static final long UNLOCK_HOLD_MS = 3000; // hold 3 sec to emergency unlock

    private BroadcastReceiver closeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            finish();
        }
    };

    private BroadcastReceiver speedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            float mph = intent.getFloatExtra(MainActivity.EXTRA_SPEED_MPH, 0f);
            tvOverlaySpeed.setText(String.format("%.0f mph", mph));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Make it a lock-screen overlay
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

        setContentView(R.layout.overlay_drive_mode);

        tvOverlaySpeed = (TextView) findViewById(R.id.tv_overlay_speed);
        Button btnMaps = (Button) findViewById(R.id.btn_open_maps);
        View btnEmergencyUnlock = findViewById(R.id.btn_emergency_unlock);

        // Maps button -the one thing you're allowed to do
        btnMaps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                launchMaps();
            }
        });

        // Emergency unlock -hold for 3 seconds
        btnEmergencyUnlock.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, android.view.MotionEvent event) {
                switch (event.getAction()) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        unlockPressStart = System.currentTimeMillis();
                        return true;
                    case android.view.MotionEvent.ACTION_UP:
                        long held = System.currentTimeMillis() - unlockPressStart;
                        if (held >= UNLOCK_HOLD_MS) {
                            Toast.makeText(DriveModeOverlay.this,
                                    "Emergency unlock -drive safe!", Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(DriveModeOverlay.this,
                                    "Hold for 3 seconds to unlock", Toast.LENGTH_SHORT).show();
                        }
                        return true;
                }
                return false;
            }
        });

        registerReceiver(closeReceiver, new IntentFilter(ACTION_CLOSE_OVERLAY));
        registerReceiver(speedReceiver, new IntentFilter(MainActivity.ACTION_SPEED_UPDATE));
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(closeReceiver);
        unregisterReceiver(speedReceiver);
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        // Block back button -you're driving!
        Toast.makeText(this, "SafeAccelerate is active -focus on the road!",
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Block home and recent apps as much as possible
        if (keyCode == KeyEvent.KEYCODE_HOME || keyCode == KeyEvent.KEYCODE_APP_SWITCH) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void launchMaps() {
        // Try Google Maps first, fall back to any maps app
        Intent mapsIntent = new Intent(Intent.ACTION_VIEW);
        mapsIntent.setPackage("com.google.android.apps.maps");

        PackageManager pm = getPackageManager();
        ComponentName resolved = mapsIntent.resolveActivity(pm);

        if (resolved != null) {
            startActivity(mapsIntent);
        } else {
            // Fall back to generic map intent
            Intent genericMaps = new Intent(Intent.ACTION_VIEW,
                    android.net.Uri.parse("geo:0,0"));
            if (genericMaps.resolveActivity(pm) != null) {
                startActivity(genericMaps);
            } else {
                Toast.makeText(this, "No maps app found", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
