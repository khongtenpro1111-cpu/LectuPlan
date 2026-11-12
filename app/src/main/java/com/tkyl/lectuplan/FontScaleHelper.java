package com.tkyl.lectuplan;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;

public class FontScaleHelper {
    public static final String PREF_NAME = "lectuplan_font_prefs";
    public static final String KEY_FONT_SIZE_MODE = "app_font_size_mode";

    public static final int FONT_SMALL = 0;
    public static final int FONT_MEDIUM = 1;
    public static final int FONT_LARGE = 2;

    public static int getFontSizeMode(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_FONT_SIZE_MODE, FONT_MEDIUM);
    }

    public static void setFontSizeMode(Context context, int mode) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_FONT_SIZE_MODE, mode).apply();
    }

    public static Context wrapContext(Context context) {
        int mode = getFontSizeMode(context);
        float fontScale = 1.0f;
        if (mode == FONT_SMALL) {
            fontScale = 0.88f;
        } else if (mode == FONT_LARGE) {
            fontScale = 1.20f;
        } else {
            fontScale = 1.0f;
        }

        Configuration configuration = new Configuration(context.getResources().getConfiguration());
        configuration.fontScale = fontScale;
        return context.createConfigurationContext(configuration);
    }
}
