package com.example.bcapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SettingsActivity extends AppCompatActivity {

    TextView tvCurrentAccount;
    Button btnLogout;

    FirebaseAuth auth;
    SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        tvCurrentAccount = findViewById(R.id.tvCurrentAccount);
        btnLogout = findViewById(R.id.btnLogout);

        auth = FirebaseAuth.getInstance();
        prefs = getSharedPreferences("accounts", MODE_PRIVATE);

        FirebaseUser user = auth.getCurrentUser();

        if (user != null) {
            tvCurrentAccount.setText(user.getEmail());
        } else {
            tvCurrentAccount.setText("No account logged in");
        }

        // Logout button
        btnLogout.setOnClickListener(v -> {

            auth.signOut();

            Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);

        });
    }
}
