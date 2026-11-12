package com.tkyl.lectuplan;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;

public class ThemeHelper {
    public static final String PREF_NAME = "lectuplan_theme_prefs";
    public static final String KEY_THEME_MODE = "key_theme_mode";

    public static final int MODE_SYSTEM = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM; // -1
    public static final int MODE_LIGHT = AppCompatDelegate.MODE_NIGHT_NO; // 1
    public static final int MODE_DARK = AppCompatDelegate.MODE_NIGHT_YES; // 2

    /**
     * Áp dụng chế độ giao diện đã lưu trong SharedPreferences.
     */
    public static void applyTheme(Context context) {
        int mode = getThemeMode(context);
        AppCompatDelegate.setDefaultNightMode(mode);
    }

    /**
     * Lấy chế độ giao diện đang lưu (Mặc định: Theo hệ thống).
     */
    public static int getThemeMode(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_THEME_MODE, MODE_SYSTEM);
    }

    /**
     * Lưu và áp dụng ngay chế độ giao diện mới.
     */
    public static void setThemeMode(Context context, int mode) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_THEME_MODE, mode).apply();
        AppCompatDelegate.setDefaultNightMode(mode);
    }
}
