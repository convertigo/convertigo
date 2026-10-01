package com.twinsoft.convertigo.beans.flow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.Rule;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import org.junit.rules.TemporaryFolder;

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.Sequence;
import com.twinsoft.convertigo.engine.Engine;

public class FlowWorkingCopiesTest {
	private org.apache.log4j.Logger previousStudioLogger;
	@Before public void headlessStudioLogger() {
		previousStudioLogger = Engine.logStudio;
		Engine.logStudio = org.apache.log4j.Logger.getLogger(getClass());
	}
	@After public void restoreStudioLogger() { Engine.logStudio = previousStudioLogger; }

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

	@Test
	public void historyRestoresBothSidesOfAMoveAndUndoRedoNeverWriteDisk() throws Exception {
		var directory = saved();
		var target = directory.toPath().resolve("_flow/frontbuilder/moved.flow.svelte");
		var current = new Loaded(directory);
		current.engine.applySourceChanges(Map.of(target.toString(), "moved draft"), List.of(page.toString()));
		var moved = FlowWorkingCopies.of(current.project);
		assertEquals(Map.of("removed-file:_flow/frontbuilder/page.flow.svelte", "",
				"file:_flow/frontbuilder/moved.flow.svelte", "moved draft"), moved);
		Flow.projectUnloaded(current.project);
		var undone = new Loaded(directory);
		FlowWorkingCopies.restore(undone.project, Map.of(), moved);
		assertEquals("saved page", undone.engine.getSource(page.toString()));
		assertFalse(undone.engine.hasSource(target.toString()));
		Flow.projectUnloaded(undone.project);
		var redone = new Loaded(directory);
		FlowWorkingCopies.restore(redone.project, moved, Map.of());
		assertFalse(redone.engine.hasSource(page.toString()));
		assertEquals("moved draft", redone.engine.getSource(target.toString()));
		assertEquals(moved, FlowWorkingCopies.of(redone.project));
		assertEquals("saved page", Files.readString(page));
		assertFalse(Files.exists(target));
		redone.engine.saveSources();
		assertFalse(Files.exists(page));
		assertEquals("moved draft", Files.readString(target));
		assertTrue(FlowWorkingCopies.of(redone.project).isEmpty());
	}

	@Test
	public void reloadRemembersOnlyAffectedPathsUntilTheReplacementProjectLoads() throws Exception {
		var directory = saved();
		var target = directory.toPath().resolve("_flow/new/page.flow.svelte");
		var metadata = directory.toPath().resolve("_flow/meta.json");
		var current = new Loaded(directory);
		current.engine.applySourceChanges(Map.of(target.toString(), "moved draft",
				metadata.toString(), "metadata draft"), List.of(page.toString()));
		Flow.projectUnloaded(current.project);
		var reloaded = new Loaded(directory);
		assertEquals("saved page", reloaded.engine.getSource(page.toString()));
		assertFalse(reloaded.engine.hasSource(target.toString()));
		assertFalse(reloaded.engine.hasSource(metadata.toString()));
		var affected = FlowEngine.takeDiscardedSourcePaths(reloaded.project);
		assertEquals(Set.of(page.toFile().getCanonicalPath(), target.toFile().getCanonicalPath(),
				metadata.toFile().getCanonicalPath()), affected);
		assertTrue(FlowEngine.takeDiscardedSourcePaths(reloaded.project).isEmpty());
		assertFalse(Files.exists(target));
		assertFalse(Files.exists(metadata));
	}

	@Test
	public void historyRestoresTheFinalEffectiveStateBeforeConsumingReloadNotifications() throws Exception {
		var directory = saved();
		var current = new Loaded(directory);
		current.engine.setSource(page.toString(), "draft page");
		var copies = FlowWorkingCopies.of(current.project);
		Flow.projectUnloaded(current.project);
		var restored = new Loaded(directory);
		FlowWorkingCopies.restore(restored.project, copies, copies);
		assertEquals("draft page", restored.engine.getSource(page.toString()));
		assertTrue(FlowEngine.takeDiscardedSourcePaths(restored.project).isEmpty());
		assertEquals("saved page", Files.readString(page));
		Flow.projectUnloaded(restored.project);
		FlowEngine.takeDiscardedSourcePaths(restored.project);
	}

	@Test
	public void reloadOfSavedSourcesDoesNotNotifyDiscardedValues() throws Exception {
		var directory = saved();
		var current = new Loaded(directory);
		current.engine.setSource(page.toString(), "published");
		current.engine.saveSources();
		Flow.projectUnloaded(current.project);
		assertTrue(FlowEngine.takeDiscardedSourcePaths(current.project).isEmpty());
		assertEquals("published", Files.readString(page));
	}
}
