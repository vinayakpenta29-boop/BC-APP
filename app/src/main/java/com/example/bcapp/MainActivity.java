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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    // UI references
    TextView menuButton;
    Spinner spinnerBc, spinnerMember;
    EditText editPayDate, editPayAmount;
    Button buttonAdd;
    LinearLayout tableContainer;

    // Undo / Redo Buttons
    ImageButton btnUndo, btnRedo;

    // Lock icon
    ImageView imgLock;

    // Data
    List<Bc> bcData = new ArrayList<>();
    BcManager bcManager;

    // Firebase
    FirebaseDatabase database;
    DatabaseReference testRef;

    // Date formats
    final SimpleDateFormat isoFormat =
            new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    final SimpleDateFormat displayFormat =
            new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ✅ STEP 1 — Initialize Firebase
        FirebaseApp.initializeApp(this);
        database = FirebaseDatabase.getInstance();

        // ✅ STEP 2 — Test Firebase Connection
        testRef = database.getReference("test");

        testRef.setValue("BC App Connected")
                .addOnSuccessListener(unused ->
                        Log.d("FIREBASE", "✅ Data sent successfully"))
                .addOnFailureListener(e ->
                        Log.e("FIREBASE", "❌ Failed: " + e.getMessage()));

        // ✅ STEP 3 — Bind Views
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

        // ✅ STEP 4 — Initialize BC Manager
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
    }
}
