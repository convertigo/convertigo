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
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

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
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;

/**
 * The shared runtimes of a stateless server when a project is unloaded (deployed, reloaded, imported): those of the
 * project and of the loaded projects using it (referencing it, directly or not, or provided by it) are prepared again,
 * the other projects keep theirs, as their method responses and the snapshots of their files. Each key is prepared once
 * for its concurrent callers, different keys at the same time, and a runtime prepared while its project is invalidated
 * is never kept for its previous revision.
 */
public class FlowRuntimeInvalidationTest {
	private static final AtomicInteger preparations = new AtomicInteger();
	/** The preparations wait for each other on it while set. */
	private static volatile CyclicBarrier together;
	/** The next preparation waits for it, then fails when failing is set. */
	private static final AtomicReference<CountDownLatch> gate = new AtomicReference<>();
	private static final AtomicInteger atGate = new AtomicInteger();
	private static volatile boolean failing;
	/** Called once by the next preparation, on its thread. */
	private static volatile Callable<?> reentry;
	/** The invalidations started when a preparation started, by its number. */
	private static final Map<Integer, Integer> startedAfter = new ConcurrentHashMap<>();
	private static final AtomicInteger invalidationsStarted = new AtomicInteger();

	/** Called by each evaluation of the provider, the preparation of a shared runtime: its number. */
	public static int preparing() throws Exception {
		var id = preparations.incrementAndGet();
		startedAfter.put(id, invalidationsStarted.get());
		var barrier = together;
		if (barrier != null) {
			barrier.await(10, TimeUnit.SECONDS);
		}
		var held = gate.getAndSet(null);
		if (held != null) {
			atGate.incrementAndGet();
			if (!held.await(10, TimeUnit.SECONDS)) {
				throw new IllegalStateException("The gate of the preparation stayed closed");
			}
			if (failing) {
				throw new IllegalArgumentException("broken provider");
			}
		}
		var again = reentry;
		reentry = null;
		if (again != null) {
			again.call();
		}
		return id;
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

	private ExecutorService pool;

	@Before
	public void create() throws Exception {
		Engine.logEngine = Engine.logBeans = Engine.logContext = org.apache.log4j.Logger.getLogger("runtime-invalidation-test");
		EnginePropertiesManager.initProperties();
		preparations.set(0);
		together = null;
		gate.set(null);
		atGate.set(0);
		failing = false;
		reentry = null;
		startedAfter.clear();
		invalidationsStarted.set(0);
		pool = Executors.newCachedThreadPool();
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
		together = null;
		var held = gate.getAndSet(null);
		if (held != null) {
			held.countDown();
		}
		pool.shutdownNow();
		FlowRuntimeWarmup.stop();
		EnginePropertiesManager.setProperty(PropertyName.JAVASCRIPT_RUNTIME_WARMUP, PropertyName.JAVASCRIPT_RUNTIME_WARMUP.getDefaultValue());
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

	/** @return the gate the next preparation waits for */
	private static CountDownLatch hold() {
		atGate.set(0);
		var held = new CountDownLatch(1);
		gate.set(held);
		return held;
	}

	private static int waiters() throws Exception {
		return ((AtomicInteger) staticField("sharedRuntimeWaiters")).get();
	}

	/** Until the next preparation holds at the gate and count callers wait for preparations in progress. */
	private static void awaitHeld(int count) throws Exception {
		for (long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10); atGate.get() == 0 || waiters() < count;) {
			assertTrue("Expected a preparation held and " + count + " waiting callers, got " + atGate.get() + " and " + waiters(),
					System.nanoTime() < deadline);
			Thread.sleep(5);
		}
	}

	private Future<Object> later(Project project) {
		return pool.submit(() -> runtime(project));
	}

	private static Throwable cause(Future<?> future) throws Exception {
		try {
			future.get(20, TimeUnit.SECONDS);
		} catch (ExecutionException e) {
			var cause = e.getCause();
			while (cause instanceof java.lang.reflect.InvocationTargetException || cause instanceof org.mozilla.javascript.WrappedException) {
				cause = cause instanceof org.mozilla.javascript.WrappedException wrapped ? wrapped.getWrappedException() : cause.getCause();
			}
			return cause;
		}
		fail("Expected a failure");
		return null;
	}

	private static boolean kept(Object runtime) throws Exception {
		return ((Map<?, ?>) staticField("sharedRuntimes")).containsValue(runtime);
	}

	@Test
	public void theRuntimesOfDifferentProjectsArePreparedAtTheSameTime() throws Exception {
		int count = 4;
		// each preparation waits for all the others: a lock shared by the keys would time this barrier out
		together = new CyclicBarrier(count);
		var results = new ArrayList<Future<Object>>();
		for (int i = 0; i < count; i++) {
			results.add(later(project("Application" + i)));
		}
		var seen = new java.util.HashSet<Object>();
		for (var result : results) {
			assertTrue(seen.add(result.get(20, TimeUnit.SECONDS)));
		}
		assertEquals(count, preparations.get());
	}

	@Test
	public void theCallersOfAProjectWaitForItsSinglePreparationWhileAnotherIsPrepared() throws Exception {
		var application = project("Application");
		var other = project("Other");
		int count = 6;
		var held = hold();
		var results = new ArrayList<Future<Object>>();
		for (int i = 0; i < count; i++) {
			results.add(later(application));
		}
		awaitHeld(count - 1);
		assertNotNull("Another project is prepared meanwhile", later(other).get(5, TimeUnit.SECONDS));
		held.countDown();
		var first = results.get(0).get(20, TimeUnit.SECONDS);
		for (var result : results) {
			assertSame(first, result.get(20, TimeUnit.SECONDS));
		}
		assertEquals("One preparation for the project, one for the other", 2, preparations.get());
		assertSame(first, runtime(application));
		assertEquals(0, waiters());
		assertEquals(0, ((Map<?, ?>) staticField("sharedRuntimeFlights")).size());
	}

	@Test
	public void theCallersOfAFailedPreparationSeeItsFailureWhichIsNotKept() throws Exception {
		var application = project("Application");
		var held = hold();
		failing = true;
		var results = new ArrayList<Future<Object>>();
		for (int i = 0; i < 4; i++) {
			results.add(later(application));
		}
		awaitHeld(3);
		held.countDown();
		for (var result : results) {
			var failure = cause(result);
			assertTrue(String.valueOf(failure), failure instanceof IllegalArgumentException && "broken provider".equals(failure.getMessage()));
		}
		assertEquals("The callers waiting did not prepare it again", 1, preparations.get());
		failing = false;
		assertEquals(2, id(runtime(application)));
	}

	@Test
	public void aPreparationInterruptedIsPreparedAgainByACallerWaitingForIt() throws Exception {
		var application = project("Application");
		hold();
		var owner = new AtomicReference<Thread>();
		var first = pool.submit(() -> {
			owner.set(Thread.currentThread());
			return runtime(application);
		});
		awaitHeld(0);
		var waiting = later(application);
		awaitHeld(1);
		owner.get().interrupt();
		assertTrue(cause(first) instanceof InterruptedException);
		var runtime = waiting.get(20, TimeUnit.SECONDS);
		assertEquals("The caller waiting prepared it again", 2, id(runtime));
		assertSame(runtime, runtime(application));
	}

	@Test
	public void aPreparationNeedingItsOwnRuntimeOnItsThreadPreparesOneForItself() throws Exception {
		var application = project("Application");
		var nested = new AtomicReference<Object>();
		reentry = () -> {
			nested.set(runtime(application));
			return null;
		};
		var runtime = pool.submit(() -> runtime(application)).get(20, TimeUnit.SECONDS);
		assertNotSame(runtime, nested.get());
		assertSame("The outer preparation is published", runtime, runtime(application));
		assertFalse(kept(nested.get()));
	}

	@Test
	public void aRuntimePreparedWhileItsProjectIsInvalidatedIsNotKept() throws Exception {
		var library = project("Library");
		var application = project("Application", "Library");
		var unrelated = project("Unrelated");
		var held = hold();
		var preparing = later(application);
		awaitHeld(0);
		// the invalidation does not wait for the preparation
		pool.submit(() -> { reload(library); return null; }).get(5, TimeUnit.SECONDS);
		held.countDown();
		var stale = preparing.get(20, TimeUnit.SECONDS);
		assertFalse("Prepared for the previous revision of its project", kept(stale));
		var fresh = runtime(application);
		assertNotSame(stale, fresh);
		// the invalidation of another project during a preparation does not concern it
		var other = project("Other");
		held = hold();
		preparing = later(other);
		awaitHeld(0);
		pool.submit(() -> { reload(unrelated); return null; }).get(5, TimeUnit.SECONDS);
		held.countDown();
		var runtime = preparing.get(20, TimeUnit.SECONDS);
		assertTrue(kept(runtime));
		assertSame(runtime, runtime(other));
		assertSame(fresh, runtime(application));
	}

	@Test
	public void noCallAfterAnInvalidationGetsARuntimePreparedBeforeIt() throws Exception {
		project("Library");
		var application = project("Application", "Library");
		var library = loaded.get("Library");
		var completed = new AtomicInteger();
		var stop = new java.util.concurrent.atomic.AtomicBoolean();
		var failures = new java.util.concurrent.CopyOnWriteArrayList<String>();
		var callers = new ArrayList<Future<?>>();
		for (int i = 0; i < 4; i++) {
			callers.add(pool.submit(() -> {
				while (!stop.get()) {
					var before = completed.get();
					var id = id(runtime(application));
					// prepared after the start of every invalidation completed before this call
					if (startedAfter.get(id) < before) {
						failures.add("runtime " + id + " prepared after " + startedAfter.get(id) + " invalidations served after " + before);
					}
				}
				return null;
			}));
		}
		var invalidations = pool.submit(() -> {
			for (long end = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(1500); System.nanoTime() < end;) {
				invalidationsStarted.incrementAndGet();
				FlowEngineBridge.invalidateProjectDataCaches(library.getDirPath());
				completed.incrementAndGet();
				Thread.sleep(1);
			}
			return null;
		});
		try {
			invalidations.get(30, TimeUnit.SECONDS);
		} finally {
			stop.set(true);
		}
		for (var caller : callers) {
			caller.get(30, TimeUnit.SECONDS);
		}
		assertTrue(failures.toString(), failures.isEmpty());
		assertTrue("Runtimes prepared again: " + preparations.get(), preparations.get() > 1);
	}

	@Test
	public void aRuntimePreparedBetweenTheUnloadAndTheImportOfALibraryIsNotServedAfterTheImport() throws Exception {
		var library = project("Library");
		var application = project("Application", "Library");
		var unrelated = project("Unrelated");
		var other = runtime(unrelated);
		// the projects of the start are loaded, no preparation in the background
		EnginePropertiesManager.setProperty(PropertyName.JAVASCRIPT_RUNTIME_WARMUP, "off");
		FlowRuntimeWarmup.projectsLoaded();
		// deployed: unloaded, its files replaced, then imported
		loaded.remove("Library");
		Flow.projectUnloaded(library);
		var window = runtime(application);
		assertSame("Prepared from the files being replaced and kept", window, runtime(application));
		loaded.put("Library", library);
		FlowEngineBridge.projectLoaded(library);
		assertNotSame("Prepared again after the import", window, runtime(application));
		assertSame(other, runtime(unrelated));
	}

	@Test
	public void theLoadsOfTheStartInvalidateNothing() throws Exception {
		var library = project("Library");
		var application = project("Application", "Library");
		var runtime = runtime(application);
		var revision = FlowEngineBridge.metadataSourceRevision(application.getDirPath());
		FlowEngineBridge.projectLoaded(library);
		assertSame(runtime, runtime(application));
		assertEquals(revision, FlowEngineBridge.metadataSourceRevision(application.getDirPath()));
	}

	@Test
	public void savingAFlowForgetsTheSnapshotOfItsSourceFile() throws Exception {
		// the files of a project are under its directory file, as the source files of its Flows
		var directory = Files.createDirectories(workspace.resolve("Saved")).toFile();
		var project = new Project() {
			@Override public File getDirFile() { return directory; }
		};
		project.setName("Saved");
		var flow = flow(project);
		Files.createDirectories(flow.getFlowSourceFile().getParentFile().toPath());
		Files.writeString(flow.getFlowSourceFile().toPath(), "official");
		assertEquals("Read through the snapshots of a server", "official", flow.getFlowSource());
		flow.setFlowSource("saved");
		flow.saveFlowSourceFile();
		assertEquals("saved", flow(project).getFlowSource());
	}

	private static Flow flow(Project project) throws Exception {
		var flow = new Flow() {
			@Override public Project getProject() { return project; }
		};
		flow.setName("Proof");
		return flow;
	}
}
