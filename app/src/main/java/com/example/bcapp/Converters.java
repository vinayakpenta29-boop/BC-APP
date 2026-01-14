package com.example.bcapp;

import androidx.room.TypeConverter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Converters {

    static Gson gson = new Gson();

    // ---------- List<String> ----------
    @TypeConverter
    public static List<String> stringToList(String json) {
        if (json == null) return new ArrayList<>();
        Type type = new TypeToken<List<String>>() {}.getType();
        return gson.fromJson(json, type);
    }

    @TypeConverter
    public static String listToString(List<String> list) {
        if (list == null) return null;
        return gson.toJson(list);
    }

    // ---------- Map<String, Boolean> ----------
    @TypeConverter
    public static Map<String, Boolean> stringToMap(String json) {
        if (json == null) return new HashMap<>();
        Type type = new TypeToken<Map<String, Boolean>>() {}.getType();
        return gson.fromJson(json, type);
    }

    @TypeConverter
    public static String mapToString(Map<String, Boolean> map) {
        if (map == null) return null;
        return gson.toJson(map);
    }

    // ---------- List<Double> ----------
    @TypeConverter
    public static List<Double> stringToDoubleList(String json) {
        if (json == null) return new ArrayList<>();
        Type type = new TypeToken<List<Double>>() {}.getType();
        return gson.fromJson(json, type);
    }

    @TypeConverter
    public static String doubleListToString(List<Double> list) {
        if (list == null) return null;
        return gson.toJson(list);
    }

    // ---------- HashMap<String, Double> (PAID AMOUNT) ----------
    @TypeConverter
    public static HashMap<String, Double> stringToPaidAmountMap(String json) {
        if (json == null) return new HashMap<>();
        Type type = new TypeToken<HashMap<String, Double>>() {}.getType();
        return gson.fromJson(json, type);
    }

    @TypeConverter
    public static String paidAmountMapToString(HashMap<String, Double> map) {
        if (map == null) return null;
        return gson.toJson(map);
    }

    // =====================================================
    // 🔹 STEP 2 NEW: List<PaymentEntry>
    // =====================================================

    @TypeConverter
    public static List<PaymentEntry> stringToPaymentEntryList(String json) {
        if (json == null) return new ArrayList<>();
        Type type = new TypeToken<List<PaymentEntry>>() {}.getType();
        return gson.fromJson(json, type);
    }

    @TypeConverter
    public static String paymentEntryListToString(List<PaymentEntry> list) {
        if (list == null) return null;
        return gson.toJson(list);
    }
}
