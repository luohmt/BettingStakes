package com.betting.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Lightweight JSON parser for simple flat JSON objects
 * Supports only string, integer, and boolean values
 * Example: {"key1":"value1","key2":123,"key3":true}
 */
public class LightJsonParser {
    private final Map<String, String> map = new HashMap<>();

    public LightJsonParser(String json) {
        if (json == null || json.isBlank()) return;
        json = json.trim();
        if (json.startsWith("{")) json = json.substring(1);
        if (json.endsWith("}")) json = json.substring(0, json.length() - 1);

        for (String pair : json.split(",")) {
            String[] kv = pair.split(":", 2);
            if (kv.length != 2) continue;
            String key = kv[0].trim().replaceAll("^\"|\"$", "");
            String value = kv[1].trim().replaceAll("^\"|\"$", "");
            map.put(key, value);
        }
    }

    public String getString(String key) {
        return map.get(key);
    }

    public Integer getInt(String key) {
        String v = map.get(key);
        return v != null ? Integer.parseInt(v) : null;
    }

    public Boolean getBoolean(String key) {
        String v = map.get(key);
        return v != null ? Boolean.parseBoolean(v) : null;
    }

}
