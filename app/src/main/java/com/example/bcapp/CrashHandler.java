package com.example.bcapp;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Looper;
import android.util.Log;

public class CrashHandler implements Thread.UncaughtExceptionHandler {

    private final Activity activity;

    public CrashHandler(Activity activity) {
        this.activity = activity;
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {

        String crash = Log.getStackTraceString(throwable);

        Log.e("APP_CRASH", crash);

        new Thread(() -> {
            Looper.prepare();

            new AlertDialog.Builder(activity)
                    .setTitle("App Crash Detected")
                    .setMessage(crash)
                    .setPositiveButton("Restart App", (d, w) -> {

                        Intent intent = new Intent(activity, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                                Intent.FLAG_ACTIVITY_CLEAR_TASK);

                        activity.startActivity(intent);
                        activity.finish();
                    })
                    .setNegativeButton("Close", (d, w) -> activity.finish())
                    .setCancelable(false)
                    .show();

            Looper.loop();

        }).start();
    }
}
