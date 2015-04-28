package com.w4ester.acceleratesafe;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;
import android.util.Log;

/**
 * Listens for incoming SMS while drive mode is active.
 * Auto-replies so people know you're driving and not ignoring them.
 *
 * Keeps a simple cooldown per number so we don't spam someone
 * who sends multiple texts in a row.
 */
public class SmsAutoResponder extends BroadcastReceiver {

    private static final String TAG = "SmsAutoResponder";
    private static final String AUTO_REPLY =
            "I'm driving right now - SafeAccelerate has my phone locked down." +
            "I'll get back to you when I stop. Drive safe!";

    // Simple cooldown: don't auto-reply to same number within 5 minutes
    private static java.util.HashMap<String, Long> recentReplies = new java.util.HashMap<>();
    private static final long COOLDOWN_MS = 5 * 60 * 1000; // 5 minutes

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!DriveModeManager.isDriveModeActive(context)) {
            return; // Not driving, don't interfere
        }

        Bundle bundle = intent.getExtras();
        if (bundle == null) return;

        Object[] pdus = (Object[]) bundle.get("pdus");
        if (pdus == null) return;

        for (Object pdu : pdus) {
            SmsMessage message = SmsMessage.createFromPdu((byte[]) pdu);
            String sender = message.getOriginatingAddress();

            if (sender != null && shouldReply(sender)) {
                sendAutoReply(sender);
                recentReplies.put(sender, System.currentTimeMillis());
                Log.d(TAG, "Auto-replied to " + sender);
            }
        }
    }

    private boolean shouldReply(String number) {
        Long lastReply = recentReplies.get(number);
        if (lastReply == null) return true;
        return (System.currentTimeMillis() - lastReply) > COOLDOWN_MS;
    }

    private void sendAutoReply(String number) {
        try {
            SmsManager sms = SmsManager.getDefault();
            sms.sendTextMessage(number, null, AUTO_REPLY, null, null);
        } catch (Exception e) {
            Log.e(TAG, "Failed to send auto-reply", e);
        }
    }
}
