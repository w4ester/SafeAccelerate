# SafeAccelerate

Android app that locks your phone while driving to keep you safe.

## How It Works

SafeAccelerate monitors your speed using GPS. When you hit **10 mph**, the app engages drive mode:

- **Locks the screen** with a full-screen overlay, no texting, no browsing
- **Only Maps stays accessible** so you can still navigate
- **Auto-replies to texts** to let people know you're driving
- **Mutes notifications** so there are no dings to distract you
- **30-second grace period** at stoplights so it doesn't disengage at every red light
- **Emergency unlock** by holding for 3 seconds if you need to override

When your speed drops below 10 mph for 30 seconds, drive mode disengages and your phone returns to normal.

## Why 10 mph?

That's faster than anyone jogs. If you're going 10+ mph, you're in a vehicle. Simple threshold, no false positives from walking or running.

## Tech

- Android (Java), targeting API 16+ (Jelly Bean and up)
- GPS `LocationManager` for speed detection
- Accelerometer as supplemental motion sensor
- Foreground service with persistent notification (so Android doesn't kill it)
- `BroadcastReceiver` for SMS auto-reply and boot persistence

## Permissions

| Permission | Why |
|-----------|-----|
| `ACCESS_FINE_LOCATION` | GPS speed monitoring |
| `RECEIVE_SMS` / `SEND_SMS` | Auto-reply while driving |
| `SYSTEM_ALERT_WINDOW` | Drive mode overlay |
| `MODIFY_AUDIO_SETTINGS` | Mute notifications |
| `RECEIVE_BOOT_COMPLETED` | Restart after reboot |
| `WAKE_LOCK` | Keep service alive |

## Published

Released on Google Play Store, 2015.
