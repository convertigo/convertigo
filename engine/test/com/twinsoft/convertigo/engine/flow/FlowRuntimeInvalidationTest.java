/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine.flow;

import static org.junit.Assert.*;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.Reference;
import com.twinsoft.convertigo.beans.flow.Flow;
import com.twinsoft.convertigo.beans.flow.FlowEngine;
import com.twinsoft.convertigo.beans.references.ProjectSchemaReference;
import com.twinsoft.convertigo.engine.DatabaseObjectsManager;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;

/**
 * The shared runtimes of a stateless server when a project is unloaded (deployed, reloaded, imported): those of the
 * project and of the loaded projects using it (referencing it, directly or not, or provided by it) are prepared again,
 * the other projects keep theirs, as their method responses and the snapshots of their files.
 */
public class FlowRuntimeInvalidationTest {
	private static final AtomicInteger preparations = new AtomicInteger();

	/** Called by each evaluation of the provider, the preparation of a shared runtime: its number. */
	public static int preparing() {
		return preparations.incrementAndGet();
	}

	private static Method method(Class<?> c, String name, Class<?>... types) throws Exception {
		var method = c.getDeclaredMethod(name, types);
		method.setAccessible(true);
		return method;
	}

	private static Object field(Object object, String name) throws Exception {
		return method(object.getClass(), name).invoke(object);
	}

	private static Object staticField(String name) throws Exception {
		var field = FlowEngineBridge.class.getDeclaredField(name);
		field.setAccessible(true);
		return field.get(null);
	}

	private Engine previous;
	private Path workspace;
	private final Map<String, Project> loaded = new LinkedHashMap<>();
	private final ClassLoader generation = new ClassLoader(getClass().getClassLoader()) { };
	private File engineFile;
	private Object reference, source;
	private Method acquire;

	@Before
	public void create() throws Exception {
		Engine.logEngine = Engine.logBeans = Engine.logContext = org.apache.log4j.Logger.getLogger("runtime-invalidation-test");
		EnginePropertiesManager.initProperties();
		preparations.set(0);
		workspace = Files.createTempDirectory("runtime-invalidation-");
		var provider = project("Provider");
		engineFile = provider.getDirFile().toPath().resolve("_flow/Engine.js").toFile();
		Files.createDirectories(engineFile.getParentFile().toPath());
		Files.writeString(engineFile.toPath(), "// @convertigo-runtime: stateless-v1\n"
				+ "(function(){var id=Packages." + getClass().getName() + ".preparing();"
				+ "return {supportsStatelessExecution:true,id:id};})()");
		previous = Engine.theApp;
		Engine.theApp = new Engine();
		Engine.theApp.databaseObjectsManager = new DatabaseObjectsManager() {
			@Override public Project getLoadedProjectByName(String name) { return loaded.get(name); }
			@Override public Project getOriginalProjectByName(String name, boolean openable) { return loaded.get(name); }
			@Override public List<Project> getLoadedProjects() { return new ArrayList<>(loaded.values()); }
		};
		var bridge = FlowEngineBridge.class;
		var ref = Class.forName(bridge.getName() + "$EngineRef");
		var src = Class.forName(bridge.getName() + "$CachedEngineSource");
		reference = method(ref, "parse", String.class).invoke(null, "Provider.Engine");
		var constructor = src.getDeclaredConstructor(File.class, String.class, long.class, long.class);
		constructor.setAccessible(true);
		source = constructor.newInstance(engineFile, Files.readString(engineFile.toPath()), engineFile.lastModified(), engineFile.length());
		acquire = method(bridge, "sharedEngineRuntime", ref, File.class, src, Context.class, JSONObject.class,
				com.twinsoft.convertigo.engine.Context.class);
		FlowEngineBridge.clearCaches();
	}

	@After
	public void release() throws Exception {
		FlowEngineBridge.clearCaches();
		Engine.theApp = previous;
		if (workspace != null) {
			org.apache.commons.io.FileUtils.deleteDirectory(workspace.toFile());
		}
	}

	/** A loaded project whose runtime the provider gives, referencing these projects. */
	private Project project(String name, String... references) throws Exception {
		var directory = Files.createDirectories(workspace.resolve(name)).toFile();
		var list = new ArrayList<Reference>();
		for (var referenced : references) {
			var reference = new ProjectSchemaReference();
			reference.setProjectName(referenced);
			list.add(reference);
		}
		var project = new Project() {
			final FlowEngine flowEngine = new FlowEngine() {
				@Override public String getEngineQName() { return "Provider.Engine"; }
			};
			@Override public String getDirPath() { return directory.getAbsolutePath(); }
			@Override public File getDirFile() { return directory; }
			@Override public ClassLoader getProjectClassLoader() { return generation; }
			@Override public List<Reference> getReferenceList() { return list; }
			@Override public FlowEngine getFlowEngine() { return flowEngine; }
		};
		project.setName(name);
		loaded.put(name, project);
		return project;
	}

	private static JSONObject request(Project project) throws Exception {
		return new JSONObject().put("project", project.getName()).put("projectDir", project.getDirPath());
	}

	/** The shared runtime a call of the project runs in, as on a stateless server. */
	private Object runtime(Project project) throws Exception {
		try (var cx = Context.enter()) {
			return field(acquire.invoke(null, reference, engineFile, source, cx, request(project), null), "runtime");
		}
	}

	private static int id(Object runtime) throws Exception {
		return ((Number) ScriptableObject.getProperty((Scriptable) field(runtime, "engineObject"), "id")).intValue();
	}

