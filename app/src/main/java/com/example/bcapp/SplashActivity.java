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
                getSharedPreferences("BC_LOGIN", MODE_PRIVATE);

        boolean loggedIn = pref.getBoolean("LOGGED_IN", false);

        if (loggedIn) {

            String email = pref.getString("EMAIL", "");

            Intent i = new Intent(this, MainActivity.class);
            i.putExtra("USER_EMAIL", email);
            startActivity(i);

        } else {
            startActivity(new Intent(this,
                    LoginTypeActivity.class));
        }

        finish();
    }
}
