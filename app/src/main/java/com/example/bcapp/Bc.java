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

    // ✅ NEW: key: member_monthIndex → paid amount
    public HashMap<String, Double> paidAmount = new HashMap<>();

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
        this.paidAmount = new HashMap<>(); // ✅ IMPORTANT
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
        this.paidAmount = new HashMap<>(); // ✅ IMPORTANT
    }

    public String getPaidKey(String member, int monthIndex) {
        return member + "_" + monthIndex;
    }
}
