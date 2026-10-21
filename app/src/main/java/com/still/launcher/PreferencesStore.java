package com.still.launcher;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class PreferencesStore {
    final SharedPreferences raw;
    PreferencesStore(Context context) { this(context, "still"); }
    PreferencesStore(Context context, String name) { raw = context.getSharedPreferences(name, Context.MODE_PRIVATE); }
    List<String> favourites() {
        String value = raw.getString("favourites", "");
        return value.isEmpty() ? new ArrayList<>() : new ArrayList<>(Arrays.asList(value.split("\\|")));
    }
    void favourites(List<String> ids) { raw.edit().putString("favourites", TextUtils.join("|", ids)).apply(); }
    Set<String> hidden() { return new HashSet<>(raw.getStringSet("hidden", Collections.emptySet())); }
    void hide(String id) { Set<String> ids = hidden(); ids.add(id); raw.edit().putStringSet("hidden", ids).apply(); }
    void restoreHidden() { raw.edit().remove("hidden").apply(); }
    int scene() { return Math.max(0, Math.min(2, raw.getInt("scene", 0))); }
    int density() { return Math.max(0, Math.min(2, raw.getInt("density", 1))); }
    int textSize() { return Math.max(0, Math.min(2, raw.getInt("textSize", 1))); }
    int columns() { return new int[]{4, 5, 6}[density()]; }
    boolean contrast() { return raw.getBoolean("contrast", false); }
    void setting(String key, int value) { raw.edit().putInt(key, value).apply(); }
    List<String> recent() {
        String value = raw.getString("recent", "");
        return value.isEmpty() ? new ArrayList<>() : new ArrayList<>(Arrays.asList(value.split("\\|")));
    }
    void opened(String id) {
        List<String> ids = recent(); ids.remove(id); ids.add(0, id);
        while (ids.size() > 12) ids.remove(ids.size() - 1);
        raw.edit().putString("recent", TextUtils.join("|", ids)).apply();
    }
    Map<String, List<String>> groups() {
        Map<String, List<String>> groups = new LinkedHashMap<>();
        try {
            JSONObject object = new JSONObject(raw.getString("groups", "{}"));
            JSONArray names = object.names();
            if (names != null) for (int i = 0; i < names.length(); i++) {
                String name = names.getString(i);
                groups.put(name, readArray(object.getJSONArray(name)));
            }
        } catch (JSONException invalid) { /* Imported files are validated before storage. */ }
        return groups;
    }
    void setGroup(String name, List<String> ids) {
        Map<String, List<String>> groups = groups();
        name = ConfigRules.groupName(name);
        if (!groups.containsKey(name) && groups.size() >= ConfigRules.MAX_GROUPS) throw new IllegalArgumentException();
        if (ids.isEmpty()) groups.remove(name); else groups.put(name, ConfigRules.packages(ids));
        raw.edit().putString("groups", groupJson(groups).toString()).apply();
    }
    private static JSONObject groupJson(Map<String, List<String>> groups) {
        JSONObject object = new JSONObject();
        for (Map.Entry<String, List<String>> group : groups.entrySet()) {
            try { object.put(group.getKey(), new JSONArray(group.getValue())); }
            catch (JSONException impossible) { throw new IllegalArgumentException(impossible); }
        }
        return object;
    }
    private static List<String> readArray(JSONArray array) throws JSONException {
        if (array.length() > ConfigRules.MAX_APPS) throw new IllegalArgumentException();
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < array.length(); i++) ids.add(array.getString(i));
        return ConfigRules.packages(ids);
    }
    String exportConfig() throws JSONException {
        return new JSONObject().put("schema", 2).put("favourites", new JSONArray(favourites()))
                .put("hidden", new JSONArray(hidden())).put("groups", groupJson(groups()))
                .put("scene", scene()).put("density", density()).put("textSize", textSize())
                .put("contrast", contrast()).toString(2);
    }
    void importConfig(String text) throws JSONException {
        if (text.length() > ConfigRules.MAX_BYTES) throw new IllegalArgumentException();
        JSONObject value = new JSONObject(text);
        if (value.getInt("schema") != 2) throw new IllegalArgumentException();
        List<String> favourites = readArray(value.getJSONArray("favourites"));
        Set<String> hidden = new HashSet<>(readArray(value.getJSONArray("hidden")));
        JSONObject sourceGroups = value.getJSONObject("groups");
        if (sourceGroups.length() > ConfigRules.MAX_GROUPS) throw new IllegalArgumentException();
        Map<String, List<String>> groups = new LinkedHashMap<>();
        JSONArray names = sourceGroups.names();
        if (names != null) for (int i = 0; i < names.length(); i++) {
            String name = names.getString(i); String clean = ConfigRules.groupName(name);
            if (groups.containsKey(clean)) throw new IllegalArgumentException();
            groups.put(clean, readArray(sourceGroups.getJSONArray(name)));
        }
        int scene = ConfigRules.range(value.getInt("scene"), 0, 2);
        int density = ConfigRules.range(value.getInt("density"), 0, 2);
        int textSize = ConfigRules.range(value.getInt("textSize"), 0, 2);
        boolean contrast = value.getBoolean("contrast");
        // One transaction, only after every imported field has passed validation.
        raw.edit().putString("favourites", TextUtils.join("|", favourites)).putStringSet("hidden", hidden)
                .putString("groups", groupJson(groups).toString()).putInt("scene", scene)
                .putInt("density", density).putInt("textSize", textSize).putBoolean("contrast", contrast)
                .putBoolean("onboarded", true).apply();
    }
}
