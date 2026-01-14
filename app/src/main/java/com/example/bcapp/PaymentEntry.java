package com.example.bcapp;

public class PaymentEntry {

    public String member;
    public int monthIndex;          // 0-based (M1 = 0)
    public double amount;           // Paid amount
    public String paidDateIso;      // yyyy-MM-dd

    // ✅ ALIAS FIELD (used by popup UI)
    public String date;             // yyyy-MM-dd

    // REQUIRED empty constructor (Room / Gson)
    public PaymentEntry() {
    }

    // EXISTING constructor (DO NOT REMOVE)
    public PaymentEntry(String member, int monthIndex, double amount, String paidDateIso) {
        this.member = member;
        this.monthIndex = monthIndex;
        this.amount = amount;
        this.paidDateIso = paidDateIso;
        this.date = paidDateIso; // keep both in sync
    }

    // ✅ NEW constructor (matches markInstallment usage)
    public PaymentEntry(String member, int monthIndex, String date, double amount) {
        this.member = member;
        this.monthIndex = monthIndex;
        this.amount = amount;
        this.paidDateIso = date;
        this.date = date;
    }
}
