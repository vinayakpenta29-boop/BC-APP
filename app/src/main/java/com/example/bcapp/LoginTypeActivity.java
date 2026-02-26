package com.example.bcapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class LoginTypeActivity extends AppCompatActivity {

    Button btnAdmin, btnMember;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_type);

        btnAdmin = findViewById(R.id.btnAdmin);
        btnMember = findViewById(R.id.btnMember);

        btnAdmin.setOnClickListener(v -> openLogin("ADMIN"));
        btnMember.setOnClickListener(v -> openLogin("MEMBER"));
    }

    private void openLogin(String role) {
        Intent i = new Intent(this, LoginActivity.class);
        i.putExtra("ROLE", role);
        startActivity(i);
    }
}
