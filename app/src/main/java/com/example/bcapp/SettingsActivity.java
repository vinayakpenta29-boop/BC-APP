package com.example.bcapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

public class SettingsActivity extends AppCompatActivity {

    TextView tvCurrentAccount;
    
    FirebaseAuth auth;
    SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        tvCurrentAccount = findViewById(R.id.tvCurrentAccount);
        
        auth = FirebaseAuth.getInstance();
        prefs = getSharedPreferences("accounts", MODE_PRIVATE);

        if(auth.getCurrentUser()!=null){
            tvCurrentAccount.setText(auth.getCurrentUser().getEmail());
        }

        });
}

