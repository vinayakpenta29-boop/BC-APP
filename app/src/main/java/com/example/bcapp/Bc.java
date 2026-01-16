package com.example.bcapp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Bc {
    public String name;
    public int months;
    public String startDateIso; // "yyyy-MM-dd"
    public List<String> members = new ArrayList<>();
    public boolean afterTaken;

    // NEW: amount entered when After Taken BC is checked
    public double afterTakenAmount = 0.0;

    public List<Double> amounts = new ArrayList<>();

    // key: member_monthIndex (paid or not)
    public Map<String, Boolean> paid = new HashMap<>();

    // key: member_monthIndex → paid amount
    public HashMap<String, Double> paidAmount = new HashMap<>();

    // 🔹 STEP 2 NEW: Store all payment entries (multiple + partial)
    public List<PaymentEntry> payments = new ArrayList<>();

    public HashMap<String, List<PaymentEntry>> paymentEntries = new HashMap<>();
    // 🔹 NEW: Paid BC per member
    public HashMap<String, Double> paidBcAmount = new HashMap<>();

    // REQUIRED: no-argument constructor (Room/Gson)
    public Bc() {
        this.name = "";
        this.months = 0;
        this.startDateIso = "";
        this.members = new ArrayList<>();
        this.afterTaken = false;
        this.afterTakenAmount = 0.0;
        this.amounts = new ArrayList<>();
        this.paid = new HashMap<>();
        this.paidAmount = new HashMap<>();
        this.payments = new ArrayList<>();
    }

    // Original constructor
    public Bc(String name, int months, String startDateIso) {
        this.name = name;
        this.months = months;
        this.startDateIso = startDateIso;
        this.members = new ArrayList<>();
        this.afterTaken = false;
        this.afterTakenAmount = 0.0;
        this.amounts = new ArrayList<>();
        this.paid = new HashMap<>();
        this.paidAmount = new HashMap<>();
        this.payments = new ArrayList<>();
    }

    public String getPaidKey(String member, int monthIndex) {
        return member + "_" + monthIndex;
    }

    // ✅ STEP 2: Get all payments for a member in a month
    public List<PaymentEntry> getPaymentsFor(String member, int monthIndex) {
        List<PaymentEntry> list = new ArrayList<>();
        for (PaymentEntry p : payments) {
            if (p.member.equals(member) && p.monthIndex == monthIndex) {
                list.add(p);
            }
        }
        return list;
    }

    // ✅ STEP 2: Total paid by member (all months)
    public double getTotalPaidForMember(String member) {
        double total = 0.0;
        for (PaymentEntry p : payments) {
            if (p.member.equals(member)) {
                total += p.amount;
            }
        }
        return total;
    }

    // ✅ STEP 2: Expected total for BC
    public double getExpectedTotal() {
        double total = 0.0;
        for (double amt : amounts) {
            total += amt;
        }
        return total;
    }
}
