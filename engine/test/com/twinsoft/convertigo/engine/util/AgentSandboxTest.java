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
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.ComboEnum;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyCategory;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.util.AgentSandbox.ToolPolicy;

/**
 * agents.tool_policy: what the No Code agents of an engine may do besides the Convertigo MCP. auto is files;
 * sandboxed-shell runs every process of the agent through the helper, and is files where the kernel cannot confine it,
 * so a command is never run unconfined.
 */
public class AgentSandboxTest {
	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	@Before
	public void init() throws Exception {
		EnginePropertiesManager.initProperties();
	}

	@After
	public void restore() {
		EnginePropertiesManager.setProperty(PropertyName.AGENTS_TOOL_POLICY, PropertyName.AGENTS_TOOL_POLICY.getDefaultValue());
		System.clearProperty(AgentSandbox.HELPER_SYSTEM_PROPERTY);
	}

	// PropertyOptions and its Visibility are private to EnginePropertiesManager
	private static String visibility(PropertyName property) throws Exception {
		for (Annotation annotation : PropertyName.class.getField(property.name()).getAnnotations()) {
			if (annotation.annotationType().getSimpleName().equals("PropertyOptions")) {
				Method visibility = annotation.annotationType().getDeclaredMethod("visibility");
				visibility.setAccessible(true);
				return String.valueOf(visibility.invoke(annotation));
			}
		}
		return null;
	}

	@Test
	public void thePropertyIsAnAdvancedChoiceHiddenInTheCloud() throws Exception {
		PropertyName property = PropertyName.AGENTS_TOOL_POLICY;
		assertEquals("agents.tool_policy", property.getKey());
		assertEquals("auto", property.getDefaultValue());
		assertEquals(PropertyCategory.Account, property.getCategory());
		assertTrue(property.isAdvance());
		assertEquals("HIDDEN_CLOUD", visibility(property));
		assertTrue("listed outside the cloud", property.isVisible());
		List<String> values = Arrays.stream(property.getCombo()).map(ComboEnum::getValue).collect(Collectors.toList());
		assertEquals(List.of("auto", "mcp-only", "files", "sandboxed-shell", "full"), values);
		for (String value : values.subList(1, values.size())) {
			assertEquals(value, ToolPolicy.of(value).getValue());
		}
	}

	@Test
	public void autoIsFiles() {
		for (String auto : new String[] {"auto", "", null, " AUTO ", "sandbox"}) {
			assertEquals(String.valueOf(auto), ToolPolicy.FILES, AgentSandbox.resolve(auto));
		}
		assertEquals(ToolPolicy.MCP_ONLY, AgentSandbox.resolve("mcp-only"));
		assertEquals(ToolPolicy.SANDBOXED_SHELL, AgentSandbox.resolve(" Sandboxed-Shell"));
		assertEquals(ToolPolicy.FULL, AgentSandbox.resolve("full"));
	}

	@Test
	public void theEngineReadsThePolicyFromItsProperty() {
		assertEquals(ToolPolicy.FILES, AgentSandbox.getPolicy());
		EnginePropertiesManager.setProperty(PropertyName.AGENTS_TOOL_POLICY, "mcp-only");
		assertEquals(ToolPolicy.MCP_ONLY, AgentSandbox.getPolicy());
		assertEquals(ToolPolicy.MCP_ONLY, AgentSandbox.getEffectivePolicy());
	}

	@Test
	public void eachPolicySaysWhatTheAgentMayDo() {
		assertFalse(ToolPolicy.MCP_ONLY.allowsFiles());
		assertFalse(ToolPolicy.MCP_ONLY.allowsShell());
		assertTrue(ToolPolicy.FILES.allowsFiles());
		assertFalse(ToolPolicy.FILES.allowsShell());
		assertTrue(ToolPolicy.SANDBOXED_SHELL.allowsShell());
		assertTrue(ToolPolicy.SANDBOXED_SHELL.isConfined());
		assertTrue(ToolPolicy.FULL.allowsShell());
		assertFalse(ToolPolicy.FULL.isConfined());
	}

