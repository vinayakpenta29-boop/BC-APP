package com.example.bcapp;

import android.app.AlertDialog;
import android.os.Bundle;
import androidx.appcompat.widget.SwitchCompat;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class AppLockSettingsActivity extends AppCompatActivity {

    private SwitchCompat lockSwitch;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        try {

            setContentView(R.layout.activity_lock_settings);

            lockSwitch = findViewById(R.id.lockSwitch);

            if (lockSwitch == null) {
                throw new Exception("lockSwitch not found in layout");
            }

            // Load current state
            lockSwitch.setChecked(
                    AppLockManager.isLockEnabled(this)
            );

            lockSwitch.setOnCheckedChangeListener((btn, isChecked) -> {

                try {

                    if (isChecked) {

                        // Setup PIN
                        PinSetupDialog.show(this, pin -> {
                            try {
                                AppLockManager.enableLock(this, pin);
                                Toast.makeText(
                                        this,
                                        "App Lock Enabled",
                                        Toast.LENGTH_SHORT
                                ).show();
                            } catch (Exception e) {
                                showCrashDialog(e);
                            }
                        });

                    } else {

                        AppLockManager.disableLock(this);

                        Toast.makeText(
                                this,
                                "App Lock Disabled",
                                Toast.LENGTH_SHORT
                        ).show();
                    }

                } catch (Exception e) {
                    showCrashDialog(e);
                }
            });

        } catch (Exception e) {
            showCrashDialog(e);
        }
    }

    // ✅ CRASH DETAILS DIALOG
    private void showCrashDialog(Exception e) {

        StringBuilder error = new StringBuilder();
        error.append(e.toString()).append("\n\n");

        for (StackTraceElement element : e.getStackTrace()) {
            error.append(element.toString()).append("\n");
        }

        new AlertDialog.Builder(this)
                .setTitle("Crash Details")
                .setMessage(error.toString())
                .setPositiveButton("OK", null)
                .show();
    }
}
