/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */
package com.twinsoft.convertigo.beans;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import org.apache.log4j.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.w3c.dom.Document;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.util.YamlConverter;

/**
 * A project saved by an older engine, with sequences that use none of the native options: loading it does not migrate
 * it (a migration deletes its _data and _private folders and rewrites it) and saving it writes the same file. The
 * native options of a sequence (nativeExecution, jsonNativeOutput) are in the default values of GenericSequence since
 * 7.9.0, not in a newer entry that made every project with a sequence older than it.
 */
public class OldProjectNoMigrationTest {
	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	@Test
	public void anOldProjectWithSequencesLoadsWithoutMigrationAndSavesTheSameFiles() throws Exception {
		Map<String, String> old = new TreeMap<String, String>();
		old.put("c8oProject.yaml", lines(
				"↑convertigo: 7.9.0.m006",
				"↓Demo [core.Project]: ",
				"  ↓Hello [sequences.GenericSequence]: 🗏 sequences/Hello.yaml",
				"  ↓Other [sequences.GenericSequence]: 🗏 sequences/Other.yaml"));
		old.put("_c8oProject/sequences/Hello.yaml", lines(
				"accessibility: Private",
				"↓Greeting [steps.SimpleStep-1001]: ",
				"  expression: greeting = \"hello\";",
				"↓who [variables.RequestableVariable-1002]: "));
		old.put("_c8oProject/sequences/Other.yaml", lines(
				"responseTimeout: 120"));
		Roundtrip roundtrip = roundtrip("Demo", old);
		assertEquals("no migration of a project without any new feature", null, roundtrip.migrating);
		assertEquals("the files written again are the same", old, roundtrip.written);
	}

	@Test
	public void aSequenceThatSetsANativeOptionKeepsItWithoutRaisingTheVersion() throws Exception {
		Map<String, String> nativeProject = new TreeMap<String, String>();
		nativeProject.put("c8oProject.yaml", lines(
				"↑convertigo: 7.9.0.m006",
				"↓Native [core.Project]: ",
				"  ↓Fast [sequences.GenericSequence]: 🗏 sequences/Fast.yaml"));
		nativeProject.put("_c8oProject/sequences/Fast.yaml", lines(
				"jsonNativeOutput: true",
				"nativeExecution: true",
				"↓Step [steps.SimpleStep-1001]: ",
				"  expression: x = 1;"));
		Roundtrip roundtrip = roundtrip("Native", nativeProject);
		assertEquals(null, roundtrip.migrating);
		assertEquals("only the options that differ from their default are written", nativeProject, roundtrip.written);
		assertNotEquals("the option is read", -1, roundtrip.unshrunk.indexOf("name=\"nativeExecution\"><java.lang.Boolean value=\"true\""));
	}

	private static String lines(String... lines) {
		// the engine writes no line break after the last line
		return String.join("\n", lines);
	}

	private static class Roundtrip {
		String migrating;
		Map<String, String> written = new TreeMap<String, String>();
		String unshrunk;
	}

	/** what loading (DatabaseObjectsManager) and saving (CarUtils.exportProject) do with the project files */
	private Roundtrip roundtrip(String name, Map<String, String> files) throws Exception {
		Logger logger = Logger.getLogger("old-project-no-migration-test");
		Engine.logBeans = logger;
		Engine.logEngine = logger;

		File project = folder.newFolder(name);
		for (Map.Entry<String, String> file : files.entrySet()) {
			File target = new File(project, file.getKey());
			target.getParentFile().mkdirs();
			Files.write(target.toPath(), file.getValue().getBytes(StandardCharsets.UTF_8));
		}

		Document document = BeansDefaultValues.unshrinkProject(YamlConverter.readYaml(new File(project, "c8oProject.yaml")));
		Roundtrip roundtrip = new Roundtrip();
		roundtrip.migrating = (String) document.getUserData("isMigrating");
		roundtrip.unshrunk = com.twinsoft.convertigo.engine.util.XMLUtils.prettyPrintDOM(document).replaceAll(">\\s+<", "><");

		File written = folder.newFolder(name + "-written");
		YamlConverter.writeYaml(BeansDefaultValues.shrinkProject(document), new File(written, "c8oProject.yaml"), new File(written, "_c8oProject"));
		try (Stream<Path> paths = Files.walk(written.toPath())) {
			for (Path path : (Iterable<Path>) paths.filter(Files::isRegularFile)::iterator) {
				String relative = written.toPath().relativize(path).toString().replace(File.separatorChar, '/');
				roundtrip.written.put(relative, new String(Files.readAllBytes(path), StandardCharsets.UTF_8));
			}
		}
		return roundtrip;
	}
}
