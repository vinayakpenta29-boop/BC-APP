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
                // 1️⃣ Verify authentication status first
                if (com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() == null) {
                    intent = new Intent(SplashActivity.this, LoginActivity.class);
                } 
                // 2️⃣ Verify application level access restrictions next
                else if (AppLockManager.isLockEnabled(this)) {
                   intent = new Intent(SplashActivity.this, LockActivity.class);
                } 
                // 3️⃣ Open workspace directly if authenticated and lock is disabled
               else {
                    intent = new Intent(SplashActivity.this, MainActivity.class);
                }

                startActivity(intent);
                finish();
            } catch (Exception e) {
                e.printStackTrace();
                startActivity(new Intent(SplashActivity.this, LoginActivity.class));
                finish();
            }

        }, SPLASH_TIME);
    }
}
