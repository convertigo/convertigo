package com.twinsoft.convertigo.engine.flow;

import static org.junit.Assert.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.engine.DatabaseObjectsManager;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;

public class FlowProjectLoadBoundaryTest {
	@Rule public TemporaryFolder folder = new TemporaryFolder();
	private org.apache.log4j.Logger savedBeansLogger;
	private org.apache.log4j.Logger savedEngineLogger;
	private org.apache.log4j.Logger savedDatabaseObjectsLogger;
	private Engine savedEngine;
	private String savedProjectsPath;
	private final Map<String, File> descriptors = new HashMap<>();
	private final DatabaseObjectsManager manager = new DatabaseObjectsManager() {
		@Override public Object getCompiledValue(Object value) { return value; }
		@Override public StudioProjects getStudioProjects() {
			return new StudioProjects() {
				@Override public File getProject(String name) { return descriptors.get(name); }
				@Override public Map<String, File> getProjects(boolean openable) { return Map.copyOf(descriptors); }
				@Override public void reloadProject(String name) { fail("No reload during preflight"); }
				@Override public void renameProject(String oldName, String newName) { fail("No rename during preflight"); }
			};
		}
		@Override public Project getOriginalProjectByName(String name, boolean openable) {
			throw new AssertionError("No recursive project import during preflight: " + name);
		}
	};

	@Before public void initializeEngine() {
		savedBeansLogger = Engine.logBeans;
		Engine.logBeans = org.apache.log4j.Logger.getLogger(getClass());
		savedEngineLogger = Engine.logEngine;
		Engine.logEngine = Engine.logBeans;
		savedDatabaseObjectsLogger = Engine.logDatabaseObjectManager;
		Engine.logDatabaseObjectManager = Engine.logBeans;
		savedEngine = Engine.theApp;
		savedProjectsPath = Engine.PROJECTS_PATH;
		Engine.theApp = new Engine();
		Engine.theApp.databaseObjectsManager = manager;
	}

	@After public void restoreEngine() {
		Engine.logBeans = savedBeansLogger;
		Engine.logEngine = savedEngineLogger;
		Engine.logDatabaseObjectManager = savedDatabaseObjectsLogger;
		Engine.theApp = savedEngine;
		Engine.PROJECTS_PATH = savedProjectsPath;
	}

	@Test public void canonicalSourcesAreAdmittedWithoutMigrationReceiptOrStartupHost() throws Exception {
		var descriptor = project("Canonical");
		write(descriptor, "_flow/flows/Test.flow.js", "export default {};\n");

		FlowProjectLoadBoundary.beforeImport(descriptor, manager);
		FlowProjectLoadBoundary.checkLocalAdmission(descriptor.getParentFile().toPath(), FlowSourceLayout.FLOW);
		assertEquals("Canonical", DatabaseObjectsManager.getProjectName(descriptor));
	}

	@Test public void canonicalSourcesAreStillRefusedByTheLegacyRuntime() throws Exception {
		var descriptor = project("Canonical");
		write(descriptor, "_flow/flows/Test.flow.js", "export default {};\n");

		expectCode("FLOW_PROJECT_CANONICAL_RUNTIME_REQUIRED", () ->
				FlowProjectLoadBoundary.checkLocalAdmission(descriptor.getParentFile().toPath(), FlowSourceLayout.LEGACY));
	}

	@Test public void pendingLegacyMixedAndSourceLessFlowProjectsRemainBlocked() throws Exception {
		var pending = project("Pending");
		write(pending, FlowProjectLoadBoundary.PENDING_FILE, "pending");
		expectCode("FLOW_PROJECT_RECOVERY_REQUIRED", () -> FlowProjectLoadBoundary.beforeImport(pending, manager));

		var legacy = project("Legacy");
		write(legacy, "libs/flows/Test.flow.js", "legacy");
		expectCode("FLOW_PROJECT_LEGACY_RUNTIME_REQUIRED", () -> FlowProjectLoadBoundary.beforeImport(legacy, manager));

		var mixed = project("Mixed");
		write(mixed, "_flow/flows/Test.flow.js", "canonical");
		write(mixed, "libs/flows/Test.flow.js", "legacy");
		expectCode("FLOW_PROJECT_MIXED_LAYOUT", () -> FlowProjectLoadBoundary.beforeImport(mixed, manager));

		var sourceLess = project("SourceLess");
		expectCode("FLOW_PROJECT_SOURCE_REQUIRED", () -> FlowProjectLoadBoundary.beforeImport(sourceLess, manager));
	}

