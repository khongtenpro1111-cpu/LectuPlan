package com.tkyl.lectuplan;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.LocationManager;
import android.os.Build;
import android.provider.Settings;

public class AutoDndHelper {
    public static final String PREF_NAME = "lectuplan_dnd_prefs";
    public static final String KEY_AUTO_DND_ENABLED = "key_auto_dnd_enabled";
    public static final String KEY_AUTO_CHECKIN_ENABLED = "key_auto_checkin_enabled";

    public static boolean isAutoDndEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_AUTO_DND_ENABLED, false);
    }

    public static void setAutoDndEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_AUTO_DND_ENABLED, enabled).apply();
    }

    public static boolean isGpsEnabledOnDevice(Context context) {
        if (context == null) return false;
        LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (lm == null) return false;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                return lm.isLocationEnabled();
            } else {
                return lm.isProviderEnabled(LocationManager.GPS_PROVIDER) || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            }
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isAutoCheckinEnabled(Context context) {
        if (context == null) return false;
        if (!isGpsEnabledOnDevice(context)) {
            SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            if (prefs.getBoolean(KEY_AUTO_CHECKIN_ENABLED, false)) {
                prefs.edit().putBoolean(KEY_AUTO_CHECKIN_ENABLED, false).apply();
            }
            return false;
        }
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_AUTO_CHECKIN_ENABLED, false);
    }

    public static void setAutoCheckinEnabled(Context context, boolean enabled) {
        if (context == null) return;
        boolean actualEnabled = enabled && isGpsEnabledOnDevice(context);
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_AUTO_CHECKIN_ENABLED, actualEnabled).apply();
    }

    public static boolean hasDndPermission(Context context) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            return nm.isNotificationPolicyAccessGranted();
        }
        return false;
    }

    public static void requestDndPermission(Context context) {
        Intent intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
        context.startActivity(intent);
    }

    public static void enableDnd(Context context) {
        if (!hasDndPermission(context)) return;
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            try {
                nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE);
            } catch (Exception ignored) {}
        }
    }

    public static void disableDnd(Context context) {
        if (!hasDndPermission(context)) return;
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            try {
                nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL);
            } catch (Exception ignored) {}
        }
    }
}
