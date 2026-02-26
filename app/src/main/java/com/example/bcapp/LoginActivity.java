package com.example.bcapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    EditText email, password;
    Button btnLogin;

    FirebaseAuth auth;

    String role;   // ⭐ Admin or Member

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        email = findViewById(R.id.editEmail);
        password = findViewById(R.id.editPassword);
        btnLogin = findViewById(R.id.btnLogin);

        auth = FirebaseAuth.getInstance();

        // ⭐ Get role from LoginTypeActivity
        role = getIntent().getStringExtra("ROLE");

        btnLogin.setOnClickListener(v -> loginUser());
    }

    private void loginUser() {

        String userEmail = email.getText().toString().trim();
        String userPassword = password.getText().toString().trim();

        auth.signInWithEmailAndPassword(userEmail, userPassword)
                .addOnSuccessListener(result -> {

                    FirebaseUser user = auth.getCurrentUser();

                    // ⭐ SAVE LOGIN SESSION (REMEMBER LOGIN)
                    SharedPreferences pref =
                            getSharedPreferences("BC_LOGIN", MODE_PRIVATE);

                    pref.edit()
                            .putBoolean("LOGGED_IN", true)
                            .putString("EMAIL", user.getEmail())
                            .putString("ROLE", role)
                            .apply();

                    // ⭐ OPEN MAIN ACTIVITY
                    Intent i = new Intent(this, MainActivity.class);
                    i.putExtra("USER_EMAIL", user.getEmail());

                    startActivity(i);
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Login Failed",
                                Toast.LENGTH_SHORT).show());
    }
}
