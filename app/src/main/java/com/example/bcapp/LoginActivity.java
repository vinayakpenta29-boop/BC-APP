package com.example.bcapp;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    EditText etEmail, etPassword;
    Button btnLogin;

    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load login layout
        setContentView(R.layout.activity_login);

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance();

        // If user already logged in → go to MainActivity
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {

            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
            return;
        }

        // Connect XML views
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(v -> loginUser());
    }

    private void loginUser() {

        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Email validation
        if (TextUtils.isEmpty(email)) {
            etEmail.setError("Enter Email");
            etEmail.requestFocus();
            return;
        }

        // Password validation
        if (TextUtils.isEmpty(password)) {
            etPassword.setError("Enter Password");
            etPassword.requestFocus();
            return;
        }

        if (password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            etPassword.requestFocus();
            return;
        }

        // Login with Firebase
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {

                    if (task.isSuccessful()) {

                        Toast.makeText(LoginActivity.this,
                                "Login Successful",
                                Toast.LENGTH_SHORT).show();

                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish();

                    } else {

                        // If login fails → create new account
                        auth.createUserWithEmailAndPassword(email, password)
                                .addOnCompleteListener(task2 -> {

                                    if (task2.isSuccessful()) {

                                        Toast.makeText(LoginActivity.this,
                                                "Account Created & Logged In",
                                                Toast.LENGTH_LONG).show();

                                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                                        finish();

                                    } else {

                                        Toast.makeText(LoginActivity.this,
                                                "Error: " + task2.getException().getMessage(),
                                                Toast.LENGTH_LONG).show();
                                    }

                                });

                    }

                });
    }
}
