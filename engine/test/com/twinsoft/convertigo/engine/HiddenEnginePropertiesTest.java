/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */
package com.twinsoft.convertigo.engine;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;

/**
 * The engine properties of the native fast paths and of the traces are hidden: the Configuration page of the admin
 * console and the engine preferences of the Studio do not list them (PropertyName.isVisible), and engine.properties
 * gets no commented default line for them. They are still read from engine.properties or -D, and written when changed.
 */
public class HiddenEnginePropertiesTest {
	private static final List<PropertyName> HIDDEN = List.of(
			PropertyName.JAVASCRIPT_SHARED_CLASS_CACHE,
			PropertyName.ENGINE_COPY_ON_DEMAND,
			PropertyName.CLASSLOADER_LOOKUP_CACHE,
			PropertyName.NATIVE_INLINE_CALLS,
			PropertyName.JAVASCRIPT_SHARED_SCOPE,
			PropertyName.TIMING_TRACE,
			PropertyName.XML_COUNT_DOCUMENTS);

	@Before
	public void init() throws Exception {
		EnginePropertiesManager.initProperties();
	}

	@After
	public void restore() {
		EnginePropertiesManager.setProperty(PropertyName.TIMING_TRACE, PropertyName.TIMING_TRACE.getDefaultValue());
	}

	private static String saved() throws Exception {
		Method save = EnginePropertiesManager.class.getDeclaredMethod("saveProperties", OutputStream.class, String.class);
		save.setAccessible(true);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		save.invoke(null, out, "test");
		return out.toString(StandardCharsets.UTF_8);
	}

	@Test
	public void theyAreNotListed() {
		for (PropertyName property : HIDDEN) {
			assertFalse(property.getKey(), property.isVisible());
			assertTrue(property.getKey(), property.isHidden());
		}
		assertTrue(PropertyName.ZIP_BACKUP_OLD_PROJECT.isVisible());
		assertFalse(PropertyName.ZIP_BACKUP_OLD_PROJECT.isHidden());
	}

	@Test
	public void theyAreNotWrittenAsCommentedDefaults() throws Exception {
		String saved = saved();
		for (PropertyName property : HIDDEN) {
			assertFalse(saved, saved.contains("#" + property.getKey() + "="));
		}
		assertTrue(saved, saved.contains("#" + PropertyName.ZIP_BACKUP_OLD_PROJECT.getKey() + "=false"));
		// a property hidden before keeps its commented default: an existing engine.properties is written as before
		assertTrue(PropertyName.UPDATE_STEPS.isHidden());
		assertTrue(saved, saved.contains("#" + PropertyName.UPDATE_STEPS.getKey() + "=false"));
	}

	@Test
	public void theyAreStillReadAndWrittenWhenChanged() throws Exception {
		assertFalse(EnginePropertiesManager.getPropertyAsBoolean(PropertyName.TIMING_TRACE));
		EnginePropertiesManager.setProperty(PropertyName.TIMING_TRACE, "true");
		assertTrue(EnginePropertiesManager.getPropertyAsBoolean(PropertyName.TIMING_TRACE));
		String saved = saved();
		assertTrue(saved, saved.contains("\ntiming.trace=true") || saved.startsWith("timing.trace=true"));
	}
}
