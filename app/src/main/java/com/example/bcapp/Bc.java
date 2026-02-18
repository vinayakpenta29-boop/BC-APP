package com.example.bcapp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Calendar;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class Bc {

    // ================= BASIC INFO =================

    public String name;
    public int months;
    public String startDateIso; // "yyyy-MM-dd"

    // false = Monthly (default)
    // true = Weekly
    public boolean isWeekly = false;

    public List<String> members;

    // ================= AFTER TAKEN =================

    public boolean afterTaken;
    public double afterTakenAmount;

    // ================= CONTRIBUTION =================

    public List<Double> amounts;

    // ================= RECEIVE AMOUNT =================

    // true = Fixed, false = Random
    public boolean isReceiveAmountFixed;

    // If fixed → index 0 used
    // If random → size == months
    public List<Double> receiveAmounts;

    // ================= PAYMENT TRACKING =================

    // key: member_monthIndex → paid or not
    public Map<String, Boolean> paid;

    // key: member_monthIndex → total paid amount
    public HashMap<String, Double> paidAmount;

    // All payment entries (multiple + partial)
    public List<PaymentEntry> payments;

    // key: member_monthIndex → list of payments
    public HashMap<String, List<PaymentEntry>> paymentEntries;

    // Paid BC per member
    public HashMap<String, Double> paidBcAmount;

    // =====================================================
    // ✅ REQUIRED NO-ARG CONSTRUCTOR (Room / Gson Safe)
    // =====================================================

    public Bc() {

        this.name = "";
        this.months = 0;
        this.startDateIso = "";
        this.isWeekly = false;

        this.members = new ArrayList<>();

        this.afterTaken = false;
        this.afterTakenAmount = 0.0;

        this.amounts = new ArrayList<>();

        this.isReceiveAmountFixed = true;
        this.receiveAmounts = new ArrayList<>();

        this.paid = new HashMap<>();
        this.paidAmount = new HashMap<>();

        this.payments = new ArrayList<>();
        this.paymentEntries = new HashMap<>();
        this.paidBcAmount = new HashMap<>();
    }

    // =====================================================
    // ✅ MAIN CONSTRUCTOR (New)
    // =====================================================

    public Bc(String name,
              int months,
              String startDateIso,
              boolean isWeekly) {

        this.name = name;
        this.months = months;
        this.startDateIso = startDateIso;
        this.isWeekly = isWeekly;

        this.members = new ArrayList<>();

        this.afterTaken = false;
        this.afterTakenAmount = 0.0;

        this.amounts = new ArrayList<>();

        this.isReceiveAmountFixed = true;
        this.receiveAmounts = new ArrayList<>();

        this.paid = new HashMap<>();
        this.paidAmount = new HashMap<>();

        this.payments = new ArrayList<>();
        this.paymentEntries = new HashMap<>();
        this.paidBcAmount = new HashMap<>();
    }

    // =====================================================
    // 🔥 BACKWARD COMPATIBILITY CONSTRUCTOR (IMPORTANT)
    // =====================================================

    public Bc(String name, int months, String startDateIso) {
        this(name, months, startDateIso, false); // default Monthly
    }

    // =====================================================
    // 🔹 HELPER METHODS
    // =====================================================

    public String getPaidKey(String member, int monthIndex) {
        return member + "_" + monthIndex;
    }

    public List<PaymentEntry> getPaymentsFor(String member, int monthIndex) {
        List<PaymentEntry> list = new ArrayList<>();
        if (payments == null) return list;

        for (PaymentEntry p : payments) {
            if (p.member.equals(member) && p.monthIndex == monthIndex) {
                list.add(p);
            }
        }
        return list;
    }

    public double getTotalPaidForMember(String member) {
        double total = 0.0;
        if (payments == null) return total;

        for (PaymentEntry p : payments) {
            if (p.member.equals(member)) {
                total += p.amount;
            }
        }
        return total;
    }

    public double getExpectedTotal() {
        double total = 0.0;
        if (amounts == null) return total;

        for (double amt : amounts) {
            total += amt;
        }
        return total;
    }

    public double getReceiveAmountForMonth(int monthIndex) {

        if (receiveAmounts == null || receiveAmounts.isEmpty()) {
            return 0.0;
        }

        if (isReceiveAmountFixed) {
            return receiveAmounts.get(0);
        } else {
            return (monthIndex < receiveAmounts.size())
                    ? receiveAmounts.get(monthIndex)
                    : 0.0;
        }
    }

    public int getCurrentInstallmentIndex() {

        if (startDateIso == null || startDateIso.isEmpty()) return -1;

        Calendar startCal = Calendar.getInstance();
        Calendar todayCal = Calendar.getInstance();

        try {
            Date startDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    .parse(startDateIso);

            if (startDate == null) return -1;

            startCal.setTime(startDate);

        } catch (Exception e) {
            return -1;
        }

        if (isWeekly) {

            long diffMillis =
                    todayCal.getTimeInMillis() - startCal.getTimeInMillis();

            long diffDays = diffMillis / (1000 * 60 * 60 * 24);

            return (int) (diffDays / 7);

        } else {

            return
                    (todayCal.get(Calendar.YEAR) - startCal.get(Calendar.YEAR)) * 12 +
                    (todayCal.get(Calendar.MONTH) - startCal.get(Calendar.MONTH));
        }
    }
}
