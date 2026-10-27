/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */
package com.twinsoft.convertigo.engine.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.junit.Test;

/**
 * Crypto2 keeps the key derived from its passphrase and reuses its ciphers: its output stays the same, byte for byte,
 * as the former implementation that built both ciphers at each call, including from concurrent threads.
 */
public class Crypto2Test {

	// Produced by the former implementation, which built a key and ciphers at each call
	private static final String[][] KNOWN = {
		{"unit-test-passphrase", "hello", "xe86d5ccd43916ea5f793a0c52ef7b171"},
		{"unit-test-passphrase", "", "x41c98d9e96a1ac84a9872ab4c1d18e32"},
		{"another passphrase é", "données accentuées €", "xb9d52a5e18e019f5370c1b01512e0b28c0466902299c480df3bd6d0a43dedce4"},
		{"p", "0123456789abcdef", "xbae3f00fbc5f47d168c6e7fafae90589cd1d160ed0185d6442a17ace2f4e75b8"},
		{"unit-test-passphrase", "a longer text that spans several AES blocks of sixteen bytes each",
			"xb6dcec90d68331be46dcc72a2ebc884f1dc21eada0b83c27ef950051a4eaed0253668b117983a13e55cf79813cedb1f0d221e8aa2c18c7443ca20cb2856cff05aaba4e45d6a80bee38608207e5c2189a"},
	};

	/** The former implementation: a key and a cipher built for each call. */
	private static byte[] reference(int mode, String passphrase, byte[] data) throws Exception {
		byte[] digest = MessageDigest.getInstance("MD5").digest(passphrase.getBytes(StandardCharsets.UTF_8));
		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		cipher.init(mode, new SecretKeySpec(digest, "AES"), new IvParameterSpec(digest));
		return cipher.doFinal(data);
	}

	private static String referenceHex(String passphrase, String text) throws Exception {
		return "x" + HexUtils.toHexString(reference(Cipher.ENCRYPT_MODE, passphrase, text.getBytes(StandardCharsets.UTF_8)));
	}

	private static String text(Random random, int length) {
		var builder = new StringBuilder();
		for (int i = 0; i < length; i++) {
			builder.append((char) (random.nextInt(4) == 0 ? 0xe0 + random.nextInt(32) : 0x20 + random.nextInt(95)));
		}
		return builder.toString();
	}

	@Test public void knownValuesStayTheSameOnRepeatedCalls() {
		for (int round = 0; round < 3; round++) {
			for (var known : KNOWN) {
				assertEquals(known[2], Crypto2.encodeToHexString(known[0], known[1]));
				assertEquals(known[1], Crypto2.decodeFromHexString(known[0], known[2]));
			}
		}
	}

	@Test public void matchesTheFormerImplementation() throws Exception {
		var random = new Random(1220);
		String[] passphrases = {"first", "second passphrase", "éè€", ""};
		for (int i = 0; i < 400; i++) {
			var passphrase = passphrases[i % passphrases.length];
			var text = text(random, random.nextInt(70));
			var hex = Crypto2.encodeToHexString(passphrase, text);
			assertEquals(referenceHex(passphrase, text), hex);
			assertEquals(text, Crypto2.decodeFromHexString(passphrase, hex));
			var bytes = text.getBytes(StandardCharsets.UTF_8);
			var ciphered = Crypto2.encodeToByteArray(passphrase, bytes);
			assertArrayEquals(reference(Cipher.ENCRYPT_MODE, passphrase, bytes), ciphered);
			assertArrayEquals(bytes, Crypto2.decodeFromByteArray(passphrase, ciphered));
		}
	}

	@Test public void aFailedDecodingDoesNotSpoilTheNextOnes() throws Exception {
		var hex = Crypto2.encodeToHexString("unit-test-passphrase", "hello");
		assertNull(Crypto2.decodeFromHexString("unit-test-passphrase", hex.substring(0, hex.length() - 2)));
		assertNull(Crypto2.decodeFromByteArray("unit-test-passphrase", new byte[] {1, 2, 3}));
		assertNull(Crypto2.decodeFromHexString("another passphrase", hex));
		for (int i = 0; i < 40; i++) {
			assertEquals("hello", Crypto2.decodeFromHexString("unit-test-passphrase", hex));
			assertEquals(hex, Crypto2.encodeToHexString("unit-test-passphrase", "hello"));
		}
	}

	@Test public void concurrentCallsGiveTheSameBytes() throws Exception {
		int threads = Math.max(8, Runtime.getRuntime().availableProcessors() * 2);
		var random = new Random(27);
		int count = 64;
		String[] passphrases = {"concurrent one", "concurrent two"};
		var texts = new String[count];
		var expected = new String[count];
		for (int i = 0; i < count; i++) {
			texts[i] = text(random, 1 + random.nextInt(90));
			expected[i] = referenceHex(passphrases[i % 2], texts[i]);
		}
		ExecutorService pool = Executors.newFixedThreadPool(threads);
		try {
			var gate = new CyclicBarrier(threads);
			var futures = new ArrayList<Future<Integer>>();
			for (int t = 0; t < threads; t++) {
				int offset = t;
				futures.add(pool.submit(() -> {
					gate.await();
					int checked = 0;
					for (int n = 0; n < 3000; n++) {
						int i = (offset * 7 + n) % count;
						// two passphrases in turn: the kept key changes while other threads use the former one
						var passphrase = passphrases[i % 2];
						var hex = Crypto2.encodeToHexString(passphrase, texts[i]);
						assertEquals(expected[i], hex);
						assertEquals(texts[i], Crypto2.decodeFromHexString(passphrase, expected[i]));
						checked++;
					}
					return checked;
				}));
			}
			int total = 0;
			for (var future : futures) {
				total += future.get(120, TimeUnit.SECONDS);
			}
			assertEquals(threads * 3000, total);
		} finally {
			pool.shutdownNow();
		}
	}
}
