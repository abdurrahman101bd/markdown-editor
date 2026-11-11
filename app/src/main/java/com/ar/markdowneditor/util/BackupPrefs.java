package com.ar.markdowneditor.util;

import android.content.Context;
import android.content.SharedPreferences;

public class BackupPrefs {

    private static final String PREFS = "backup_prefs";
    private static final String KEY_MANUAL_TREE_URI = "manual_tree_uri";
    private static final String KEY_LAST_BACKUP_AT = "last_backup_at";
    private static final String KEY_LAST_BACKUP_NAME = "last_backup_name";
    private static final String KEY_AUTO_LAST_AT = "auto_last_at";
    private static final String KEY_AUTO_LAST_NAME = "auto_last_name";

    public static String getManualTreeUri(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_MANUAL_TREE_URI, null);
    }

    public static void setManualTreeUri(Context c, String uri) {
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(KEY_MANUAL_TREE_URI, uri).apply();
    }

    public static long getLastBackupAt(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getLong(KEY_LAST_BACKUP_AT, 0);
    }

    public static void setLastBackupAt(Context c, long time) {
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putLong(KEY_LAST_BACKUP_AT, time).apply();
    }

    public static String getLastBackupName(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_LAST_BACKUP_NAME, null);
    }

    public static void setLastBackupName(Context c, String name) {
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(KEY_LAST_BACKUP_NAME, name).apply();
    }
    
    public static void clearLastAutoBackup(Context c) {
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_AUTO_LAST_AT)
                .remove(KEY_AUTO_LAST_NAME)
                .apply();
    }
    
    public static long getLastAutoBackupAt(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getLong(KEY_AUTO_LAST_AT, 0);
    }

    public static void setLastAutoBackupAt(Context c, long time) {
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putLong(KEY_AUTO_LAST_AT, time).apply();
    }

    public static String getLastAutoBackupName(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_AUTO_LAST_NAME, null);
    }

    public static void setLastAutoBackupName(Context c, String name) {
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString(KEY_AUTO_LAST_NAME, name).apply();
    }
}