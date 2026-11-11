package com.ar.markdowneditor.util;

import android.content.Context;
import android.content.SharedPreferences;

public class ViewModePrefs {

    private static final String PREFS = "settings";
    private static final String KEY_VIEW_MODE = "default_view_mode";

    public static final int MODE_LIST = 0;
    public static final int MODE_GRID = 1;

    private static final int DEFAULT_MODE = MODE_GRID;

    public static int getMode(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return prefs.getInt(KEY_VIEW_MODE, DEFAULT_MODE);
    }

    public static void setMode(Context context, int mode) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().putInt(KEY_VIEW_MODE, mode).apply();
    }
}