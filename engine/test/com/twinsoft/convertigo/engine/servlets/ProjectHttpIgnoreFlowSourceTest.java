/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */
package com.twinsoft.convertigo.engine.servlets;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.twinsoft.convertigo.engine.flow.FlowSourceLayout;

public class ProjectHttpIgnoreFlowSourceTest {
	@Rule public TemporaryFolder temporary = new TemporaryFolder();

	private Path project(String rules) throws Exception {
		var root = temporary.newFolder().toPath();
		Files.writeString(root.resolve(".httpignore"), rules, StandardCharsets.UTF_8);
		return root;
	}

	@Test
	public void rootRuleProtectsAllSourceKindsWithoutHidingPublicArtifacts() throws Exception {
		var root = project("/_flow/\n");
		assertTrue(ProjectHttpIgnore.isIgnored(root.toFile(), "_flow", true));
		for (var name : new String[] { "engine.yaml", "flows/Hello.flow.js", "blocks/custom.block.js",
				"frontbuilder/svelte/model/App/src/routes/+page.flow.svelte", "resources/private.json" }) {
			assertTrue(name, ProjectHttpIgnore.isIgnored(root.toFile(), "_flow/" + name, false));
		}
		for (var name : new String[] { "resources/logo.png", "DisplayObjects/mobile/index.html", "index.html" }) {
			assertFalse(name, ProjectHttpIgnore.isIgnored(root.toFile(), name, false));
		}
		assertTrue(ProjectHttpIgnore.isIgnored(root.toFile(), ".httpignore", false));
	}

	@Test
	public void appendedRootRuleOverridesOldExceptionsAndAcceptsCrLf() throws Exception {
		var root = project("!/_flow/\r\n!/_flow/resources/**\r\n/_flow/\r\n");
		assertTrue(ProjectHttpIgnore.isIgnored(root.toFile(), "_flow/resources/private.json", false));
		assertTrue(ProjectHttpIgnore.isIgnored(root.toFile(), "/_flow/flows/Hello.flow.js", false));
		assertTrue(ProjectHttpIgnore.isIgnored(root.toFile(), "_flow\\flows\\Hello.flow.js", false));
	}

	@Test
	public void directoryNameAloneIsNotAProtection() throws Exception {
		var root = project("");
		assertFalse(ProjectHttpIgnore.isIgnored(root.toFile(), "_flow/engine.yaml", false));
		Files.writeString(root.resolve(".httpignore"), "/_flow/\n", StandardCharsets.UTF_8);
		assertTrue(ProjectHttpIgnore.isIgnored(root.toFile(), "_flow/engine.yaml", false));
	}

	@Test
	public void canonicalSourceHelperMakesTheRootNonPublicBeforeItsFirstWrite() throws Exception {
		var root = temporary.newFolder().toPath();
		FlowSourceLayout.FLOW.ensureHttpIgnore(root.toFile());
		assertTrue(ProjectHttpIgnore.isIgnored(root.toFile(), "_flow", true));
		assertTrue(ProjectHttpIgnore.isIgnored(root.toFile(), "_flow/frontbuilder/svelte/model/App/src/routes/+page.flow.svelte", false));
	}

	@Test
	public void canonicalSourceHelperRestoresProtectionAfterAnOldRootNegation() throws Exception {
		for (var negation : new String[] { "!/_flow/", "!/*", "!/**" }) {
			var root = project("/_flow/\n" + negation + "\n");
			FlowSourceLayout.FLOW.ensureHttpIgnore(root.toFile());
			assertEquals("/_flow/\n" + negation + "\n/_flow/\n", Files.readString(root.resolve(".httpignore")));
			assertTrue(negation, ProjectHttpIgnore.isIgnored(root.toFile(), "_flow/engine.yaml", false));
		}
	}
}
