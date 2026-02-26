package com.example.bcapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    EditText email, password;
    Button btnLogin;

    FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        email = findViewById(R.id.editEmail);
        password = findViewById(R.id.editPassword);
        btnLogin = findViewById(R.id.btnLogin);

        auth = FirebaseAuth.getInstance();

        btnLogin.setOnClickListener(v -> loginUser());
    }

    private void loginUser() {

        String e = email.getText().toString().trim();
        String p = password.getText().toString().trim();

        auth.signInWithEmailAndPassword(e, p)
                .addOnSuccessListener(result -> {

                    FirebaseUser user = auth.getCurrentUser();

                    Intent i =
                            new Intent(this, MainActivity.class);

                    i.putExtra("USER_EMAIL",
                            user.getEmail());

                    startActivity(i);
                    finish();
                })
                .addOnFailureListener(e1 ->
                        Toast.makeText(this,
                                "Login Failed",
                                Toast.LENGTH_SHORT).show());
    }

    SharedPreferences pref =
            getSharedPreferences("BC_LOGIN", MODE_PRIVATE);

    pref.edit()
            .putBoolean("LOGGED_IN", true)
            .putString("EMAIL", userEmail)
            .putString("ROLE", role)
            .apply();
}
