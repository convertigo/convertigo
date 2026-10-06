package com.twinsoft.convertigo.engine.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.apache.log4j.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.twinsoft.convertigo.engine.Engine;

public class YamlConverterTest {
	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	@Test
	public void projectExportReplacesYamlSourcesAndKeepsOtherProjectDocuments() throws Exception {
		Engine.logEngine = Logger.getLogger("yaml-test");
		var project = folder.newFolder("Demo").toPath();
		var sources = project.resolve("_c8oProject");
		Files.createDirectories(sources.resolve("sequences"));
		// A document of a newer Studio, as the tags of the project, is not a source of this writer.
		var tags = sources.resolve("tags.json");
		var tagsContent = "{\n  \"schemaVersion\": 1\n}\n".getBytes(StandardCharsets.UTF_8);
		Files.write(tags, tagsContent);
		var obsolete = sources.resolve("sequences/Removed.yaml");
		Files.writeString(obsolete, "obsolete");
		var document = XMLUtils.parseDOMFromString("<convertigo>"
				+ "<bean yaml_key=\"Demo [core.Project]\">"
				+ "<bean yaml_key=\"Main [sequences.GenericSequence]\" yaml_file=\"sequences/Main.yaml\" comment=\"main\"/>"
				+ "</bean></convertigo>");

		YamlConverter.writeYaml(document, project.resolve("c8oProject.yaml").toFile(), sources.toFile());

		assertTrue(Files.isRegularFile(project.resolve("c8oProject.yaml")));
		assertTrue(Files.readString(sources.resolve("sequences/Main.yaml")).contains("main"));
		assertFalse("an obsolete YAML source must be removed", Files.exists(obsolete));
		assertArrayEquals("another project document must be kept unchanged", tagsContent, Files.readAllBytes(tags));
	}
}