	@Test
	public void aShellThatCannotBeConfinedIsFiles() throws Exception {
		assertEquals(ToolPolicy.FILES, AgentSandbox.effective(ToolPolicy.SANDBOXED_SHELL, 0));
		assertEquals(ToolPolicy.SANDBOXED_SHELL, AgentSandbox.effective(ToolPolicy.SANDBOXED_SHELL, 1));
		assertEquals(ToolPolicy.FULL, AgentSandbox.effective(ToolPolicy.FULL, 0));
		System.setProperty(AgentSandbox.HELPER_SYSTEM_PROPERTY, new File(folder.getRoot(), "missing").getPath());
		EnginePropertiesManager.setProperty(PropertyName.AGENTS_TOOL_POLICY, "sandboxed-shell");
		assertEquals(ToolPolicy.SANDBOXED_SHELL, AgentSandbox.getPolicy());
		assertEquals(ToolPolicy.FILES, AgentSandbox.getEffectivePolicy());
		JSONObject described = new JSONObject(AgentSandbox.describe());
		assertEquals("sandboxed-shell", described.getString("policy"));
		assertEquals("files", described.getString("effective"));
		assertEquals(0, described.getInt("landlockAbi"));
		assertThrows("never run unconfined", IllegalStateException.class, () -> AgentSandbox.wrap(List.of("bash"), List.of(), List.of()));
	}

	@Test
	public void theCommandRunsThroughTheHelperWithTheSystemReadOnly() {
		List<String> args = AgentSandbox.build("/usr/local/bin/c8o-agent-sandbox", List.of("vibe-acp", "--x"),
				List.of("/workspace/agents/vibe/.venv", "/usr/bin"), List.of("/workspace/agents/vibe/homes/u1", "/workspace/agents/vibe/homes/u1"));
		assertEquals("/usr/local/bin/c8o-agent-sandbox", args.get(0));
		int separator = args.indexOf("--");
		assertEquals(List.of("vibe-acp", "--x"), args.subList(separator + 1, args.size()));
		List<String> options = args.subList(1, separator);
		for (String system : AgentSandbox.SYSTEM_READ_ONLY) {
			assertTrue(system, options.contains(system));
			assertEquals("--ro", options.get(options.indexOf(system) - 1));
		}
		assertEquals("--ro", options.get(options.indexOf("/workspace/agents/vibe/.venv") - 1));
		assertEquals("--rw", options.get(options.indexOf("/workspace/agents/vibe/homes/u1") - 1));
		assertEquals("--rw", options.get(options.indexOf("/dev/null") - 1));
		assertEquals("a path given twice is passed once", 1, options.stream().filter("/workspace/agents/vibe/homes/u1"::equals).count());
		assertEquals(1, options.stream().filter("/usr/bin"::equals).count());
		assertFalse("not Tomcat nor the engine", options.contains("/usr/local") || options.contains("/usr"));
		assertFalse(options.contains("/workspace"));
	}

	@Test
	public void aRelativePathOrNoCommandIsRefused() {
		assertThrows(IllegalArgumentException.class, () -> AgentSandbox.build("h", List.of("bash"), List.of("workspace"), List.of()));
		assertThrows(IllegalArgumentException.class, () -> AgentSandbox.build("h", List.of("bash"), List.of(), Arrays.asList((String) null)));
		assertThrows(IllegalArgumentException.class, () -> AgentSandbox.build("h", List.of(), List.of(), List.of()));
	}

	private String helper(String body) throws Exception {
		File script = folder.newFile();
		Files.write(script.toPath(), ("#!/bin/sh\n" + body + "\n").getBytes(StandardCharsets.UTF_8));
		assertTrue(script.setExecutable(true));
		return script.getPath();
	}

	@Test
	public void theProbeTakesTheLandlockVersionOnSuccessOnly() throws Exception {
		assertEquals(3, AgentSandbox.probe(helper("[ \"$1\" = --probe ] && echo 3"), 5000));
		assertEquals(0, AgentSandbox.probe(helper("echo 0; exit 3"), 5000));
		assertEquals(0, AgentSandbox.probe(helper("echo 2; exit 1"), 5000));
		assertEquals(0, AgentSandbox.probe(helper("echo landlock"), 5000));
		assertEquals(0, AgentSandbox.probe(helper("sleep 5; echo 1"), 300));
		File notExecutable = folder.newFile();
		Files.write(notExecutable.toPath(), "#!/bin/sh\necho 1\n".getBytes(StandardCharsets.UTF_8));
		assertEquals(0, AgentSandbox.probe(notExecutable.getPath(), 5000));
		assertEquals(0, AgentSandbox.probe(new File(folder.getRoot(), "missing").getPath(), 5000));
	}
}
