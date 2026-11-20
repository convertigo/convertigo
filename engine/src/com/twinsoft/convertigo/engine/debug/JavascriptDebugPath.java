/* Copyright (c) 2001-2026 Convertigo SA. Licensed under the GNU AGPL, version 3 or later. */
package com.twinsoft.convertigo.engine.debug;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
/** Parsed scope paths. No source text crosses the participant boundary. */
public final class JavascriptDebugPath {
    static final String REFUSAL = "Evaluation requires a bounded scope path.";
    private static final Set<String> ROOTS = Set.of("input", "arguments", "output", "local", "result");
    private final String root;
    private final List<Object> segments;
    private JavascriptDebugPath(String root, List<Object> segments) { this.root = root; this.segments = List.copyOf(segments); }
    public String root() { return root; }
    static JavascriptDebugPath parse(String source) {
        if (source == null || source.isEmpty() || source.length() > 4096) throw refused();
        int at = 0;
        while (at < source.length() && identifier(source.charAt(at), at == 0)) at++;
        String root = source.substring(0, at);
        if (!ROOTS.contains(root)) throw refused();
        var segments = new ArrayList<Object>();
        while (at < source.length()) {
            if (segments.size() >= 31) throw refused();
            char separator = source.charAt(at++); int start = at;
            if (separator == '.') {
                while (at < source.length() && identifier(source.charAt(at), at == start)) at++;
                if (at == start || at - start > 128) throw refused();
                segments.add(source.substring(start, at));
            } else if (separator == '[') {
                while (at < source.length() && source.charAt(at) >= '0' && source.charAt(at) <= '9') at++;
                if (at == start || at - start > 9 || at - start > 1 && source.charAt(start) == '0'
                    || at == source.length() || source.charAt(at++) != ']') throw refused();
                segments.add(Integer.valueOf(source.substring(start, at - 1)));
            } else throw refused();
        }
        return new JavascriptDebugPath(root, segments);
    }
    private static boolean identifier(char c, boolean first) {
        return c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z' || c == '_' || c == '$' || !first && c >= '0' && c <= '9';
    }
    static IllegalArgumentException refused() { return new IllegalArgumentException(REFUSAL); }
    Object resolve(Object value, Runnable check) throws Exception {
        for (Object segment : segments) {
            check.run();
            if (segment instanceof String name && JavascriptDebugRedaction.sensitive(name)) return "[redacted]";
            value = segment instanceof Integer index ? JavascriptDebugProperties.index(value, index) : JavascriptDebugProperties.read(value, (String)segment);
            check.run();
        }
        return value;
    }
}
