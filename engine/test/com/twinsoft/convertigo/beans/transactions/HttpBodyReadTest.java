/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */
package com.twinsoft.convertigo.beans.transactions;

import static org.junit.Assert.assertArrayEquals;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

import org.junit.Test;

/**
 * AbstractHttpTransaction.readResult reads the body into one buffer of its Content-Length, capped: a server announcing a
 * length far bigger than what it sends no longer gets a buffer of that length (the task runs with a small heap, so the
 * old buffer of 1.5 GB fails with OutOfMemoryError). The body read is the same whatever the length announced.
 */
public class HttpBodyReadTest {

	private static byte[] body(int length) {
		byte[] body = new byte[length];
		new Random(length).nextBytes(body);
		return body;
	}

	/** a stream giving a few bytes per read, like a network */
	private static InputStream slow(byte[] body) {
		return new FilterInputStream(new ByteArrayInputStream(body)) {
			@Override
			public int read(byte[] b, int off, int len) throws IOException {
				return super.read(b, off, Math.min(len, 1000));
			}
		};
	}

	private static void check(byte[] body, long announced) throws IOException {
		assertArrayEquals("announced " + announced, body, AbstractHttpTransaction.readBody(new ByteArrayInputStream(body), announced));
		assertArrayEquals("announced " + announced + ", slow", body, AbstractHttpTransaction.readBody(slow(body), announced));
	}

	@Test
	public void aHugeAnnouncedLengthDoesNotAllocateIt() throws IOException {
		byte[] body = body(10_000);
		check(body, 1_500_000_000L);
		check(body, Integer.MAX_VALUE - 9L);
		check(body, Long.MAX_VALUE);
		check(new byte[0], 1_500_000_000L);
	}

	@Test
	public void aBodyLongerThanTheFirstBufferIsReadWhole() throws IOException {
		byte[] body = body(AbstractHttpTransaction.MAX_FIRST_BUFFER + 12_345);
		check(body, 1_500_000_000L);
		check(body, body.length);
		check(body, -1);
	}

	@Test
	public void theAnnouncedLengthUnknownShorterOrExact() throws IOException {
		for (int length : new int[] {0, 1, 8191, 8192, 8193, 100_000}) {
			byte[] body = body(length);
			check(body, -1);
			check(body, 0);
			check(body, length);
			check(body, Math.max(1, length / 3));
			check(body, length + 10);
		}
	}
}
