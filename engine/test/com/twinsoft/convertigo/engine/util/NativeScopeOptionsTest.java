/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */
package com.twinsoft.convertigo.engine.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;

/** The optimizations whose property is native apply to the native sequences and their calls only. */
public class NativeScopeOptionsTest {
	private static final PropertyName[] OPTIONS = { PropertyName.JAVASCRIPT_SHARED_CLASS_CACHE,
			PropertyName.ENGINE_COPY_ON_DEMAND, PropertyName.CLASSLOADER_LOOKUP_CACHE, PropertyName.JAVASCRIPT_SHARED_SCOPE };

	@Before
	public void setUp() throws Exception {
		EnginePropertiesManager.initProperties();
	}

	@After
	public void tearDown() throws Exception {
		for (PropertyName option : OPTIONS) {
			EnginePropertiesManager.setProperty(option, option.getDefaultValue());
		}
	}

	@Test
	public void nativeByDefault() {
		for (PropertyName option : OPTIONS) {
			assertEquals(option.name(), "native", option.getDefaultValue());
			assertTrue(option.name(), EnginePropertiesManager.isOnFor(option, true));
			assertFalse(option.name(), EnginePropertiesManager.isOnFor(option, false));
		}
	}

	@Test
	public void allOrTrueForEveryRequestable() {
		for (PropertyName option : OPTIONS) {
			for (String value : new String[] { "all", "true" }) {
				EnginePropertiesManager.setProperty(option, value);
				assertTrue(option.name() + "=" + value, EnginePropertiesManager.isOnFor(option, true));
				assertTrue(option.name() + "=" + value, EnginePropertiesManager.isOnFor(option, false));
			}
		}
	}

	@Test
	public void falseForNone() {
		for (PropertyName option : OPTIONS) {
			EnginePropertiesManager.setProperty(option, "false");
			assertFalse(option.name(), EnginePropertiesManager.isOnFor(option, true));
			assertFalse(option.name(), EnginePropertiesManager.isOnFor(option, false));
		}
	}

	@Test
	public void theMarkOfANativeRequestableFollowsTheNestedCalls() {
		assertFalse(NativeRunning.isRunning());
		Boolean outer = NativeRunning.enter(true);
		assertTrue(NativeRunning.isRunning());
		// a classic sequence called from the native one, in the same thread
		Boolean inner = NativeRunning.enter(false);
		assertFalse(NativeRunning.isRunning());
		NativeRunning.restore(inner);
		assertTrue(NativeRunning.isRunning());
		NativeRunning.restore(outer);
		assertFalse(NativeRunning.isRunning());
	}
}
