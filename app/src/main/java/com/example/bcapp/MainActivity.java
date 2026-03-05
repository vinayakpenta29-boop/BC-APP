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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    SwipeRefreshLayout swipeRefresh;

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
        testRef = database.getReference("test");

        testRef.setValue("BC App Connected")
                .addOnSuccessListener(unused ->
                        Log.d("FIREBASE", "✅ Firebase Connected"))
                .addOnFailureListener(e ->
                        Log.e("FIREBASE", "❌ " + e.getMessage()));

        /* ✅ STEP 3 — BIND VIEWS */
        swipeRefresh = findViewById(R.id.swipeRefresh);
        

        swipeRefresh.setColorSchemeResources(
                android.R.color.holo_blue_dark,
                android.R.color.holo_green_dark,
                android.R.color.holo_red_dark
        );
        swipeRefresh.setEnabled(false);

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
        swipeRefresh.setOnRefreshListener(() -> {

            bcManager.restoreFromFirebase();

            swipeRefresh.postDelayed(() ->
                    swipeRefresh.setRefreshing(false), 1000);
        });
    }
}
