/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */
package com.twinsoft.convertigo.engine.flow;

import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * For the tests of the persistent keys: time passing for what they keep in memory (the dates of the walks of their
 * projects, of the resolutions of their scopes and of the reads of the files of their requests moved back), and the
 * wait for their verifications in the background.
 */
final class AuthoringKeyClock {
	private AuthoringKeyClock() {
	}

	/** As if what the keys keep had been verified this long before. */
	@SuppressWarnings("unchecked")
	static void elapse(long millis) throws Exception {
		var roots = (Map<Object, Object>) field(FlowEngineBridge.class, "rootFingerprints", null);
		var type = Class.forName(FlowEngineBridge.class.getName() + "$RootFingerprint");
		var constructor = type.getDeclaredConstructors()[0];
		constructor.setAccessible(true);
		for (var entry : roots.entrySet()) {
			if (!String.valueOf(entry.getKey()).startsWith(FlowEngineBridge.AUTHORING_ROOT)) {
				continue;
			}
			var value = entry.getValue();
			var components = type.getRecordComponents();
			var arguments = new Object[components.length];
			for (int i = 0; i < components.length; i++) {
				var accessor = components[i].getAccessor();
				accessor.setAccessible(true);
				arguments[i] = accessor.invoke(value);
				if (components[i].getName().equals("computedAt")) {
					arguments[i] = (Long) arguments[i] - millis;
				}
			}
			entry.setValue(constructor.newInstance(arguments));
		}
		shift((Map<?, ?>) field(FlowAuthoringKeys.class, "scopes", null), "resolvedAt", millis);
		shift((Map<?, ?>) field(FlowAuthoringKeys.class, "files", null), "verifiedAt", millis);
	}

	/** Waits for the verifications started in the background to end. */
	@SuppressWarnings("unchecked")
	static void awaitVerifications() throws Exception {
		var executor = (ThreadPoolExecutor) field(FlowAuthoringKeys.class, "verifications", null);
		var refreshing = (Map<Object, ?>) field(FlowAuthoringKeys.class, "refreshing", null);
		var deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
		while (!refreshing.isEmpty() || executor.getActiveCount() > 0 || !executor.getQueue().isEmpty()) {
			if (System.nanoTime() > deadline) {
				throw new IllegalStateException("The authoring keys were not verified in time.");
			}
			Thread.sleep(10);
		}
	}

	/** @return the threads of the verifications in the background */
	static int verificationThreads() throws Exception {
		return ((ThreadPoolExecutor) field(FlowAuthoringKeys.class, "verifications", null)).getPoolSize();
	}

	private static void shift(Map<?, ?> map, String name, long millis) throws Exception {
		for (var value : map.values()) {
			var field = value.getClass().getDeclaredField(name);
			field.setAccessible(true);
			field.setLong(value, field.getLong(value) - millis);
		}
	}

	private static Object field(Class<?> type, String name, Object target) throws Exception {
		var field = type.getDeclaredField(name);
		field.setAccessible(true);
		return field.get(target);
	}
}
