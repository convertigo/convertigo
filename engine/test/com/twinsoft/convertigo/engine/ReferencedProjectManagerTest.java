package com.twinsoft.convertigo.engine;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ReferencedProjectManagerTest {
	private static Engine previous;

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	@BeforeClass
	public static void initialize() throws Exception {
		previous = Engine.theApp;
		Engine.logBeans = Engine.logEngine = Engine.logDatabaseObjectManager = Logger.getLogger("references-test");
		EnginePropertiesManager.initProperties();
		Engine.theApp = new Engine();
		Engine.theApp.databaseObjectsManager = new DatabaseObjectsManager();
		Engine.theApp.databaseObjectsManager.symbolsProperties = new Properties();
	}

	@AfterClass
	public static void restore() {
		Engine.theApp = previous;
	}

	@Test
	public void projectWithoutReferencesDoesNotReadIncludedSequences() throws Exception {
		var file = yaml("  ↓Sequence [sequences.GenericSequence]: 🗏 sequences/missing.yaml\n");
		assertTrue(ReferencedProjectManager.references(file).isEmpty());
	}

	@Test
	public void readsOnlyProjectReferencesAndRestoresTheirProperties() throws Exception {
		var file = yaml("  ↓Sequence [sequences.GenericSequence]: 🗏 sequences/missing.yaml\n"
				+ "  ↓First [references.ProjectSchemaReference]: \n"
				+ "    projectName: lib_First\n"
				+ "  ↓Second [references.ProjectSchemaReference]: \n"
				+ "    projectName: lib_Second\n");
		var refs = ReferencedProjectManager.references(file);
		assertEquals(Set.of("lib_First", "lib_Second"), refs.stream()
				.map(ref -> ref.getParser().getProjectName()).collect(Collectors.toSet()));
		assertEquals(Set.of("First", "Second"), refs.stream()
				.map(ref -> ref.getName()).collect(Collectors.toSet()));
	}

	@Test
	public void readsClosedLegacyXmlProjectReferencesWithoutLoadingTheProject() throws Exception {
		var doc = com.twinsoft.convertigo.engine.util.XMLUtils.getDefaultDocumentBuilder().newDocument();
		var root = doc.createElement("convertigo"); doc.appendChild(root);
		var reference = new com.twinsoft.convertigo.beans.references.ProjectSchemaReference();
		reference.setName("Library"); reference.setProjectName("lib_First=https://github.com/example/first.git");
		root.appendChild(reference.toXml(doc));
		var schema = new com.twinsoft.convertigo.beans.references.ImportXsdSchemaReference(); schema.setName("Schema");
		root.appendChild(schema.toXml(doc));
		var file = folder.newFile("Demo.xml");
		com.twinsoft.convertigo.engine.util.XMLUtils.saveXml(doc, file);
		var refs = ReferencedProjectManager.references(file);
		assertEquals(Set.of("lib_First"), refs.stream().map(ref -> ref.getParser().getProjectName()).collect(Collectors.toSet()));
		assertEquals("Library", refs.iterator().next().getName());
	}

	private File yaml(String content) throws Exception {
		var file = folder.newFile("c8oProject.yaml");
		Files.writeString(file.toPath(), "↑convertigo: 8.0.0.m006\n↓Demo [core.Project]: \n" + content);
		return file;
	}
}
