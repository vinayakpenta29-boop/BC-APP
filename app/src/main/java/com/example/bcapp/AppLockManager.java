package com.example.bcapp;

import android.content.Context;
import android.content.SharedPreferences;

public class AppLockManager {

    private static final String PREF = "APP_LOCK";
    private static final String KEY_ENABLED = "LOCK_ENABLED";
    private static final String KEY_PIN = "LOCK_PIN";

    public static void enableLock(Context c, String pin) {
        SharedPreferences sp =
                c.getSharedPreferences(PREF, Context.MODE_PRIVATE);

        sp.edit()
                .putBoolean(KEY_ENABLED, true)
                .putString(KEY_PIN, pin)
                .apply();
    }

    public static void disableLock(Context c) {
        SharedPreferences sp =
                c.getSharedPreferences(PREF, Context.MODE_PRIVATE);

        sp.edit().putBoolean(KEY_ENABLED, false).apply();
    }

    public static boolean isLockEnabled(Context c) {
        return c.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .getBoolean(KEY_ENABLED, false);
    }

    public static String getPin(Context c) {
        return c.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .getString(KEY_PIN, "");
    }
}
