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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.lang.reflect.Field;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mozilla.javascript.ClassCache;

import com.twinsoft.convertigo.beans.core.RequestableObject;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;

/**
 * javascript.shared_class_cache and classloader.lookup_cache keep what they learn for the life of the engine. When a
 * project is reloaded or removed (DatabaseObjectsManager.releaseProject), they start again empty: the shared
 * descriptions of the Java classes no longer hold the classes of its former class loader, and a class looked up in
 * vain is looked up again.
 */
public class SharedJavaScriptCachesTest {
	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	@Before
	public void init() throws Exception {
		EnginePropertiesManager.initProperties();
	}

	@After
	public void restore() {
		EnginePropertiesManager.setProperty(PropertyName.CLASSLOADER_LOOKUP_CACHE, PropertyName.CLASSLOADER_LOOKUP_CACHE.getDefaultValue());
	}

	private static Object field(String name) throws Exception {
		Field field = RequestableObject.class.getDeclaredField(name);
		field.setAccessible(true);
		return field.get(null);
	}

	@Test
	public void theSharedDescriptionsOfTheJavaClassesStartAgainEmpty() throws Exception {
		ClassCache before = (ClassCache) field("sharedClassCache");
		assertNotNull(before);
		RequestableObject.resetSharedJavaScriptCaches();
		ClassCache after = (ClassCache) field("sharedClassCache");
		assertNotNull(after);
		assertNotSame("a new cache, the former one is no longer referenced by the engine", before, after);
		assertNull("the sealed standard scope tied to the former cache is built again", field("sharedStandardScope"));
	}

	@Test
	public void aClassLookedUpInVainIsLookedUpAgain() throws Exception {
		EnginePropertiesManager.setProperty(PropertyName.CLASSLOADER_LOOKUP_CACHE, "all");
		File libs = folder.newFolder("libs");
		DirClassLoader loader = new DirClassLoader(libs, getClass().getClassLoader());
		for (int i = 0; i < 2; i++) {
			try {
				loader.loadClass("com.example.NotThereYet");
				fail("the class does not exist");
			} catch (ClassNotFoundException e) {
				// expected
			}
		}
		assertEquals(loader.loadClass("java.lang.String"), String.class);
		assertEquals("the miss and the hit are remembered", 2, loader.lookupCount());
		loader.clearLookups();
		assertEquals(0, loader.lookupCount());
	}
}
