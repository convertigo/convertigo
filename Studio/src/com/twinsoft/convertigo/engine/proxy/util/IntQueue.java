/*
 * Copyright (c) 2001-2011 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 *
 * $URL$
 * $Author$
 * $Revision$
 * $Date$
 */

package com.twinsoft.convertigo.engine.proxy.util;

public class IntQueue {
	private int[] data;
	private int incCapacity;
	
	// qSize = qEnd - qStart + 1 except when qSize = 0
	private int qStart, qEnd, qSize;

	public IntQueue(int initCapacity, int capacityIncrement) {
		data = new int[initCapacity];
		incCapacity = capacityIncrement;
	}

	public IntQueue() {
		this(16, 16);
	}

	public void ensureCapacity(int minCapacity) {
		if (minCapacity > data.length)
			setCapacity(Math.max(data.length + incCapacity, minCapacity));
	}

	public void setCapacity(int newCapacity) {
		if (newCapacity == data.length)
			return;

		if (newCapacity < qSize)
			throw new IndexOutOfBoundsException(
				"New capacity: "
					+ newCapacity
					+ " is less than queue size: "
					+ qSize);

		int[] newData = new int[newCapacity];

		if (qSize > 0) {
			if (qEnd >= qStart) {
				System.arraycopy(data, qStart, newData, 0, qSize);
			}
			else {
				System.arraycopy(
					data,
					qStart,
					newData,
					0,
					data.length - qStart);
				System.arraycopy(
					data,
					0,
					newData,
					data.length - qStart,
					qEnd + 1);
			}
			qStart = 0;
			qEnd = qSize - 1;
		}

		data = newData;
	}

	public int capacity() {
		return data.length;
	}

	public void clear() {
		qStart = qEnd = qSize = 0;
	}

	public int size() {
		return qSize;
	}

	public void push(int value) {
		ensureCapacity(qSize + 1);

		if (qSize == 0) {
			qSize = 1;
			qStart = qEnd = 0;
			data[0] = value;
		}
		else {
			qSize++;
			qStart = qStart > 0 ? qStart - 1 : data.length - 1;
			data[qStart] = value;
		}
	}

	public void push(String str) {
		int strLen = str.length();
		int strPos = strLen;

		ensureCapacity(qSize + strLen);

		int len = Math.min(qStart, strLen);
		for (int k = 0; k < len; k++)
			data[--qStart] = (int) str.charAt(--strPos);

		if (len < strLen) {
			len = strLen - len;

			qStart = data.length;
			for (int k = 0; k < len; k++)
				data[--qStart] = (int) str.charAt(--strPos);
		}

		qSize += strLen;
	}

	public void push(StringBuffer str) {
		int strLen = str.length();
		int strPos = strLen;

		ensureCapacity(qSize + strLen);

		int len = Math.min(qStart, strLen);
		for (int k = 0; k < len; k++)
			data[--qStart] = (int) str.charAt(--strPos);

		if (len < strLen) {
			len = strLen - len;

			qStart = data.length;
			for (int k = 0; k < len; k++)
				data[--qStart] = (int) str.charAt(--strPos);
		}

		qSize += strLen;
	}

	public void append(int value) {
		ensureCapacity(qSize + 1);

		if (qSize == 0) {
			qSize = 1;
			qStart = qEnd = 0;
			data[0] = value;
		}
		else {
			qSize++;
			qEnd = qEnd < data.length - 1 ? qEnd + 1 : 0;
			data[qEnd] = value;
		}
	}

	public void append(String str) {
		int strLen = str.length();
		int strPos = 0;

		ensureCapacity(qSize + strLen);

		int len =
			qEnd >= qStart
				? Math.min(strLen, data.length - qEnd - 1)
				: Math.min(strLen, qStart - qEnd - 1);
		for (int k = 0; k < len; k++)
			data[++qEnd] = (int) str.charAt(strPos++);

		if (len < strLen) {
			len = strLen - len;

			qEnd = -1;
			for (int k = 0; k < len; k++)
				data[++qStart] = (int) str.charAt(--strPos);
		}

		qSize += strLen;
	}

	public void append(StringBuffer str) {
		int strLen = str.length();
		int strPos = 0;

		ensureCapacity(qSize + strLen);

		int len =
			qEnd >= qStart
				? Math.min(strLen, data.length - qEnd - 1)
				: Math.min(strLen, qStart - qEnd - 1);
		for (int k = 0; k < len; k++)
			data[++qEnd] = (int) str.charAt(strPos++);

		if (len < strLen) {
			len = strLen - len;

			qEnd = -1;
			for (int k = 0; k < len; k++)
				data[++qStart] = (int) str.charAt(--strPos);
		}

		qSize += strLen;
	}

