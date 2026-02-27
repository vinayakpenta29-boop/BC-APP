package com.example.bcapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class ModeActivity extends AppCompatActivity {

    Button btnOnline, btnOffline;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mode);

        btnOnline = findViewById(R.id.btnOnline);
        btnOffline = findViewById(R.id.btnOffline);

        SharedPreferences pref =
                getSharedPreferences("APP_MODE", MODE_PRIVATE);

        // ONLINE MODE
        btnOnline.setOnClickListener(v -> {

            pref.edit()
                    .putBoolean("ONLINE_MODE", true)
                    .apply();

            startActivity(new Intent(this, MainActivity.class));
            finish();
        });

        // OFFLINE MODE
        btnOffline.setOnClickListener(v -> {

            pref.edit()
                    .putBoolean("ONLINE_MODE", false)
                    .apply();

            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }
}
