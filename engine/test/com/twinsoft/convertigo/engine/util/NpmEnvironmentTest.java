/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */
package com.twinsoft.convertigo.engine.util;

import static org.junit.Assert.*;

import java.util.HashMap;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;

public class NpmEnvironmentTest {
	private static final String KEY = ProcessUtils.NPM_ALLOW_ALL_SCRIPTS_ENV;
	private String previous;

	@Before public void init() throws Exception {
		EnginePropertiesManager.initProperties();
		previous = EnginePropertiesManager.getOriginalProperty(PropertyName.NPM_ALLOW_ALL_SCRIPTS);
	}

	@After public void restore() {
		EnginePropertiesManager.setProperty(PropertyName.NPM_ALLOW_ALL_SCRIPTS, previous);
	}

	@Test public void allowsAllScriptsByDefault() {
		var env = new HashMap<String, String>();
		ProcessUtils.configureNpmEnvironment(env);
		assertEquals("true", env.get(KEY));
	}

	@Test public void keepsAValueAlreadySet() {
		var env = new HashMap<String, String>();
		env.put(KEY, "false");
		ProcessUtils.configureNpmEnvironment(env);
		assertEquals("false", env.get(KEY));
	}

	@Test public void canBeDisabledByTheEngineProperty() {
		EnginePropertiesManager.setProperty(PropertyName.NPM_ALLOW_ALL_SCRIPTS, "false");
		var env = new HashMap<String, String>();
		ProcessUtils.configureNpmEnvironment(env);
		assertFalse(env.containsKey(KEY));
	}

	@Test public void npmProcessBuilderCarriesTheSetting() throws Exception {
		var pb = ProcessUtils.getNpmProcessBuilder(null, "npm", "--version");
		assertEquals("true", pb.environment().get(KEY));
	}
}
