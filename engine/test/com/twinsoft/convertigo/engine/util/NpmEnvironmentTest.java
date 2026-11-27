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
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;

public class NpmEnvironmentTest {
	private static final String KEY = ProcessUtils.NPM_ALLOW_ALL_SCRIPTS_ENV;
	private String previous;
	@Rule public TemporaryFolder project = new TemporaryFolder();

	@Before public void init() throws Exception {
		EnginePropertiesManager.initProperties();
		previous = EnginePropertiesManager.getOriginalProperty(PropertyName.NPM_ALLOW_ALL_SCRIPTS);
	}

	@After public void restore() {
		EnginePropertiesManager.setProperty(PropertyName.NPM_ALLOW_ALL_SCRIPTS, previous);
	}

	@Test public void allowsAllScriptsByDefault() {
		var env = new HashMap<String, String>();
		ProcessUtils.configureNpmEnvironment(env, project.getRoot().toPath());
		assertEquals("true", env.get(KEY));
	}

	@Test public void keepsAValueAlreadySet() {
		var env = new HashMap<String, String>();
		String inheritedKey = KEY.toUpperCase(Locale.ROOT);
		env.put(inheritedKey, "false");
		ProcessUtils.configureNpmEnvironment(env, project.getRoot().toPath());
		assertEquals("false", env.get(inheritedKey));
		assertFalse(env.containsKey(KEY));
	}

	@Test public void canBeDisabledByTheEngineProperty() {
		EnginePropertiesManager.setProperty(PropertyName.NPM_ALLOW_ALL_SCRIPTS, "false");
		var env = new HashMap<String, String>();
		ProcessUtils.configureNpmEnvironment(env, project.getRoot().toPath());
		assertFalse(env.containsKey(KEY));
	}

	@Test public void npmProcessBuilderCarriesTheSetting() throws Exception {
		var pb = ProcessUtils.getNpmProcessBuilder(null, project.getRoot(), "npm", "--version");
		assertEquals(project.getRoot(), pb.directory());
		assertEquals("true", pb.environment().get(KEY));
	}

	@Test public void respectsAProjectScriptPolicy() throws Exception {
		Files.writeString(project.getRoot().toPath().resolve("package.json"),
				"""
				{"allowScripts":{"fixture-dependency":false}}
				""");
		var pb = ProcessUtils.getNpmProcessBuilder(null, project.getRoot(), "npm", "ci");
		assertFalse(pb.environment().containsKey(KEY));
	}

	@Test public void respectsAnEmptyProjectScriptPolicy() throws Exception {
		Files.writeString(project.getRoot().toPath().resolve("package.json"), "{\"allowScripts\":{}}");
		var env = new HashMap<String, String>();
		ProcessUtils.configureNpmEnvironment(env, project.getRoot().toPath());
		assertFalse(env.containsKey(KEY));
	}

	@Test public void respectsProjectNpmConfiguration() throws Exception {
		Files.writeString(project.getRoot().toPath().resolve("package.json"), "{}");
		Files.writeString(project.getRoot().toPath().resolve(".npmrc"),
				"""
				# Project lifecycle scripts
				allow-scripts[] = fixture-dependency
				""");
		var env = new HashMap<String, String>();
		ProcessUtils.configureNpmEnvironment(env, project.newFolder("src").toPath());
		assertFalse(env.containsKey(KEY));
	}

	@Test public void ignoresCommentedOutPolicies() throws Exception {
		Files.writeString(project.getRoot().toPath().resolve(".npmrc"), "# allow-scripts = fixture-dependency");
		var env = new HashMap<String, String>();
		ProcessUtils.configureNpmEnvironment(env, project.getRoot().toPath());
		assertEquals("true", env.get(KEY));
	}

	@Test public void npmAppliesTheProjectPolicyToDependencyInstallScripts() throws Exception {
		Path root = project.getRoot().toPath();
		Path npmrc = Files.createFile(root.resolve("empty.npmrc"));
		String version;
		try {
			version = runNpm(root, npmrc, 0, "--version").trim();
		} catch (IOException npmUnavailable) {
			assumeTrue("npm must be available to test lifecycle scripts", false);
			return;
		}
		assumeTrue("npm 11.20 or later in the npm 11 series", version.matches("11\\.(?:2[0-9]|[3-9][0-9]|[0-9]{3,})\\.[0-9]+"));

		Path dependency = project.newFolder("dependency").toPath();
		Files.writeString(dependency.resolve("package.json"), """
				{"name":"fixture-dependency","version":"1.0.0","scripts":{"install":"node mark.cjs"}}
				""");
		Files.writeString(dependency.resolve("mark.cjs"), "require('node:fs').writeFileSync('script-ran', '')");
		runNpm(dependency, npmrc, 0, "pack", "--ignore-scripts", "--pack-destination", root.toString());
		String manifest = """
				{"name":"fixture-project","dependencies":{"fixture-dependency":"file:../fixture-dependency-1.0.0.tgz"}%s}
				""";
		Path restricted = project.newFolder("restricted").toPath();
		Files.writeString(restricted.resolve("package.json"), manifest.formatted("""
				,"allowScripts":{"file:../fixture-dependency-1.0.0.tgz":false}
				"""));
		Path configured = project.newFolder("configured").toPath();
		Files.writeString(configured.resolve("package.json"), manifest.formatted(""));
		Files.writeString(configured.resolve(".npmrc"), "allow-scripts=another-package\nstrict-allow-scripts=true\n");
		Path compatible = project.newFolder("compatible").toPath();
		Files.writeString(compatible.resolve("package.json"), manifest.formatted(""));

		assertFalse(dependencyInstallScriptRan(restricted, npmrc));
		String error = runNpm(configured, npmrc, 1, "install", "--offline", "--no-audit", "--no-fund");
		assertTrue(error, error.contains("strict-allow-scripts"));
		assertTrue(dependencyInstallScriptRan(compatible, npmrc));
	}

	private static boolean dependencyInstallScriptRan(Path directory, Path npmrc) throws Exception {
		runNpm(directory, npmrc, 0, "install", "--offline", "--no-audit", "--no-fund");
		Path installed = directory.resolve("node_modules/fixture-dependency");
		assertTrue(Files.isRegularFile(installed.resolve("mark.cjs")));
		return Files.isRegularFile(installed.resolve("script-ran"));
	}

	private static String runNpm(Path directory, Path npmrc, int expectedExit, String... arguments) throws Exception {
		var command = new LinkedList<>(List.of(arguments));
		command.addFirst("npm");
		var builder = ProcessUtils.getNpmProcessBuilder(null, directory.toFile(), command);
		var env = builder.environment();
		env.put("npm_config_userconfig", npmrc.toString());
		env.put("npm_config_globalconfig", npmrc.resolveSibling("global.npmrc").toString());
		env.put("npm_config_cache", npmrc.resolveSibling("cache").toString());
		Path log = Files.createTempFile(npmrc.getParent(), "npm-", ".log");
		var process = builder.redirectErrorStream(true).redirectOutput(log.toFile()).start();
		try {
			assertTrue("npm timed out", process.waitFor(30, TimeUnit.SECONDS));
			String output = Files.readString(log);
			assertEquals(output, expectedExit, process.exitValue());
			return output;
		} finally {
			if (process.isAlive()) {
				process.destroyForcibly();
			}
		}
	}
}
