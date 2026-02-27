package com.example.bcapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_TIME = 800; // 0.8 sec

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Optional: you can create splash layout
        setContentView(R.layout.activity_splash);

        new Handler().postDelayed(() -> {

            SharedPreferences prefs =
                    getSharedPreferences("AppLockPrefs", MODE_PRIVATE);

            boolean isLockEnabled =
                    prefs.getBoolean("APP_LOCK_ENABLED", false);

            Intent intent;

            // 🔐 If App Lock ON → Lock Screen
            if (isLockEnabled) {
                intent = new Intent(SplashActivity.this,
                        LockActivity.class);
            }
            // 🚀 Normal App Open
            else {
                intent = new Intent(SplashActivity.this,
                        MainActivity.class);
            }

            startActivity(intent);
            finish();

        }, SPLASH_TIME);
    }
}
