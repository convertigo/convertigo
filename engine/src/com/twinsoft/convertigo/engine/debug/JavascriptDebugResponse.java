/* Copyright (c) 2001-2026 Convertigo SA. Licensed under the GNU AGPL, version 3 or later. */
package com.twinsoft.convertigo.engine.debug;

import org.codehaus.jettison.json.*;

/** A strict, cooperative codec for participant replies. No host objects or arbitrary getters. */
final class JavascriptDebugResponse {
    // Fixed protocol fields are work units (clock checks), not additional business-property candidates.
    private static final java.util.Set<String> FIELDS = java.util.Set.of("scopes", "scope", "this", "variables", "name", "value", "result", "type",
        "ref", "scopeRef", "thisRef", "expandable", "fullValue", "limited", "partial", "reason", "view", "cursor", "nextOffset", "evaluationError",
        "scopeNextOffset", "thisNextOffset", "scopeReason", "thisReason", "refused");
    interface Work { void check(boolean candidate); }
    static final class Limit extends RuntimeException {
        final String reason;
        Limit(String reason) { super("Inspection response refused."); this.reason = reason; }
    }
    /** JSONObject's enclosing serializer uses toString; do not reformat user data outside the worker quota. */
    static final class Prepared extends JSONObject {
        private final String body;
        private String wire;
        Prepared(JSONObject object, String body, Work work) throws JSONException {
            this.body = body;
            var keys = object.keys();
            while (keys.hasNext()) { work.check(false); String key = (String)keys.next(); work.check(!FIELDS.contains(key)); put(key, object.get(key)); work.check(false); }
        }
        void seal() throws JSONException {
            String trailer = "\"inspectionBudget\":" + getJSONObject("inspectionBudget").toString();
            wire = body.substring(0, body.length() - 1) + (length() > 1 ? "," : "") + trailer + "}";
        }
        @Override public String toString() { return wire == null ? body : wire; }
        @Override public String toString(int indent) { return toString(); }
    }
    /** Link the codec on session creation, before a request pauses or starts its inspection clock. */
    static void prepare() {
        var fixture = decode("{\"result\":\"\"}", candidate -> {});
        prepare(fixture, candidate -> {});
    }
    static JSONObject decode(String text, Work work) {
        try {
            var parser = new Parser(text, work);
            Object value = parser.value(0, true);
            parser.space();
            if (parser.at != text.length() || !(value instanceof JSONObject object)) throw new Limit("participant-response-shape");
            if (object.has("inspectionBudget")) throw new Limit("participant-response-shape");
            return object;
        } catch (JSONException | NumberFormatException e) { throw new Limit("participant-response-shape"); }
    }
    private static final class Parser {
        final String text; final Work work; int at;
        Parser(String text, Work work) { this.text = text; this.work = work; }
        char next() { work.check(false); if (at >= text.length()) throw new Limit("participant-response-shape"); return text.charAt(at++); }
        void space() { while (at < text.length() && Character.isWhitespace(text.charAt(at))) { work.check(false); at++; } }
        String string() {
            if (next() != '"') throw new Limit("participant-response-shape");
            var out = new StringBuilder();
            while (true) {
                char c = next();
                if (c == '"') return out.toString();
                if (c < 32) throw new Limit("participant-response-shape");
                if (c == '\\') {
                    c = next();
                    c = switch (c) {
                        case '"', '\\', '/' -> c;
                        case 'b' -> '\b'; case 'f' -> '\f'; case 'n' -> '\n'; case 'r' -> '\r'; case 't' -> '\t';
                        case 'u' -> { int code = 0; for (int i = 0; i < 4; i++) { int h = Character.digit(next(), 16); if (h < 0) throw new Limit("participant-response-shape"); code = code * 16 + h; } yield (char)code; }
                        default -> throw new Limit("participant-response-shape");
                    };
                }
                out.append(c); work.check(false);
            }
        }
        Object value(int depth, boolean candidate) throws JSONException {
            work.check(candidate); space();
            if (depth > 24 || at >= text.length()) throw new Limit("participant-response-shape");
            char c = text.charAt(at);
            if (c == '"') return string();
            if (c == '{') {
                at++; var result = new JSONObject(); space();
                if (at < text.length() && text.charAt(at) == '}') { at++; return result; }
                while (true) {
                    space(); work.check(false); String key = string(); work.check(!FIELDS.contains(key)); space();
                    if (next() != ':' || result.has(key)) throw new Limit("participant-response-shape");
                    result.put(key, value(depth + 1, false)); space(); c = next();
                    if (c == '}') return result;
                    if (c != ',') throw new Limit("participant-response-shape");
                }
            }
            if (c == '[') {
                at++; var result = new JSONArray(); space();
                if (at < text.length() && text.charAt(at) == ']') { at++; return result; }
                while (true) {
                    result.put(value(depth + 1, true)); space(); c = next();
                    if (c == ']') return result;
                    if (c != ',') throw new Limit("participant-response-shape");
                }
            }
            int start = at;
            while (at < text.length() && ",]} \t\r\n".indexOf(text.charAt(at)) < 0) { work.check(false); at++; }
            String token = text.substring(start, at);
            if (token.equals("null")) return JSONObject.NULL;
            if (token.equals("true")) return true;
            if (token.equals("false")) return false;
            if (token.length() > 64 || !token.matches("-?(0|[1-9][0-9]*)(\\.[0-9]+)?([eE][+-]?[0-9]+)?")) throw new Limit("participant-response-shape");
            if (token.indexOf('.') < 0 && token.indexOf('e') < 0 && token.indexOf('E') < 0) {
                try { long integer = Long.parseLong(token); work.check(false); return integer; } catch (NumberFormatException e) { throw new Limit("participant-number-range"); }
            }
            double number = Double.parseDouble(token);
            if (!Double.isFinite(number)) throw new Limit("participant-response-shape");
            work.check(false); return number;
        }
    }
    static JSONObject prepare(JSONObject value, Work work) {
        var wire = new StringBuilder();
        try {
            var result = (JSONObject)copy(value, "", false, false, 0, true, work, wire);
            work.check(false);
            var prepared = new Prepared(result, wire.toString(), work);
            work.check(false); return prepared;
        } catch (JSONException e) { throw new Limit("participant-response-shape"); }
    }
    private static void quote(String text, Work work, StringBuilder wire) {
        wire.append('"');
        for (int i = 0; i < text.length(); i++) {
            work.check(false); char c = text.charAt(i);
            if (c == '"' || c == '\\') wire.append('\\').append(c);
            else if (c < 32) { wire.append("\\u00").append(Character.forDigit(c / 16, 16)).append(Character.forDigit(c % 16, 16)); }
            else wire.append(c);
            if (wire.length() > 262144) throw new Limit("response-size");
            work.check(false);
        }
        wire.append('"');
    }
    private static Object copy(Object value, String field, boolean dynamicKeys, boolean business, int depth, boolean candidate, Work work, StringBuilder wire) throws JSONException {
        work.check(candidate);
        if (business && JavascriptDebugRedaction.sensitive(field)) { quote("[redacted]", work, wire); return "[redacted]"; }
        if (depth > 24) throw new Limit("response-depth");
        if (value instanceof JSONObject object && object.getClass() == JSONObject.class) {
            var result = new JSONObject(); var keys = object.keys(); wire.append('{'); boolean first = true;
            boolean addresses = !business && JavascriptDebugRedaction.addressKeys(field);
            boolean sensitiveVariable = !business && object.opt("name") instanceof String variableName && JavascriptDebugRedaction.sensitive(variableName) && object.has("value");
            while (keys.hasNext()) {
                work.check(false); String key = (String)keys.next(); work.check(business || dynamicKeys || !FIELDS.contains(key));
                String name = business || dynamicKeys ? JavascriptDebugRedaction.text(key, () -> work.check(false)) : key;
                work.check(false); if (!first) wire.append(','); first = false; quote(name, work, wire); wire.append(':');
                Object child = sensitiveVariable ? switch (key) {
                    case "value" -> "[redacted]";
                    case "ref" -> "";
                    case "expandable", "fullValue" -> false;
                    default -> object.get(key);
                } : object.get(key);
                result.put(name, copy(child, key, !dynamicKeys && !addresses && JavascriptDebugRedaction.dataKeys(key), business || !dynamicKeys && !addresses && JavascriptDebugRedaction.rawData(key), depth + 1, false, work, wire)); work.check(false);
            }
            wire.append('}'); return result;
        }
        if (value instanceof JSONArray array && array.getClass() == JSONArray.class) {
            var result = new JSONArray(); wire.append('[');
            for (int i = 0; i < array.length(); i++) { work.check(false); if (i > 0) wire.append(','); result.put(copy(array.get(i), field, false, business, depth + 1, true, work, wire)); work.check(false); }
            wire.append(']'); return result;
        }
        if (value instanceof String string) {
            if (string.length() > 65536) throw new Limit("response-string-size");
            String masked = !business && JavascriptDebugRedaction.handle(field) ? string : JavascriptDebugRedaction.text(string, () -> work.check(false));
            work.check(false); quote(masked, work, wire); return masked;
        }
        if (value == null || value == JSONObject.NULL || value instanceof Boolean
            || value instanceof Integer || value instanceof Long || value instanceof Double || value instanceof Float) {
            String text = value instanceof Double || value instanceof Float ? org.mozilla.javascript.ScriptRuntime.numberToString(((Number)value).doubleValue(), 10) : String.valueOf(value);
            if (business && value != null && value != JSONObject.NULL) {
                String masked = JavascriptDebugRedaction.text(text, () -> work.check(false));
                if (!masked.equals(text)) { quote(masked, work, wire); return masked; }
            }
            if (text.equals("NaN") || text.contains("Infinity")) throw new Limit("response-shape");
            wire.append(text); work.check(false); return value;
        }
        throw new Limit("response-shape");
    }
    private JavascriptDebugResponse() { }
}
