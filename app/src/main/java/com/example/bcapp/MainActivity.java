package com.example.bcapp;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import android.content.Intent;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    // UI
    TextView menuButton;
    Spinner spinnerBc, spinnerMember;
    EditText editPayDate, editPayAmount;
    Button buttonAdd;
    LinearLayout tableContainer;

    ImageButton btnUndo, btnRedo;
    ImageView imgLock;

    // Data
    List<Bc> bcData = new ArrayList<>();
    BcManager bcManager;

    // Firebase
    FirebaseDatabase database;
    DatabaseReference testRef;

    FirebaseAuth auth;
    FirebaseUser currentUser;
    String userId;

    // Date Formats
    final SimpleDateFormat isoFormat =
            new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    final SimpleDateFormat displayFormat =
            new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Thread.setDefaultUncaughtExceptionHandler(
                    new CrashHandler(this)
            );
        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();

        if (currentUser == null) {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
            return;
       }

        userId = currentUser.getUid();

        /* ✅ STEP 1 — FIREBASE INIT */
        FirebaseApp.initializeApp(this);

        database = FirebaseDatabase.getInstance();

        // ⭐ Enable Offline Cache + Realtime Sync
        try {
            database.setPersistenceEnabled(true);
        } catch (Exception ignored) {
            // Prevent crash if already enabled
        }

        /* ✅ STEP 2 — CONNECTION TEST */
        testRef = database.getReference("users")
                .child(userId)
                .child("test");

        testRef.setValue("BC App Connected")
                .addOnSuccessListener(unused ->
                        Log.d("FIREBASE", "✅ Firebase Connected"))
                .addOnFailureListener(e ->
                        Log.e("FIREBASE", "❌ " + e.getMessage()));

        /* ✅ STEP 3 — BIND VIEWS */
        
        menuButton = findViewById(R.id.menuButton);
        spinnerBc = findViewById(R.id.spinnerBc);
        spinnerMember = findViewById(R.id.spinnerMember);
        editPayDate = findViewById(R.id.editPayDate);
        editPayAmount = findViewById(R.id.editPayAmount);
        buttonAdd = findViewById(R.id.buttonAdd);
        tableContainer = findViewById(R.id.tableContainer);

        btnUndo = findViewById(R.id.btnUndo);
        btnRedo = findViewById(R.id.btnRedo);
        imgLock = findViewById(R.id.imgLock);

        /* ✅ STEP 4 — INIT BC MANAGER */
        bcManager = new BcManager(
                this,
                menuButton,
                spinnerBc,
                spinnerMember,
                editPayDate,
                editPayAmount,
                buttonAdd,
                tableContainer,
                btnUndo,
                btnRedo,
                imgLock,
                bcData,
                isoFormat,
                displayFormat
        );

        bcManager.init();

        /* ⭐ REALTIME SYNC STARTS HERE */
        bcManager.startRealtimeSync();

        /* ✅ OPTIONAL MANUAL REFRESH */

            bcManager.restoreFromFirebase();

    }
}
