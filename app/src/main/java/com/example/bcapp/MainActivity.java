package com.example.bcapp;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

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

// Data + helpers  
List<Bc> bcData = new ArrayList<>();  
BcManager bcManager;  

// Date formats used everywhere  
final SimpleDateFormat isoFormat =  
        new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());  
final SimpleDateFormat displayFormat =  
        new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());  

@Override  
protected void onCreate(Bundle savedInstanceState) {  
    super.onCreate(savedInstanceState);  
    setContentView(R.layout.activity_main);  

    // Bind views  
    menuButton = findViewById(R.id.menuButton);  
    spinnerBc = findViewById(R.id.spinnerBc);  
    spinnerMember = findViewById(R.id.spinnerMember);  
    editPayDate = findViewById(R.id.editPayDate);  
    editPayAmount = findViewById(R.id.editPayAmount);  
    buttonAdd = findViewById(R.id.buttonAdd);  
    tableContainer = findViewById(R.id.tableContainer);  

    // Create manager that handles all BC logic + UI  
    bcManager = new BcManager(  
            this,  
            menuButton,  
            spinnerBc,  
            spinnerMember,  
            editPayDate,  
            editPayAmount,  
            buttonAdd,  
            tableContainer,  
            bcData,  
            isoFormat,  
            displayFormat  
    );  

    bcManager.init();   // set adapters, menu, listeners, etc.  
}

}
