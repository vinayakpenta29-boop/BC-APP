package com.example.bcapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_TIME = 800;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_splash);

        new Handler().postDelayed(() -> {

            try {

                Intent intent;

                // 🔐 Check App Lock using Manager
                if (AppLockManager.isLockEnabled(this)) {

                    // Open Lock Screen
                    intent = new Intent(
                            SplashActivity.this,
                            LockActivity.class
                    );

                } else {

                    // Open App Normally
                    intent = new Intent(
                            SplashActivity.this,
                            MainActivity.class
                    );
                }

                startActivity(intent);
                finish();

            } catch (Exception e) {
                e.printStackTrace();

                // fallback (never crash splash)
                startActivity(new Intent(
                        SplashActivity.this,
                        MainActivity.class));

                finish();
            }

        }, SPLASH_TIME);
    }
}
