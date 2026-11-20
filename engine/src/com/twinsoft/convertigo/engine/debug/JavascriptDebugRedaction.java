/* Copyright (c) 2001-2026 Convertigo SA. Licensed under the GNU AGPL, version 3 or later. */
package com.twinsoft.convertigo.engine.debug;

import java.util.List;
import java.util.ArrayList;
import java.util.function.Supplier;
import com.twinsoft.convertigo.engine.Engine;
import org.codehaus.jettison.json.*;

/** Output boundary, independent of the name under which a secret was copied by business code.
 * This is masking, not data-flow taint tracking: arbitrary transformations performed by evaluation
 * are not made safe by string replacement. No secret is logged or included in an exception.
 */
final class JavascriptDebugRedaction {
    private record Catalog(Object manager, long generation, List<String> values, boolean limited) { }
    private static volatile Catalog catalog = new Catalog(null, -1, List.of(), false);
    static Supplier<List<String>> testValues;
    private static final java.util.regex.Pattern SENSITIVE = java.util.regex.Pattern.compile("(?i)(password|passwd|secret|token|authorization|cookie|api[._-]?key|private[._-]?key|credential)");
    static boolean sensitive(String key) { return key != null && (key.length() > 65536 || SENSITIVE.matcher(key).find()); }
    private static Catalog catalog() {
        var manager = Engine.theApp == null ? null : Engine.theApp.databaseObjectsManager;
        if (manager == null || !Engine.isStarted) return new Catalog(null, -1, List.of(), false);
        long generation = manager.getSymbolsGeneration();
        var current = catalog;
        if (current.manager == manager && current.generation == generation) return current;
        var values = new ArrayList<String>();
        try {
            int count = 0;
            for (String name : manager.symbolsGetNames()) {
                if (++count > 1000) return new Catalog(manager, generation, List.of(), true);
                if (sensitive(name)) {
                    String value = manager.symbolsGetValue(name);
                    if (value != null && !value.isEmpty()) values.add(value);
                }
            }
            values.sort(java.util.Comparator.comparingInt(String::length).reversed());
            current = new Catalog(manager, generation, List.copyOf(values), false);
            catalog = current;
            return current;
        } catch (RuntimeException e) { return new Catalog(manager, generation, List.of(), true); }
    }
    static String text(String text) {
        var current = catalog();
        if (current.limited) return "[redacted: secret catalog unavailable]";
        for (var value : testValues == null ? current.values : testValues.get())
            if (!value.isEmpty()) text = text.replace(value, "[redacted]");
        return text;
    }
    /** Cooperative literal replacement: no unbounded regex or whole-response traversal. */
    static String text(String text, Runnable check) {
        check.run(); var current = catalog(); check.run();
        if (current.limited) return "[redacted: secret catalog unavailable]";
        for (var value : testValues == null ? current.values : testValues.get()) {
            check.run(); if (value.isEmpty()) continue;
            var out = new StringBuilder();
            for (int at = 0; at < text.length();) {
                check.run(); boolean match = at + value.length() <= text.length();
                for (int i = 0; match && i < value.length(); i++) { check.run(); if (text.charAt(at + i) != value.charAt(i)) match = false; }
                if (match) { out.append("[redacted]"); at += value.length(); } else out.append(text.charAt(at++));
                if (out.length() > 65536) throw new JavascriptDebugResponse.Limit("response-string-size");
                check.run();
            }
            text = out.toString(); check.run();
        }
        return text;
    }
    static JSONObject output(JSONObject value) {
        return (JSONObject)copy(value, false, "", false);
    }
    // These are transport handles, never display strings supplied by business code.
    static boolean handle(String key) {
        return java.util.Set.of("sessionId", "workerId", "frameId", "activation", "parentActivation", "rootRequestId",
            "invocationId", "id", "parent", "rootRequest", "worker", "invocation", "ref", "scopeRef", "thisRef", "cursor").contains(key);
    }
    static boolean dataKeys(String key) { return java.util.Set.of("value", "result", "data", "bindings").contains(key); }
    static boolean addressKeys(String field) { return java.util.Set.of("breakpoints", "sourceLocations").contains(field); }
    static boolean rawData(String field) { return java.util.Set.of("value", "result", "data", "bindings").contains(field); }
    private static Object copy(Object value, boolean dynamicKeys, String field, boolean business) {
        try {
            if (value instanceof JSONObject object) {
                var result = new JSONObject(); var keys = object.keys();
                boolean addresses = !business && addressKeys(field);
                boolean sensitiveVariable = !business && object.opt("name") instanceof String name && sensitive(name) && object.has("value");
                while (keys.hasNext()) {
                    String key = String.valueOf(keys.next());
                    Object child = sensitiveVariable ? switch (key) {
                        case "value" -> "[redacted]";
                        case "ref" -> "";
                        case "expandable", "fullValue" -> false;
                        default -> object.get(key);
                    } : object.get(key);
                    result.put(business || dynamicKeys ? text(key) : key, business && sensitive(key) ? "[redacted]" : copy(child, !dynamicKeys && !addresses && dataKeys(key), key, business || !dynamicKeys && !addresses && rawData(key)));
                }
                return result;
            }
            if (value instanceof JSONArray array) {
                var result = new JSONArray();
                for (int i = 0; i < array.length(); i++) result.put(copy(array.get(i), false, field, business));
                return result;
            }
            if (value instanceof String string) return business || !handle(field) ? text(string) : string;
            if (business && (value instanceof Boolean || value instanceof Integer || value instanceof Long || value instanceof Double || value instanceof Float)) {
                String plain = value instanceof Double || value instanceof Float ? org.mozilla.javascript.ScriptRuntime.numberToString(((Number)value).doubleValue(), 10) : String.valueOf(value);
                String masked = text(plain); if (!plain.equals(masked)) return masked;
            }
            return value;
        } catch (JSONException e) { throw new IllegalStateException("Debugger output unavailable."); }
    }
    private JavascriptDebugRedaction() { }
}
