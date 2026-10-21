package com.still.launcher;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/** Input limits also applied to user-selected backup files. */
public final class ConfigRules {
    public static final int MAX_BYTES = 65536, MAX_APPS = 300, MAX_GROUPS = 30;
    private ConfigRules() { }
    public static boolean packageId(String value) {
        return value != null && value.length() <= 200 && value.matches("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+");
    }
    public static String groupName(String value) {
        String clean = value == null ? "" : value.trim();
        if (clean.isEmpty() || clean.length() > 32 || clean.matches(".*[\\p{Cntrl}].*"))
            throw new IllegalArgumentException("Invalid collection name");
        return clean;
    }
    public static List<String> packages(List<String> values) {
        if (values.size() > MAX_APPS) throw new IllegalArgumentException("Too many apps");
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        for (String id : values) {
            if (!packageId(id)) throw new IllegalArgumentException("Invalid app ID");
            unique.add(id);
        }
        return new ArrayList<>(unique);
    }
    public static int range(int value, int min, int max) {
        if (value < min || value > max) throw new IllegalArgumentException("Setting out of range");
        return value;
    }
}
