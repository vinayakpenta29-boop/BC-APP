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

    public List<String> members = new ArrayList<>();
    public List<Double> amounts = new ArrayList<>();
    public Map<String, Boolean> paid = new HashMap<>();

    public BcEntity() { }

    public BcEntity(String name, int months, String startDateIso, boolean afterTaken) {
        this.name = name;
        this.months = months;
        this.startDateIso = startDateIso;
        this.afterTaken = afterTaken;
    }
}
