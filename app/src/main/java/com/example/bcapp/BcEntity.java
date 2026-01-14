package com.example.bcapp;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity(tableName = "bc_table")
@TypeConverters({Converters.class})
public class BcEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String name;
    public int months;
    public String startDateIso;
    public boolean afterTaken;

    // store amount when "After Taken BC" is enabled
    public double afterTakenAmount;

    public List<String> members = new ArrayList<>();
    public List<Double> amounts = new ArrayList<>();

    // key: member_monthIndex (paid or not)
    public Map<String, Boolean> paid = new HashMap<>();

    // key: member_monthIndex → total paid amount
    public HashMap<String, Double> paidAmount = new HashMap<>();

    // 🔹 STEP 2 NEW: full payment history (multiple + partial)
    public List<PaymentEntry> payments = new ArrayList<>();

    public BcEntity() { }

    public BcEntity(String name, int months, String startDateIso, boolean afterTaken) {
        this.name = name;
        this.months = months;
        this.startDateIso = startDateIso;
        this.afterTaken = afterTaken;
        this.afterTakenAmount = 0.0;

        // safety init
        this.paidAmount = new HashMap<>();
        this.payments = new ArrayList<>();
    }
}
