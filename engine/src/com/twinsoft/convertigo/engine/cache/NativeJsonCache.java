/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine.cache;

import java.io.IOException;
import java.io.Writer;

/**
 * The cache of the JSON answers of native requests: their text, copied while it is written to the response, in
 * stream or not, and kept only once the answer is complete and without error. A larger answer is not kept.
 */
public final class NativeJsonCache {
	/** The largest answer kept, in characters. */
	public static final int MAX_CHARS = 8 * 1024 * 1024;

	private NativeJsonCache() {
	}

	/** An answer to keep under a key until a date, once it is written. */
	public record Pending(String key, long expiryDate) {
	}

	/** The cached text of a key, or null. */
	public static String get(String key) {
		return CacheManager.cachedNativeText(key);
	}

	/** A writer to the response that copies what it writes, up to MAX_CHARS, for the cache. */
	public static final class Tee extends Writer {
		private final Writer target;
		private final Pending pending;
		private StringBuilder copy = new StringBuilder(64 * 1024);

		public Tee(Writer target, Pending pending) {
			this.target = target;
			this.pending = pending;
		}

		private boolean room(int length) {
			if (copy != null && copy.length() + length > MAX_CHARS) copy = null;
			return copy != null;
		}

		@Override public void write(char[] buffer, int offset, int length) throws IOException {
			target.write(buffer, offset, length);
			if (room(length)) copy.append(buffer, offset, length);
		}

		@Override public void write(String text, int offset, int length) throws IOException {
			target.write(text, offset, length);
			if (room(length)) copy.append(text, offset, offset + length);
		}

		@Override public void write(int c) throws IOException {
			target.write(c);
			if (room(1)) copy.append((char) c);
		}

		@Override public Writer append(CharSequence text) throws IOException {
			target.append(text);
			if (room(text.length())) copy.append(text);
			return this;
		}

		@Override public void flush() throws IOException {
			target.flush();
		}

		@Override public void close() throws IOException {
			target.close();
		}

		/** Keeps the copy: the answer is written complete, without error. */
		public void store() {
			if (copy != null) CacheManager.storeNativeText(pending.key(), pending.expiryDate(), copy.toString());
			copy = null;
		}

		/** Drops the copy: the answer is incomplete, failed or too large. */
		public void discard() {
			copy = null;
		}
	}
}
