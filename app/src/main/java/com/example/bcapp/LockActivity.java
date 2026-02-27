package com.example.bcapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import java.util.concurrent.Executor;

public class LockActivity extends AppCompatActivity {

    EditText editPin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lock);

        editPin = findViewById(R.id.editPin);

        showBiometric();
    }

    private void showBiometric() {

        Executor executor =
                ContextCompat.getMainExecutor(this);

        BiometricPrompt biometricPrompt =
                new BiometricPrompt(this, executor,
                        new BiometricPrompt.AuthenticationCallback() {

                            @Override
                            public void onAuthenticationSucceeded(
                                    @NonNull BiometricPrompt.AuthenticationResult result) {

                                unlock();
                            }
                        });

        BiometricPrompt.PromptInfo promptInfo =
                new BiometricPrompt.PromptInfo.Builder()
                        .setTitle("Unlock BC App")
                        .setSubtitle("Use fingerprint")
                        .setNegativeButtonText("Use MPIN")
                        .build();

        biometricPrompt.authenticate(promptInfo);
    }

    public void checkPin(android.view.View v) {

        String entered = editPin.getText().toString();
        String saved = AppLockManager.getPin(this);

        if (entered.equals(saved)) {
            unlock();
        } else {
            Toast.makeText(this,"Wrong MPIN",Toast.LENGTH_SHORT).show();
        }
    }

    private void unlock() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
