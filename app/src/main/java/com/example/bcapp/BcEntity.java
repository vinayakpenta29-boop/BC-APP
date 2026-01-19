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

    // ================= BASIC INFO =================

    public String name;
    public int months;
    public String startDateIso;

    // ================= AFTER TAKEN =================

    public boolean afterTaken;

    // Amount when "After Taken BC" is enabled
    public double afterTakenAmount;

    // ================= CONTRIBUTION AMOUNT =================

    // Monthly contribution amount
    public List<Double> amounts;

    // ================= 🔹 RECEIVE AMOUNT =================

    // true = Fixed, false = Random
    public boolean isReceiveAmountFixed;

    // If fixed → index 0 used
    // If random → size == months
    public List<Double> receiveAmounts;

    // ================= MEMBERS =================

    public List<String> members;

    // key: member_monthIndex → paid or not
    public Map<String, Boolean> paid;

    // key: member_monthIndex → total paid amount
    public HashMap<String, Double> paidAmount;

    // Paid BC per member
    public HashMap<String, Double> paidBcAmount;

    // ================= PAYMENT HISTORY =================

    // Full payment history (multiple + partial)
    public List<PaymentEntry> payments;

    // ================= CONSTRUCTORS =================

    // ✅ Required empty constructor for Room
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
    }

    public BcEntity(String name, int months, String startDateIso, boolean afterTaken) {
        this.name = name;
        this.months = months;
        this.startDateIso = startDateIso;
        this.afterTaken = afterTaken;

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
}
