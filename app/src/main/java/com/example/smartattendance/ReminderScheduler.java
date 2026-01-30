package com.example.smartattendance;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;

public class ReminderScheduler {

    public static void schedule10MinBeforeEnd(Context context,
                                              long endTimeMillis,
                                              String title,
                                              String message,
                                              int requestCode) {

        long triggerAt = endTimeMillis - (10 * 60 * 1000); // 10 minutes before
        if (triggerAt <= System.currentTimeMillis()) return;

        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra("title", title);
        intent.putExtra("message", message);

        PendingIntent pi = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        // ✅ Android 12+ exact alarm restriction
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

            if (am.canScheduleExactAlarms()) {
                // Allowed → use exact alarm
                try {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
                } catch (SecurityException e) {
                    // In case device still blocks → fallback
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
                }
            } else {
                // Not allowed → fallback (not guaranteed exact, but works without crash)
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
            }

        } else {
            // Android 11 and below
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi);
        }
    }

    public static void cancel(Context context, int requestCode) {
        Intent intent = new Intent(context, ReminderReceiver.class);

        PendingIntent pi = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am != null) am.cancel(pi);
    }

    // OPTIONAL: If you want to send user to allow exact alarms (Android 12+)
    public static void openExactAlarmSettings(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent i = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(i);
        }
    }
}
