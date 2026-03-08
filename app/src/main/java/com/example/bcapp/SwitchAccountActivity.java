package com.example.bcapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Map;

public class SwitchAccountActivity extends AppCompatActivity {

    SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences("accounts", MODE_PRIVATE);

        Map<String, ?> accounts = prefs.getAll();

        if(accounts.size() <= 1){

            // Ask for second login
            startActivity(new Intent(this, LoginActivity.class));
            finish();

        }

        // Otherwise show list (RecyclerView or Dialog)
    }
}
