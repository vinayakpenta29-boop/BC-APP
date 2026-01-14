package com.example.bcapp;

public class PaymentEntry {

    public String member;
    public int monthIndex;          // 0-based (M1 = 0)
    public double amount;           // Paid amount
    public String paidDateIso;      // yyyy-MM-dd

    // REQUIRED empty constructor (Room / Gson)
    public PaymentEntry() {
    }

    public PaymentEntry(String member, int monthIndex, double amount, String paidDateIso) {
        this.member = member;
        this.monthIndex = monthIndex;
        this.amount = amount;
        this.paidDateIso = paidDateIso;
    }
}
