/* Copyright (c) 2001-2026 Convertigo SA. Licensed under the GNU AGPL, version 3 or later. */
package com.twinsoft.convertigo.engine.debug;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Set;
import org.mozilla.javascript.*;

/** Incremental, read-only adapters for the bundled Rhino 1.9.2 layout. Fail closed on another layout.
 * No generic host enumeration, lock, getter, size(), or borrowed business iterator is used.
 * Slot traversal is a non-atomic view; promotion or concurrent structural mutation invalidates it.
 */
final class JavascriptDebugProperties {
    record Item(String name, Object value, boolean accessor, boolean visible) { }
    interface Cursor { Item next() throws Exception; }
    private static final Set<String> slotMaps = Set.of("org.mozilla.javascript.EmbeddedSlotMap", "org.mozilla.javascript.HashSlotMap",
        "org.mozilla.javascript.SlotMapOwner$EmptySlotMap", "org.mozilla.javascript.SlotMapOwner$SingleEntrySlotMap");
    private static boolean qualifiedMap(Object value) { return slotMaps.contains(value.getClass().getName()); }
    private static final Field map = field("org.mozilla.javascript.SlotMapOwner", "slotMap");
    private static final Field slotName = field("org.mozilla.javascript.Slot", "name");
    private static final Field slotIndex = field("org.mozilla.javascript.Slot", "indexOrHash");
    private static final Field slotValue = field("org.mozilla.javascript.Slot", "value");
    private static final Field slotAttributes = field("org.mozilla.javascript.Slot", "attributes");
    private static final Field debugFrame = field("org.mozilla.javascript.Interpreter$DebugScope", "frame");
    private static final Field function = field("org.mozilla.javascript.Interpreter$CallFrame", "fnOrScript");
    private static final Method variable = method("org.mozilla.javascript.Interpreter$CallFrame", "getFromVars", int.class);
    private static Field field(String owner, String name) {
        try { var f = Class.forName(owner).getDeclaredField(name); f.setAccessible(true); return f; }
        catch (ReflectiveOperationException | RuntimeException e) { return null; }
    }
    private static Method method(String owner, String name, Class<?>... args) {
        try { var m = Class.forName(owner).getDeclaredMethod(name, args); m.setAccessible(true); return m; }
        catch (ReflectiveOperationException | RuntimeException e) { return null; }
    }
    static boolean supported(Object value) {
        if (value == null) return false;
        return objectArray(value) || value.getClass() == NativeArray.class
            || map != null && (value.getClass() == NativeObject.class || value.getClass() == NativeCall.class)
            || debugFrame != null && variable != null && value.getClass().getName().equals("org.mozilla.javascript.Interpreter$DebugScope");
    }
    static Cursor open(Object object) throws Exception {
        if (objectArray(object)) {
            int length = java.lang.reflect.Array.getLength(object);
            return new Cursor() { int index; public Item next() {
                return index >= length ? null : new Item(Integer.toString(index), java.lang.reflect.Array.get(object, index++), false, true);
            }};
        }
        if (object != null && object.getClass() == NativeArray.class) {
            var array = (NativeArray)object;
            long length = Math.min(array.getLength(), Integer.MAX_VALUE);
            return new Cursor() { int index; public Item next() throws Exception {
                if (index >= length) return null;
                int at = index++;
                Object value = index(array, at);
                return new Item(Integer.toString(at), value, "<accessor>".equals(value), true);
            }};
        }
        if (!supported(object)) return null;
        if (object instanceof ScriptableObject) {
            Object raw = map.get(object);
            if (!qualifiedMap(raw)) return null;
            // Only these bundled implementations have O(1) linked-list iteration. Never close/remove a business iterator.
            Iterator<Slot> iterator = ((SlotMap)raw).iterator();
            return () -> {
                if (map.get(object) != raw) throw new java.util.ConcurrentModificationException();
                if (!iterator.hasNext()) return null;
                Slot slot = iterator.next();
                Object key = slotName.get(slot);
                boolean visible = (slotAttributes.getShort(slot) & ScriptableObject.DONTENUM) == 0 && !(key instanceof Symbol);
                String label = key == null ? Integer.toString(slotIndex.getInt(slot)) : key instanceof String s ? s : "<symbol>";
                // Every derived slot is conservatively opaque: lazy properties and accessors can execute code.
                boolean accessor = slot.getClass() != Slot.class;
                return new Item(label, accessor ? null : slotValue.get(slot), accessor, visible);
            };
        }
        Object frame = debugFrame.get(object);
        var descriptor = ((ScriptOrFn<?>)function.get(frame)).getDescriptor();
        int count = descriptor.getParamAndVarCount();
        return new Cursor() { int index; public Item next() throws Exception {
            if (index >= count) return null;
            int at = index++;
            return new Item(descriptor.getParamOrVarName(at), variable.invoke(frame, at), false, true);
        }};
    }
    static boolean objectArray(Object value) { return value != null && value.getClass().isArray() && !value.getClass().getComponentType().isPrimitive(); }
    private static Object unwrap(Object value) {
        return value instanceof NativeJavaObject host && JavascriptDebugSession.trustedWrapper(value) ? host.unwrap() : value;
    }
    private static final Field dense = field("org.mozilla.javascript.NativeArray", "dense");
    /** One captured slot, never a descriptor check followed by a dynamic property read. */
    static Object index(Object object, int index) throws Exception {
        object = unwrap(object);
        if (objectArray(object)) return index < java.lang.reflect.Array.getLength(object) ? java.lang.reflect.Array.get(object,index) : Undefined.instance;
        if (object == null || object.getClass() != NativeArray.class) return "<opaque>";
        var array = (NativeArray)object;
        Object raw = map.get(array);
        if (raw == null || !qualifiedMap(raw)) return "<opaque>";
        Slot slot = ((SlotMap)raw).query(null,index);
        if (slot != null) return slot.getClass() == Slot.class ? slotValue.get(slot) : "<accessor>";
        Object[] storage = (Object[])dense.get(array);
        if (storage == null || index >= storage.length) return "<opaque>";
        Object value = storage[index];
        return value == Scriptable.NOT_FOUND ? "<opaque>" : value;
    }
    static Object read(Object object, String key) throws Exception {
        if (JavascriptDebugRedaction.sensitive(key)) return "[redacted]";
        object = unwrap(object);
        if (object == null) return Undefined.instance;
        if (object.getClass() != NativeObject.class && object.getClass() != NativeCall.class) return "<opaque>";
        Object raw = map.get(object);
        if (!qualifiedMap(raw)) return "<opaque>";
        Slot slot = ((SlotMap)raw).query(key, 0);
        return slot == null ? Undefined.instance : slot.getClass() == Slot.class ? slotValue.get(slot) : "<accessor>";
    }
    static Object binding(Object scope, String key, Runnable check) throws Exception {
        check.run();
        if (scope != null && scope.getClass().getName().equals("org.mozilla.javascript.Interpreter$DebugScope")) {
            var cursor = open(scope); Item item;
            while ((item = cursor.next()) != null) { check.run(); if (item.name().equals(key)) return item.accessor() ? "<accessor>" : item.value(); }
            return Undefined.instance;
        }
        Object value = read(scope, key); check.run(); return value;
    }
    static void prepare() { }
    private JavascriptDebugProperties() { }
}