	/** Unloaded, as the cache of the projects does before a deployment, then loaded again. */
	private void reload(Project project) {
		loaded.remove(project.getName());
		Flow.projectUnloaded(project);
		loaded.put(project.getName(), project);
	}

	@Test
	public void unloadingAProjectKeepsTheRuntimesOfTheProjectsNotUsingIt() throws Exception {
		var application = project("Application");
		var unrelated = project("Unrelated");
		var runtime = runtime(application);
		assertSame("A runtime is prepared once", runtime, runtime(application));
		var other = runtime(unrelated);
		var revision = FlowEngineBridge.metadataSourceRevision(application.getDirPath());
		var otherRevision = FlowEngineBridge.metadataSourceRevision(unrelated.getDirPath());
		var generation = FlowEngineBridge.dataGeneration();
		reload(unrelated);
		assertSame("The runtime of a project not using the unloaded one is kept", runtime, runtime(application));
		assertEquals(revision, FlowEngineBridge.metadataSourceRevision(application.getDirPath()));
		assertNotSame("The runtime of the unloaded project is prepared again", other, runtime(unrelated));
		assertNotEquals(otherRevision, FlowEngineBridge.metadataSourceRevision(unrelated.getDirPath()));
		// the pooled runtimes, which serve every project, still see each invalidation
		assertNotEquals(generation, FlowEngineBridge.dataGeneration());
		assertEquals(3, preparations.get());
	}

	@Test
	public void unloadingALibraryInvalidatesTheProjectsUsingItDirectlyOrNot() throws Exception {
		var library = project("Library");
		var application = project("Application", "Library");
		var portal = project("Portal", "Application");
		var unrelated = project("Unrelated");
		var runtimes = new LinkedHashMap<Project, Object>();
		for (var project : List.of(library, application, portal, unrelated)) {
			runtimes.put(project, runtime(project));
		}
		reload(library);
		for (var project : List.of(library, application, portal)) {
			assertNotSame(project.getName() + " uses the library", runtimes.get(project), runtime(project));
		}
		assertSame(runtimes.get(unrelated), runtime(unrelated));
		// a change saved in the library, known by its directory only, as a source mutation
		runtimes.replaceAll((project, runtime) -> {
			try { return runtime(project); } catch (Exception e) { throw new AssertionError(e); }
		});
		FlowEngineBridge.invalidateProjectDataCaches(library.getDirPath());
		assertNotSame(runtimes.get(portal), runtime(portal));
		assertSame(runtimes.get(unrelated), runtime(unrelated));
	}

	@Test
	public void unloadingTheProviderInvalidatesEveryProjectItProvides() throws Exception {
		var provider = loaded.get("Provider");
		var application = project("Application");
		var unrelated = project("Unrelated");
		var first = runtime(application);
		var second = runtime(unrelated);
		reload(provider);
		assertNotSame(first, runtime(application));
		assertNotSame(second, runtime(unrelated));
	}

	@Test
	public void aProjectReferencingAProjectNotLoadedIsInvalidatedWithAnyProject() throws Exception {
		// the project not loaded (being deployed, closed) may use the one unloaded: its users are invalidated as well
		var application = project("Application", "Missing");
		var unrelated = project("Unrelated");
		var kept = project("Kept");
		var runtime = runtime(application);
		var other = runtime(kept);
		reload(unrelated);
		assertNotSame(runtime, runtime(application));
		assertSame(other, runtime(kept));
	}

	@Test
	public void theMethodResponsesOfTheOtherProjectsAreKept() throws Exception {
		var application = project("Application", "Library");
		var library = project("Library");
		var unrelated = project("Unrelated");
		var store = method(FlowEngineBridge.class, "storeMethodResponse", String.class, reference.getClass(), String.class,
				JSONObject.class, JSONObject.class);
		var cached = method(FlowEngineBridge.class, "cachedMethodResponse", String.class);
		for (var project : List.of(application, library, unrelated)) {
			store.invoke(null, project.getName(), reference, "catalog", request(project), new JSONObject().put("ok", true));
		}
		store.invoke(null, "none", reference, "catalog", new JSONObject(), new JSONObject().put("ok", true));
		reload(library);
		assertNull(cached.invoke(null, "Library"));
		assertNull("The response of a project using it", cached.invoke(null, "Application"));
		assertNull("The response of no project, which may use any of them", cached.invoke(null, "none"));
		assertNotNull(cached.invoke(null, "Unrelated"));
	}

	@Test
	public void theSnapshotsOfTheFilesOfTheOtherProjectsAreKept() throws Exception {
		var application = project("Application", "Library");
		var library = project("Library");
		var files = new LinkedHashMap<Project, File>();
		for (var project : List.of(application, library)) {
			var file = new File(project.getDirFile(), "_flow/flows/Sample.flow.js");
			Files.createDirectories(file.getParentFile().toPath());
			Files.writeString(file.toPath(), "function Sample() { return 1; }\n");
			assertEquals("function Sample() { return 1; }\n", FlowEngineBridge.readSourceFile(file));
			files.put(project, file);
		}
		reload(library);
		var entries = staticField("serverSources").getClass().getDeclaredField("entries");
		entries.setAccessible(true);
		var keys = ((Map<?, ?>) entries.get(staticField("serverSources"))).keySet().toString();
		assertTrue(keys, keys.contains(files.get(application).toPath().toAbsolutePath().normalize().toString()));
		assertFalse(keys, keys.contains(files.get(library).getParentFile().getParentFile().getParent()));
	}
}
