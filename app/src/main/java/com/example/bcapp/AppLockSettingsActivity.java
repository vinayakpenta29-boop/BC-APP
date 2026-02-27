package com.example.bcapp;

import android.os.Bundle;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class AppLockSettingsActivity extends AppCompatActivity {

    Switch lockSwitch;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_lock_settings);

        lockSwitch = findViewById(R.id.lockSwitch);

        lockSwitch.setChecked(
                AppLockManager.isLockEnabled(this));

        lockSwitch.setOnCheckedChangeListener((btn,isChecked)->{

            if(isChecked){

                PinSetupDialog.show(this, pin -> {
                    AppLockManager.enableLock(this,pin);
                    Toast.makeText(this,"App Lock Enabled",Toast.LENGTH_SHORT).show();
                });

            }else{
                AppLockManager.disableLock(this);
            }
        });
    }
}