	public int pop() {
		if (qSize == 0)
			throw new IndexOutOfBoundsException("Empty queue");

		qSize--;

		int c = data[qStart++];
		if (qStart >= data.length)
			qStart -= data.length;

		return c;
	}

	public void pop(int len) {
		if (len < 0 || len > qSize)
			throw new IllegalArgumentException(
				"pop(" + len + ") while size is " + qSize);

		qSize -= len;
		qStart += len;
		if (qStart >= data.length)
			qStart -= data.length;
	}

	public int popLast() {
		if (qSize == 0)
			throw new IndexOutOfBoundsException("Empty queue");

		qSize--;

		int c = data[qEnd--];
		if (qEnd < 0)
			qEnd = data.length - 1;

		return c;
	}

	public void popLast(int len) {
		if (len < 0 || len > qSize)
			throw new IllegalArgumentException(
				"popLast(" + len + ") while size is " + qSize);

		qSize -= len;
		qEnd -= len;
		if (qEnd < 0)
			qEnd += data.length;
	}

	public int get(int index) {
		if (index < 0 || index >= qSize)
			throw new IndexOutOfBoundsException(
				"Index: " + index + ", size: " + qSize);

		int pos = qStart + index;
		if (pos >= data.length)
			pos -= data.length;

		return data[pos];
	}

	public int get() {
		if (qSize == 0)
			throw new IndexOutOfBoundsException("Index: 0, size: " + qSize);

		return data[qStart];
	}

	public int getLast() {
		if (qSize == 0)
			throw new IndexOutOfBoundsException("Index: 0, size: " + qSize);

		return data[qEnd];
	}

	public void set(int index, int value) {
		if (index < 0 || index >= qSize)
			throw new IndexOutOfBoundsException(
				"Index: " + index + ", size: " + qSize);

		int pos = qStart + index;
		if (pos >= data.length)
			pos -= data.length;

		data[pos] = value;
	}

	public void set(int value) {
		if (qSize == 0)
			throw new IndexOutOfBoundsException("Index: 0, size: " + qSize);

		data[qStart] = value;
	}

	public void setLast(int value) {
		if (qSize == 0)
			throw new IndexOutOfBoundsException("Index: 0, size: " + qSize);

		data[qEnd] = value;
	}

	public char getChar() {
		return (char) get();
	}

	public char getChar(int index) {
		return (char) get(index);
	}

	public char getLastChar() {
		return (char) getLast();
	}

	static private final int caseDiff = 'a' - 'A';

	public char getUpperCase(int index) {
		char ch = (char) get(index);
		return (ch >= 'a' && ch <= 'z') ? (char) (ch - caseDiff) : ch;
	}

	public char getLowerCase(int index) {
		char ch = (char) get(index);
		return (ch >= 'A' && ch <= 'Z') ? (char) (ch + caseDiff) : ch;
	}

	public String getString(int len) {
		return getString(0, len);
	}

	public String getString(int pos, int len) {
		if (len > size() - pos)
			len = size() - pos;

		StringBuffer sbuf = new StringBuffer(len);

		for (int k = pos, kend = pos + len; k < kend; k++)
			sbuf.append((char) get(k));

		return sbuf.toString();
	}

	public int indexOf(String str) {
		return indexOf(str, 0, size());
	}

	public int indexOf(String str, int pos) {
		return indexOf(str, pos, size() - pos);
	}

	public int indexOf(String str, int pos, int len) {
		int foundIndex = -1;

		if (pos + len > size())
			len = size() - pos;

		if (len < str.length())
			return foundIndex;

		char firstChar = str.charAt(0);

		for (int k = pos, kEnd = k + len - str.length() + 1; k < kEnd; k++) {
			if (firstChar == getLowerCase(k)) {
				foundIndex = k;

				for (int h = 1, hEnd = str.length() - 1; h < hEnd; h++) {
					if (str.charAt(h) != getLowerCase(k + h)) {
						foundIndex = -1;
						break;
					}
				}

				if (foundIndex > -1)
					break;
			}
		}

		return foundIndex;
	}

	public int compareString(String str) {
		return compareString(str, 0, str.length());
	}

	public int compareString(String str, int pos) {
		return compareString(str, pos, str.length());
	}

	public int compareString(String str, int pos, int len) {
		int sLen = str.length();
		int qLen = size() - pos;
		int minLen = Math.min(len, Math.min(sLen, qLen));

		int diff = 0;
		for (int k = 0, h = pos; diff == 0 && k < minLen; k++, h++)
			diff = str.charAt(k) - getLowerCase(h);

		if (diff == 0) {
			if (minLen == len || sLen == qLen)
				return 0;
			else if (sLen > qLen)
				return 1;
			else
				return -1;
		}

		return diff > 0 ? 1 : -1;
	}
}