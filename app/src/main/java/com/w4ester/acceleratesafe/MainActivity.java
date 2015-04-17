package com.w4ester.acceleratesafe;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.ToggleButton;

/**
 * Main screen for SafeAccelerate.
 * Big toggle to arm/disarm the driving monitor.
 * Shows current speed and whether drive mode is engaged.
 */
public class MainActivity extends Activity {

    private static final String PREFS_NAME = "SafeAcceleratePrefs";
    private static final String KEY_ARMED = "service_armed";

    public static final String ACTION_SPEED_UPDATE = "com.w4ester.acceleratesafe.SPEED_UPDATE";
    public static final String ACTION_DRIVEMODE_CHANGED = "com.w4ester.acceleratesafe.DRIVEMODE_CHANGED";
    public static final String EXTRA_SPEED_MPH = "speed_mph";
    public static final String EXTRA_DRIVEMODE_ACTIVE = "drivemode_active";

    private ToggleButton toggleArm;
    private TextView tvSpeed;
    private TextView tvStatus;
    private View statusIndicator;

    private BroadcastReceiver uiReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_SPEED_UPDATE.equals(intent.getAction())) {
                float mph = intent.getFloatExtra(EXTRA_SPEED_MPH, 0f);
                tvSpeed.setText(String.format("%.1f mph", mph));
            } else if (ACTION_DRIVEMODE_CHANGED.equals(intent.getAction())) {
                boolean active = intent.getBooleanExtra(EXTRA_DRIVEMODE_ACTIVE, false);
                updateDriveModeUI(active);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toggleArm = (ToggleButton) findViewById(R.id.toggle_arm);
        tvSpeed = (TextView) findViewById(R.id.tv_speed);
        tvStatus = (TextView) findViewById(R.id.tv_status);
        statusIndicator = findViewById(R.id.status_indicator);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean wasArmed = prefs.getBoolean(KEY_ARMED, false);
        toggleArm.setChecked(wasArmed);

        toggleArm.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                prefs.edit().putBoolean(KEY_ARMED, isChecked).apply();

                Intent serviceIntent = new Intent(MainActivity.this, SafeAccelerateService.class);
                if (isChecked) {
                    startService(serviceIntent);
                    tvStatus.setText(R.string.status_monitoring);
                } else {
                    stopService(serviceIntent);
                    tvStatus.setText(R.string.status_disabled);
                    tvSpeed.setText("0.0 mph");
                    updateDriveModeUI(false);
                }
            }
        });

        if (wasArmed) {
            tvStatus.setText(R.string.status_monitoring);
            startService(new Intent(this, SafeAccelerateService.class));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_SPEED_UPDATE);
        filter.addAction(ACTION_DRIVEMODE_CHANGED);
        registerReceiver(uiReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(uiReceiver);
    }

    private void updateDriveModeUI(boolean active) {
        if (active) {
            tvStatus.setText(R.string.status_drive_mode);
            statusIndicator.setBackgroundResource(R.drawable.indicator_active);
        } else {
            if (toggleArm.isChecked()) {
                tvStatus.setText(R.string.status_monitoring);
            } else {
                tvStatus.setText(R.string.status_disabled);
            }
            statusIndicator.setBackgroundResource(R.drawable.indicator_idle);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            // Future: settings activity for speed threshold, auto-reply message
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
