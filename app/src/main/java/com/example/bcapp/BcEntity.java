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

    // Amount when "After Taken BC" is enabled
    public double afterTakenAmount;

    public List<String> members = new ArrayList<>();
    public List<Double> amounts = new ArrayList<>();

    // key: member_monthIndex → paid or not
    public Map<String, Boolean> paid = new HashMap<>();

    // key: member_monthIndex → total paid amount
    public HashMap<String, Double> paidAmount = new HashMap<>();

    // 🔹 NEW: Paid BC per member
    public HashMap<String, Double> paidBcAmount = new HashMap<>();

    // 🔹 STEP 2: Full payment history (multiple + partial)
    public List<PaymentEntry> payments = new ArrayList<>();

    // ✅ Required by Room
    public BcEntity() {
        this.members = new ArrayList<>();
        this.amounts = new ArrayList<>();
        this.paid = new HashMap<>();
        this.paidAmount = new HashMap<>();
        this.paidBcAmount = new HashMap<>();
        this.payments = new ArrayList<>();
    }

    public BcEntity(String name, int months, String startDateIso, boolean afterTaken) {
        this.name = name;
        this.months = months;
        this.startDateIso = startDateIso;
        this.afterTaken = afterTaken;
        this.afterTakenAmount = 0.0;

        // Safety init
        this.members = new ArrayList<>();
        this.amounts = new ArrayList<>();
        this.paid = new HashMap<>();
        this.paidAmount = new HashMap<>();
        this.paidBcAmount = new HashMap<>();
        this.payments = new ArrayList<>();
    }
}
