package com.example.bcapp;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;
import androidx.room.Ignore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Entity(tableName = "bc_table")
@TypeConverters({Converters.class})
public class BcEntity {

    @PrimaryKey(autoGenerate = true)
    public int id;

    // ================= BASIC INFO =================

    public String name;
    public int months;
    public String startDateIso;

    // false = Monthly (default)
    // true = Weekly
    public boolean isWeekly;

    // ================= AFTER TAKEN =================

    public boolean afterTaken;
    public double afterTakenAmount;

    // ================= CONTRIBUTION AMOUNT =================

    public List<Double> amounts;

    // ================= RECEIVE AMOUNT =================

    // true = Fixed, false = Random
    public boolean isReceiveAmountFixed;

    public List<Double> receiveAmounts;

    // ================= MEMBERS =================

    public List<String> members;

    public Map<String, Boolean> paid;
    public HashMap<String, Double> paidAmount;
    public HashMap<String, Double> paidBcAmount;

    // ================= PAYMENT HISTORY =================

    public List<PaymentEntry> payments;

    // =====================================================
    // ✅ REQUIRED EMPTY CONSTRUCTOR (Room)
    // =====================================================

    public BcEntity() {

        this.members = new ArrayList<>();
        this.amounts = new ArrayList<>();
        this.receiveAmounts = new ArrayList<>();

        this.paid = new HashMap<>();
        this.paidAmount = new HashMap<>();
        this.paidBcAmount = new HashMap<>();

        this.payments = new ArrayList<>();

        this.isReceiveAmountFixed = true;
        this.afterTakenAmount = 0.0;
        this.isWeekly = true; // default Monthly
    }

    // =====================================================
    // ✅ MAIN CONSTRUCTOR (New)
    // =====================================================
    
    @Ignore
    public BcEntity(String name,
                    int months,
                    String startDateIso,
                    boolean afterTaken,
                    boolean isWeekly) {

        this.name = name;
        this.months = months;
        this.startDateIso = startDateIso;
        this.afterTaken = afterTaken;
        this.isWeekly = isWeekly;

        this.afterTakenAmount = 0.0;
        this.isReceiveAmountFixed = true;

        this.members = new ArrayList<>();
        this.amounts = new ArrayList<>();
        this.receiveAmounts = new ArrayList<>();

        this.paid = new HashMap<>();
        this.paidAmount = new HashMap<>();
        this.paidBcAmount = new HashMap<>();

        this.payments = new ArrayList<>();
    }

    // =====================================================
    // 🔥 BACKWARD COMPATIBILITY CONSTRUCTOR (IMPORTANT)
    // =====================================================
    @Ignore
    public BcEntity(String name,
                    int months,
                    String startDateIso,
                    boolean afterTaken) {

        this(name, months, startDateIso, afterTaken, false); // default Monthly
    }
}
