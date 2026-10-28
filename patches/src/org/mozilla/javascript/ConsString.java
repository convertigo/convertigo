/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.javascript;

import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayDeque;

/**
 * This class represents a string composed of two components, each of which may be a {@code
 * java.lang.String} or another ConsString.
 *
 * <p>This string representation is optimized for concatenation using the "+" operator. Instead of
 * immediately copying both components to a new character array, ConsString keeps references to the
 * original components and only converts them to a String if either toString() is called or a
 * certain depth level is reached.
 *
 * <p>Convertigo patch of Rhino 1.9.2: a ConsString shared between threads (a sealed shared scope, a
 * cache of a shared runtime) stays consistent. The flat string is published through a volatile field
 * before the components are dropped, and the components of another ConsString are only read under
 * its lock. Rhino (up to 1.9.2) reads {@code isFlat} without synchronization: another thread could
 * see it set before the flat string, and fail on a ClassCastException or copy a wrong text. As in
 * Rhino 1.9.2, a ConsString is serialized as a String and a stream claiming to hold one is refused.
 *
 * <p>Both the name and the concept are borrowed from V8.
 */
public class ConsString implements CharSequence, Serializable {

    private static final long serialVersionUID = -8432806714471372570L;

    // read and written under the lock of this ConsString, null once flat is set
    private CharSequence left, right;
    private final int length;
    private volatile String flat;

    public ConsString(CharSequence str1, CharSequence str2) {
        if (!(str1 instanceof String) && !(str1 instanceof ConsString)) {
            str1 = str1.toString();
        }
        if (!(str2 instanceof String) && !(str2 instanceof ConsString)) {
            str2 = str2.toString();
        }
        left = str1;
        right = str2;
        length = left.length() + right.length();
    }

    // Replace with string representation when serializing
    private Object writeReplace() {
        return this.toString();
    }

    private void readObject(ObjectInputStream stream) throws InvalidObjectException {
        throw new InvalidObjectException("ConsString must be serialized as a String");
    }

    private void readObjectNoData() throws InvalidObjectException {
        throw new InvalidObjectException("ConsString must be serialized as a String");
    }

    @Override
    public String toString() {
        String str = flat;
        return str != null ? str : flatten();
    }

    private synchronized String flatten() {
        String str = flat;
        if (str != null) {
            return str;
        }
        final char[] chars = new char[length];
        int charPos = length;

        ArrayDeque<CharSequence> stack = new ArrayDeque<>();
        stack.addFirst(left);

        CharSequence next = right;
        do {
            if (next instanceof ConsString) {
                ConsString casted = (ConsString) next;
                String castedFlat = casted.flat;
                if (castedFlat == null) {
                    CharSequence castedLeft, castedRight;
                    synchronized (casted) {
                        castedFlat = casted.flat;
                        castedLeft = casted.left;
                        castedRight = casted.right;
                    }
                    if (castedFlat == null) {
                        stack.addFirst(castedLeft);
                        next = castedRight;
                        continue;
                    }
                }
                next = castedFlat;
            }

            final String part = (String) next;
            charPos -= part.length();
            part.getChars(0, part.length(), chars, charPos);
            next = stack.isEmpty() ? null : stack.removeFirst();
        } while (next != null);

        str = new String(chars);
        flat = str;
        left = null;
        right = null;
        return str;
    }

    @Override
    public int length() {
        return length;
    }

    @Override
    public char charAt(int index) {
        return toString().charAt(index);
    }

    @Override
    public CharSequence subSequence(int start, int end) {
        return toString().substring(start, end);
    }
}
