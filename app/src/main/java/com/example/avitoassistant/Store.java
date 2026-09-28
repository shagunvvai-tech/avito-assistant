package com.example.avitoassistant;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Store {
    private static final String PREFS = "avito_assistant";
    private static final String KEY_EVENTS = "events";
    private static final String KEY_WORDS = "keywords";
    private static final String KEY_MAX_PRICE = "max_price";

    public static class Event {
        public long time;
        public String title;
        public String text;
        public String pkg;
        public boolean highlighted;
        public boolean message;
        public long price;
    }

    private Store() {}

    public static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static String getKeywords(Context c) {
        return prefs(c).getString(KEY_WORDS, "");
    }

    public static long getMaxPrice(Context c) {
        try {
            return Long.parseLong(prefs(c).getString(KEY_MAX_PRICE, "0").trim());
        } catch (Exception e) {
            return 0;
        }
    }

    public static void saveRules(Context c, String words, String maxPrice) {
        prefs(c).edit()
                .putString(KEY_WORDS, words == null ? "" : words.trim())
                .putString(KEY_MAX_PRICE, maxPrice == null ? "" : maxPrice.trim())
                .apply();
    }

    public static long extractPrice(String s) {
        if (s == null) return 0;
        Matcher m = Pattern.compile("(?i)(\\d[\\d\\s\\u00A0]{2,})\\s*(?:₽|руб)").matcher(s);
        if (m.find()) {
            try {
                return Long.parseLong(m.group(1).replaceAll("[^0-9]", ""));
            } catch (Exception ignored) {}
        }
        return 0;
    }

    public static boolean looksLikeMessage(String title, String text) {
        String s = ((title == null ? "" : title) + " " + (text == null ? "" : text))
                .toLowerCase(Locale.ROOT);
        return s.contains("сообщен") || s.contains("пишет") || s.contains("ответил")
                || s.contains("message") || s.contains("чат");
    }

    public static boolean shouldHighlight(Context c, String title, String text, long price, boolean message) {
        if (message) return true;

        String words = getKeywords(c).toLowerCase(Locale.ROOT).trim();
        long max = getMaxPrice(c);
        String body = ((title == null ? "" : title) + " " + (text == null ? "" : text))
                .toLowerCase(Locale.ROOT);

        boolean wordsOk = words.isEmpty();
        if (!words.isEmpty()) {
            for (String raw : words.split("[,;]")) {
                String w = raw.trim();
                if (!w.isEmpty() && body.contains(w)) {
                    wordsOk = true;
                    break;
                }
            }
        }

        boolean priceOk = max <= 0 || price <= 0 || price <= max;
        return wordsOk && priceOk;
    }

    public static synchronized void addEvent(Context c, Event e) {
        try {
            JSONArray arr = new JSONArray(prefs(c).getString(KEY_EVENTS, "[]"));
            JSONObject o = new JSONObject();
            o.put("time", e.time);
            o.put("title", e.title);
            o.put("text", e.text);
            o.put("pkg", e.pkg);
            o.put("highlighted", e.highlighted);
            o.put("message", e.message);
            o.put("price", e.price);

            JSONArray next = new JSONArray();
            next.put(o);
            for (int i = 0; i < arr.length() && i < 99; i++) next.put(arr.get(i));
            prefs(c).edit().putString(KEY_EVENTS, next.toString()).apply();
        } catch (Exception ignored) {}
    }

    public static List<Event> events(Context c) {
        ArrayList<Event> out = new ArrayList<>();
        try {
            JSONArray arr = new JSONArray(prefs(c).getString(KEY_EVENTS, "[]"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                Event e = new Event();
                e.time = o.optLong("time");
                e.title = o.optString("title");
                e.text = o.optString("text");
                e.pkg = o.optString("pkg");
                e.highlighted = o.optBoolean("highlighted");
                e.message = o.optBoolean("message");
                e.price = o.optLong("price");
                out.add(e);
            }
        } catch (Exception ignored) {}
        return out;
    }

    public static void clear(Context c) {
        prefs(c).edit().remove(KEY_EVENTS).apply();
    }
}