	@Test public void unsafeMarkersAndDanglingCanonicalRootsAreNotTreatedAsAbsent() throws Exception {
		var descriptor = project("Unsafe");
		var root = descriptor.toPath().getParent();
		Files.createSymbolicLink(root.resolve("_private"), Path.of("missing"));
		expectCode("FLOW_PROJECT_UNSAFE_PATH", () -> FlowProjectLoadBoundary.beforeImport(descriptor, manager));
		Files.delete(root.resolve("_private"));
		Files.createSymbolicLink(root.resolve("_flow"), Path.of("missing"));
		expectCode("FLOW_PROJECT_UNSAFE_PATH", () -> FlowProjectLoadBoundary.beforeImport(descriptor, manager));
	}

	@Test public void pendingReferencesBlockTheirParentsBeforeProjectLoading() throws Exception {
		var dependency = project("Dependency");
		var parent = project("Parent", "Dependency");
		write(parent, "_flow/flows/Parent.flow.js", "canonical");
		write(dependency, FlowProjectLoadBoundary.PENDING_FILE, "pending");

		expectCode("FLOW_PROJECT_RECOVERY_REQUIRED", () -> FlowProjectLoadBoundary.beforeImport(parent, manager));
	}

	@Test public void registeredProjectRootLinksResolveButSourceRootLinksAreRefused() throws Exception {
		var descriptor = project("Linked");
		write(descriptor, "_flow/flows/Test.flow.js", "canonical");
		var alias = folder.getRoot().toPath().resolve("registered-link");
		Files.createSymbolicLink(alias, descriptor.getParentFile().toPath());
		FlowProjectLoadBoundary.beforeImport(alias.resolve("c8oProject.yaml").toFile(), manager);
		Files.delete(descriptor.toPath().getParent().resolve("_flow/flows/Test.flow.js"));
		Files.delete(descriptor.toPath().getParent().resolve("_flow/flows"));
		Files.delete(descriptor.toPath().getParent().resolve("_flow"));
		Files.createSymbolicLink(descriptor.toPath().getParent().resolve("_flow"), Path.of("missing"));
		expectCode("FLOW_PROJECT_UNSAFE_PATH", () -> FlowProjectLoadBoundary.beforeImport(descriptor, manager));
	}

	@Test public void ordinaryProjectsRemainLoadableWithoutMigrationTooling() throws Exception {
		var ordinary = project("Ordinary");

		FlowProjectLoadBoundary.beforeImport(ordinary, manager);
	}

	private File project(String name, String... references) throws Exception {
		var root = folder.newFolder(name).toPath();
		var text = new StringBuilder("↑convertigo: 8.5.0.m006\n↓" + name + " [core.Project]: \n");
		for (var index = 0; index < references.length; index++) {
			text.append("  ↓ref").append(index).append(" [references.ProjectSchemaReference]: \n    projectName: ")
					.append(references[index]).append('\n');
		}
		if (!name.equals("Ordinary")) text.append("  ↓engine [flow.FlowEngine]: \n");
		var descriptor = root.resolve("c8oProject.yaml");
		Files.writeString(descriptor, text);
		descriptors.put(name, descriptor.toFile());
		return descriptor.toFile();
	}

	private void write(File project, String relative, String content) throws Exception {
		var path = project.toPath().getParent().resolve(relative);
		Files.createDirectories(path.getParent());
		Files.writeString(path, content);
	}

	private void expectCode(String code, org.junit.function.ThrowingRunnable operation) {
		var error = assertThrows(EngineException.class, operation);
		assertTrue(error.toString(), error.getMessage().startsWith(code));
	}
}
