package com.grupo3.signtranslator;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@SuppressWarnings("ClassCanBeRecord")
public class Routine {
    private static final String TAG = "Routine";

    private final long id;
    private final String title;
    private final int hour;
    private final int minute;
    private final List<Integer> days; // Calendar.MONDAY, etc.

    public Routine(long id, String title, int hour, int minute, List<Integer> days) {
        this.id = id;
        this.title = title;
        this.hour = hour;
        this.minute = minute;
        this.days = days != null ? days : new ArrayList<>();
    }

    public long getId() { return id; }
    public String getTitle() { return title; }
    public int getHour() { return hour; }
    public int getMinute() { return minute; }
    public List<Integer> getDays() { return days; }

    public String getFormattedTime() {
        return String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
    }

    @SuppressWarnings("SpellCheckingInspection")
    public String getFormattedDays() {
        if (days.isEmpty() || days.size() == 7) {
            return "Todos los días";
        }
        if (days.size() == 5 &&
                days.contains(Calendar.MONDAY) &&
                days.contains(Calendar.TUESDAY) &&
                days.contains(Calendar.WEDNESDAY) &&
                days.contains(Calendar.THURSDAY) &&
                days.contains(Calendar.FRIDAY)) {
            return "Lunes a Viernes";
        }
        if (days.size() == 2 &&
                days.contains(Calendar.SATURDAY) &&
                days.contains(Calendar.SUNDAY)) {
            return "Fines de semana";
        }

        List<Integer> sortedDays = new ArrayList<>(days);
        Collections.sort(sortedDays);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < sortedDays.size(); i++) {
            sb.append(getDayName(sortedDays.get(i)));
            if (i < sortedDays.size() - 1) sb.append(", ");
        }
        return sb.toString();
    }

    private static String getDayName(int calendarDay) {
        return switch (calendarDay) {
            case Calendar.MONDAY -> "Lun";
            case Calendar.TUESDAY -> "Mar";
            case Calendar.WEDNESDAY -> "Mié";
            case Calendar.THURSDAY -> "Jue";
            case Calendar.FRIDAY -> "Vie";
            case Calendar.SATURDAY -> "Sáb";
            case Calendar.SUNDAY -> "Dom";
            default -> "";
        };
    }

    @SuppressWarnings("SpellCheckingInspection")
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("id", id);
            json.put("title", title);
            json.put("hour", hour);
            json.put("minute", minute);
            JSONArray arr = new JSONArray();
            for (int d : days) arr.put(d);
            json.put("days", arr);
        } catch (JSONException e) {
            Log.e(TAG, "Error serializando Routine a JSON", e);
        }
        return json;
    }

    @SuppressWarnings("SpellCheckingInspection")
    public static Routine fromJson(JSONObject json) {
        try {
            long id = json.optLong("id", System.currentTimeMillis());
            String title = json.optString("title", "Rutina");
            int hour = json.optInt("hour", 8);
            int minute = json.optInt("minute", 0);
            List<Integer> days = new ArrayList<>();
            JSONArray arr = json.optJSONArray("days");
            if (arr != null) {
                for (int i = 0; i < arr.length(); i++) {
                    days.add(arr.getInt(i));
                }
            }
            return new Routine(id, title, hour, minute, days);
        } catch (JSONException e) {
            Log.e(TAG, "Error parseando Routine desde JSON", e);
            return null;
        }
    }
}