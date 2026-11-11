package com.ar.markdowneditor.util;

import android.content.Context;
import android.content.SharedPreferences;

public class PrefsHelper {

    private static final String PREFS = "settings";

    private static final String KEY_AUTOSAVE_SEC = "autosave_seconds";
    public static final int AUTOSAVE_DEFAULT = 5;
    public static final int AUTOSAVE_MIN = 1;
    public static final int AUTOSAVE_MAX = 60;
	private static final String KEY_SCREENSHOT = "allow_screenshot";
	public static final boolean SCREENSHOT_DEFAULT = true;
	private static final String KEY_BIOMETRIC = "biometric_enabled";
	private static final String KEY_AUTOREMOVE_DAYS = "autoremove_days";
	public static final int AUTOREMOVE_DEFAULT = 0;   // 0 = Off
	private static final String KEY_AUTOBACKUP_DAYS = "autobackup_days";
    public static final int AUTOBACKUP_DEFAULT = 0;   // 0 = Off


    public static int getAutosaveSeconds(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
			.getInt(KEY_AUTOSAVE_SEC, AUTOSAVE_DEFAULT);
    }

    public static void setAutosaveSeconds(Context c, int seconds) {
        seconds = Math.max(AUTOSAVE_MIN, Math.min(AUTOSAVE_MAX, seconds));
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
			.edit().putInt(KEY_AUTOSAVE_SEC, seconds).apply();
    }

	public static boolean isScreenshotAllowed(Context c) {
		return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_SCREENSHOT, SCREENSHOT_DEFAULT);
	}

	public static void setScreenshotAllowed(Context c, boolean allowed) {
		c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_SCREENSHOT, allowed).apply();
	}

	public static boolean isBiometricEnabled(Context c) {
		return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean(KEY_BIOMETRIC, false);
	}

	public static void setBiometricEnabled(Context c, boolean enabled) {
		c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_BIOMETRIC, enabled).apply();
	}
	
	public static int getAutoremoveDays(Context c) {
		return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_AUTOREMOVE_DAYS, AUTOREMOVE_DEFAULT);
	}

	public static void setAutoremoveDays(Context c, int days) {
		c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putInt(KEY_AUTOREMOVE_DAYS, days).apply();
	}
	
    public static int getAutobackupDays(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getInt(KEY_AUTOBACKUP_DAYS, AUTOBACKUP_DEFAULT);
    }

    public static void setAutobackupDays(Context c, int days) {
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putInt(KEY_AUTOBACKUP_DAYS, days).apply();
    }
}