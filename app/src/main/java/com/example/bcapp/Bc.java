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
    public List<Double> amounts = new ArrayList<>();
    public Map<String, Boolean> paid = new HashMap<>(); // key: member_index

    public Bc(String name, int months, String startDateIso) {
        this.name = name;
        this.months = months;
        this.startDateIso = startDateIso;
    }

    public String getPaidKey(String member, int monthIndex) {
        return member + "_" + monthIndex;
    }
}
