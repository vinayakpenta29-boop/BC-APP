package com.example.bcapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences pref =
                getSharedPreferences("APP_MODE", MODE_PRIVATE);

        boolean logged =
                pref.getBoolean("LOGGED", false);

        if (logged) {

            // Mode already selected → open app
            startActivity(
                    new Intent(this, MainActivity.class));

        } else {

            // First open → ask Admin or Member
            startActivity(
                    new Intent(this, ModeActivity.class));
        }

        finish();
    }
}
