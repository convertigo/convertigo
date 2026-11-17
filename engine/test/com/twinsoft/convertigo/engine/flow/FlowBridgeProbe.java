/* Copyright (c) 2001-2026 Convertigo SA. Licensed under the GNU AGPL v3. */
package com.twinsoft.convertigo.engine.flow;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.atomic.LongAdder;

/** Test-only snapshot of every runtime bridge invocation, regardless of method. */
public final class FlowBridgeProbe {
	private FlowBridgeProbe() { }

	public static long calls() {
		try {
			Field statsField = FlowEngineBridge.class.getDeclaredField("invocationStats");
			statsField.setAccessible(true);
			long calls = 0;
			for (Object stats : ((Map<?, ?>) statsField.get(null)).values()) {
				Field countField = stats.getClass().getDeclaredField("count");
				countField.setAccessible(true);
				calls += ((LongAdder) countField.get(stats)).sum();
			}
			return calls;
		} catch (ReflectiveOperationException failure) {
			throw new AssertionError("Unable to read bridge invocation counter", failure);
		}
	}
}
