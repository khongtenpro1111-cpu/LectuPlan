package com.tkyl.lectuplan;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import java.util.Calendar;
import java.util.Locale;

public class AlarmScheduler {
    public static final String PREF_NAME = "lectuplan_alarm_prefs";
    public static final String KEY_ALARM_ENABLED = "key_alarm_enabled";
    public static final String KEY_ALARM_HOUR = "key_alarm_hour";
    public static final String KEY_ALARM_MINUTE = "key_alarm_minute";
    public static final String KEY_ALARM_ONLY_IF_EVENTS = "key_alarm_only_if_events";

    public static final int DEFAULT_HOUR = 6;
    public static final int DEFAULT_MINUTE = 0;

    public static boolean isAlarmEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_ALARM_ENABLED, true);
    }

    public static void setAlarmEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_ALARM_ENABLED, enabled).apply();
        scheduleAlarm(context);
    }

    public static int getAlarmHour(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_ALARM_HOUR, DEFAULT_HOUR);
    }

    public static int getAlarmMinute(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_ALARM_MINUTE, DEFAULT_MINUTE);
    }

    public static void setAlarmTime(Context context, int hour, int minute) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_ALARM_HOUR, hour).putInt(KEY_ALARM_MINUTE, minute).apply();
        scheduleAlarm(context);
    }

    public static boolean isOnlyIfEventsEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_ALARM_ONLY_IF_EVENTS, true);
    }

    public static void setOnlyIfEventsEnabled(Context context, boolean onlyIfEvents) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_ALARM_ONLY_IF_EVENTS, onlyIfEvents).apply();
    }

    public static String getFormattedTime(int hour, int minute) {
        String amPm = (hour >= 12) ? "PM" : "AM";
        int h12 = hour % 12;
        if (h12 == 0) h12 = 12;
        return String.format(Locale.US, "%02d:%02d %s", h12, minute, amPm);
    }

    public static boolean canScheduleExactAlarms(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            return alarmManager != null && alarmManager.canScheduleExactAlarms();
        }
        return true;
    }

    public static void scheduleAlarm(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.setAction("com.tkyl.lectuplan.ACTION_TRIGGER_ALARM");

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                600,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (!isAlarmEnabled(context)) {
            alarmManager.cancel(pendingIntent);
            return;
        }

        int hour = getAlarmHour(context);
        int minute = getAlarmMinute(context);

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        if (cal.before(Calendar.getInstance())) {
            cal.add(Calendar.DAY_OF_MONTH, 1);
        }

        long triggerAtMillis = cal.getTimeInMillis();

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            } else {
                // Sử dụng setAlarmClock để đảm bảo báo thức phát chính xác tuyệt đối trên Android 6+ và các dòng máy OEM (Xiaomi, Samsung, Oppo...)
                Intent showIntent = new Intent(context, MainActivity.class);
                PendingIntent showPendingIntent = PendingIntent.getActivity(
                        context, 0, showIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

                AlarmManager.AlarmClockInfo clockInfo = new AlarmManager.AlarmClockInfo(triggerAtMillis, showPendingIntent);
                alarmManager.setAlarmClock(clockInfo, pendingIntent);
            }
        } catch (SecurityException e) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
                }
            } catch (Exception ignored) {}
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getCountdownText(Context context) {
        if (!isAlarmEnabled(context)) {
            return "🔴 Báo thức đang TẮT";
        }

        int hour = getAlarmHour(context);
        int minute = getAlarmMinute(context);

        Calendar target = Calendar.getInstance();
        target.set(Calendar.HOUR_OF_DAY, hour);
        target.set(Calendar.MINUTE, minute);
        target.set(Calendar.SECOND, 0);
        target.set(Calendar.MILLISECOND, 0);

        boolean isTomorrow = target.before(Calendar.getInstance());
        if (isTomorrow) {
            target.add(Calendar.DAY_OF_MONTH, 1);
        }

        long diffMs = target.getTimeInMillis() - System.currentTimeMillis();
        long diffMinutes = (diffMs / (1000 * 60)) % 60;
        long diffHours = (diffMs / (1000 * 60 * 60));

        String timeStr = getFormattedTime(hour, minute);
        String dayStr = isTomorrow ? "Sáng mai" : "Hôm nay";

        if (diffHours == 0 && diffMinutes == 0) {
            return "⏰ Chuẩn bị báo thức lúc " + timeStr;
        } else if (diffHours == 0) {
            return "⏳ Lần báo tiếp: " + timeStr + " (" + dayStr + " - còn " + diffMinutes + " phút)";
        } else {
            return "⏳ Lần báo tiếp: " + timeStr + " (" + dayStr + " - còn " + diffHours + "h" + (diffMinutes > 0 ? String.format(Locale.US, " %02dp", diffMinutes) : "") + ")";
        }
    }
}
