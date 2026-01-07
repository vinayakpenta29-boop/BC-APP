package com.example.bcapp;

import androidx.room.TypeConverter;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

public class Converters {
    static Gson gson = new Gson();

    @TypeConverter
    public static List<String> stringToList(String json) {
        if (json == null) return null;
        Type type = new TypeToken<List<String>>() {}.getType();
        return gson.fromJson(json, type);
    }

    @TypeConverter
    public static String listToString(List<String> list) {
        if (list == null) return null;
        return gson.toJson(list);
    }

    @TypeConverter
    public static Map<String, Boolean> stringToMap(String json) {
        if (json == null) return null;
        Type type = new TypeToken<Map<String, Boolean>>() {}.getType();
        return gson.fromJson(json, type);
    }

    @TypeConverter
    public static String mapToString(Map<String, Boolean> map) {
        if (map == null) return null;
        return gson.toJson(map);
    }

    @TypeConverter
    public static List<Double> stringToDoubleList(String json) {
        if (json == null) return null;
        Type type = new TypeToken<List<Double>>() {}.getType();
        return gson.fromJson(json, type);
    }

    @TypeConverter
    public static String doubleListToString(List<Double> list) {
        if (list == null) return null;
        return gson.toJson(list);
    }
}
