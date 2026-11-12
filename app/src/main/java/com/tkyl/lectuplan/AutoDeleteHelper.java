package com.tkyl.lectuplan;

import android.content.Context;
import android.content.SharedPreferences;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AutoDeleteHelper {
    public static final String PREF_NAME = "lectuplan_autodelete_prefs";
    public static final String KEY_ENABLED = "key_auto_delete_enabled";
    public static final String KEY_RETENTION_MONTHS = "key_retention_months";
    public static final String KEY_LAST_CLEANUP = "key_last_cleanup_timestamp";

    public static boolean isEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_ENABLED, true);
    }

    public static void setEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply();
    }

    public static int getRetentionMonths(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_RETENTION_MONTHS, 12); // Mặc định 1 năm (12 tháng)
    }

    public static void setRetentionMonths(Context context, int months) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_RETENTION_MONTHS, months).apply();
    }

    public static void recordCleanupTime(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putLong(KEY_LAST_CLEANUP, System.currentTimeMillis()).apply();
    }

    public static String getLastCleanupText(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        long time = prefs.getLong(KEY_LAST_CLEANUP, 0);
        if (time == 0) {
            return "🕒 Lần dọn gần nhất: Chưa dọn dẹp";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale.getDefault());
        return "🕒 Lần dọn gần nhất: " + sdf.format(new Date(time));
    }

    public static String getRetentionText(int months) {
        if (months == 3) return "3 tháng";
        if (months == 6) return "6 tháng";
        if (months == 12) return "1 năm (12 tháng)";
        if (months == 24) return "2 năm (24 tháng)";
        return "Không xóa";
    }
}
