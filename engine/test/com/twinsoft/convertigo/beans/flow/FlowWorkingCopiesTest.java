package com.twinsoft.convertigo.beans.flow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.Sequence;

public class FlowWorkingCopiesTest {
	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	/** The objects of a project with a Flow engine and a Flow, as a load of the project gives them. */
	private static class Loaded {
		final FlowEngine engine;
		final Flow flow;
		final Project project;

		Loaded(File directory) throws Exception {
			var holder = new Loaded[] { this };
			project = new Project() {
				@Override
				public File getDirFile() {
					return directory;
				}

				@Override
				public FlowEngine getFlowEngine() {
					return holder[0].engine;
				}

				@Override
				public List<Sequence> getSequencesList() {
					return List.of(holder[0].flow);
				}
			};
			engine = new FlowEngine() {
				@Override
				public Project getProject() {
					return project;
				}
			};
			flow = new Flow() {
				@Override
				public Project getProject() {
					return project;
				}
			};
			flow.setName("probe");
		}
	}

	private Path page;
	private Path engineFile;
	private Path flowFile;

	private File saved() throws Exception {
		var directory = folder.newFolder();
		page = directory.toPath().resolve("_flow/frontbuilder/page.flow.svelte");
		engineFile = directory.toPath().resolve("_flow/engine.yaml");
		flowFile = directory.toPath().resolve("_flow/flows/probe.flow.js");
		Files.createDirectories(page.getParent());
		Files.createDirectories(flowFile.getParent());
		Files.writeString(page, "saved page");
		Files.writeString(engineFile, "saved engine");
		Files.writeString(flowFile, "saved flow");
		return directory;
	}

	@Test
	public void aStateTakesTheSourcesNotSavedAndGivesThemBackToTheProjectLoadedAgain() throws Exception {
		var directory = saved();
		var current = new Loaded(directory);
		current.engine.setFrontendSource(page.toString(), "draft page");
		current.engine.setEngineSource("draft engine");
		current.flow.setFlowSource("draft flow");

		var copies = FlowWorkingCopies.of(current.project);
		assertEquals(Map.of(
				"engine", "draft engine",
				"file:_flow/frontbuilder/page.flow.svelte", "draft page",
				"flow:probe", "draft flow"), copies);
		// taking a state, which serializes the project, writes no source
		current.engine.toXml(DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument());
		assertEquals("saved page", Files.readString(page));
		assertEquals("saved engine", Files.readString(engineFile));

		// the project loaded again drops the working copies of its previous objects
		Flow.projectUnloaded(current.project);
		var restored = new Loaded(directory);
		assertEquals(Map.of(), FlowWorkingCopies.of(restored.project));
		FlowWorkingCopies.restore(restored.project, copies, copies);

		assertEquals("draft page", restored.engine.getFrontendSource(page.toString()));
		assertTrue(restored.engine.isSourceDirty(page.toString()));
		assertEquals("draft engine", restored.engine.getEngineSource());
		assertTrue(restored.engine.isEngineSourceDirty());
		assertEquals("draft flow", restored.flow.getFlowSource());
		assertTrue(restored.flow.isFlowSourceDirty());
		assertEquals(copies, FlowWorkingCopies.of(restored.project));
		assertEquals("saved page", Files.readString(page));
		assertEquals("saved flow", Files.readString(flowFile));
		Flow.projectUnloaded(restored.project);
	}

	@Test
	public void aCopyEqualToTheSavedSourceIsNotAChange() throws Exception {
		var directory = saved();
		var restored = new Loaded(directory);
		FlowWorkingCopies.restore(restored.project, Map.of(
				"engine", "saved engine",
				"file:_flow/frontbuilder/page.flow.svelte", "saved page",
				"flow:probe", "saved flow"), Map.of(
				"engine", "saved engine",
				"file:_flow/frontbuilder/page.flow.svelte", "saved page",
				"flow:probe", "saved flow"));
		assertFalse(restored.engine.isSourceDirty(page.toString()));
		assertFalse(restored.engine.isEngineSourceDirty());
		assertFalse(restored.flow.isFlowSourceDirty());
		assertEquals(Map.of(), FlowWorkingCopies.of(restored.project));
	}

	@Test
	public void theSaveOfTheProjectWritesTheWorkingCopies() throws Exception {
		var directory = saved();
		var current = new Loaded(directory);
		current.engine.setFrontendSource(page.toString(), "draft page");
		current.engine.setEngineSource("draft engine");
		current.engine.saveSources();
		assertEquals("draft page", Files.readString(page));
		assertEquals("draft engine", Files.readString(engineFile));
		assertEquals(Map.of(), FlowWorkingCopies.of(current.project));
	}
}
