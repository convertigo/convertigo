/* Copyright (c) 2001-2026 Convertigo SA.
 * Licensed under the GNU Affero General Public License, version 3 or later.
 */
package com.twinsoft.convertigo.engine.flow;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;

/** When and in which order the runtimes of the projects are prepared, and how the preparation stops. */
public class FlowRuntimeWarmupTest {
	private final List<String> prepared = Collections.synchronizedList(new ArrayList<>());
	private final List<String> asked = Collections.synchronizedList(new ArrayList<>());
	private final Map<String, Collection<String>> using = new ConcurrentHashMap<>();
	private final Map<String, FlowRuntimeWarmup.ProjectPass> behaviours = new ConcurrentHashMap<>();
	private FlowRuntimeWarmup.ProjectPass previousPass;
	private Function<String, Collection<String>> previousDependents;
	private Function<List<String>, List<String>> previousOrdering;

	@Before
	public void install() throws Exception {
		Engine.logEngine = org.apache.log4j.Logger.getLogger("runtime-warmup-test");
		EnginePropertiesManager.initProperties();
		EnginePropertiesManager.setProperty(PropertyName.JAVASCRIPT_RUNTIME_WARMUP, "auto");
		FlowRuntimeWarmup.stop();
		previousPass = FlowRuntimeWarmup.pass;
		previousDependents = FlowRuntimeWarmup.dependents;
		previousOrdering = FlowRuntimeWarmup.ordering;
		FlowRuntimeWarmup.ordering = names -> names;
		FlowRuntimeWarmup.pass = (name, cancellation) -> {
			var behaviour = behaviours.get(name);
			try {
				return behaviour != null && behaviour.prepare(name, cancellation);
			} finally {
				prepared.add(name);
			}
		};
		FlowRuntimeWarmup.dependents = name -> {
			asked.add(name);
			return using.getOrDefault(name, List.of());
		};
	}

	@After
	public void restore() {
		FlowRuntimeWarmup.stop();
		FlowRuntimeWarmup.pass = previousPass;
		FlowRuntimeWarmup.dependents = previousDependents;
		FlowRuntimeWarmup.ordering = previousOrdering;
		EnginePropertiesManager.setProperty(PropertyName.JAVASCRIPT_RUNTIME_WARMUP,
				PropertyName.JAVASCRIPT_RUNTIME_WARMUP.getDefaultValue());
	}

	private static Project project(String name) throws Exception {
		var project = new Project();
		project.setName(name);
		return project;
	}

	private static void idle() throws Exception {
		assertTrue("The preparation did not end", FlowRuntimeWarmup.awaitIdle(20, TimeUnit.SECONDS));
	}

	@Test
	public void theProjectsOfAStartArePreparedOnceAllAreLoadedInTheirOrder() throws Exception {
		FlowRuntimeWarmup.projectLoaded(project("lib"));
		FlowRuntimeWarmup.projectLoaded(project("app"));
		FlowRuntimeWarmup.projectLoaded(project("lib"));
		Thread.sleep(200);
		assertEquals("Nothing is prepared while the projects of the start load", List.of(), prepared);
		FlowRuntimeWarmup.projectsLoaded();
		idle();
		assertEquals(List.of("lib", "app"), prepared);
		// every project comes at the start: those using a project are not looked for
		assertEquals(List.of(), asked);
	}

	@Test
	public void aProjectLoadedAgainIsPreparedThenTheProjectsUsingIt() throws Exception {
		FlowRuntimeWarmup.projectsLoaded();
		using.put("lib", List.of("app", "lib", "other"));
		FlowRuntimeWarmup.projectLoaded(project("lib"));
		idle();
		assertEquals(List.of("lib", "app", "other"), prepared);
		assertEquals("Only the project loaded again has its users looked for", List.of("lib"), asked);
	}

	@Test
	public void theProjectsReferencedByTheFewestComeFirstAtTheStartAndAfterALoad() throws Exception {
		var referencing = Map.of("provider", 3, "lib", 1);
		FlowRuntimeWarmup.ordering = names -> {
			var ordered = new ArrayList<>(names);
			ordered.sort(java.util.Comparator.comparingInt(name -> referencing.getOrDefault(name, 0)));
			return ordered;
		};
		FlowRuntimeWarmup.projectLoaded(project("provider"));
		FlowRuntimeWarmup.projectLoaded(project("lib"));
		FlowRuntimeWarmup.projectLoaded(project("app"));
		FlowRuntimeWarmup.projectLoaded(project("other"));
		FlowRuntimeWarmup.projectsLoaded();
		idle();
		assertEquals(List.of("app", "other", "lib", "provider"), prepared);
		prepared.clear();
		using.put("provider", List.of("lib", "app"));
		FlowRuntimeWarmup.projectLoaded(project("provider"));
		idle();
		assertEquals("The provider loaded again: the projects using it first, itself last", List.of("app", "lib", "provider"), prepared);
		assertEquals(List.of("provider"), asked);
	}

	@Test
	public void aProjectLoadedSeveralTimesWhilePendingIsPreparedOnce() throws Exception {
		FlowRuntimeWarmup.projectsLoaded();
		var started = new CountDownLatch(1);
		var release = new CountDownLatch(1);
		behaviours.put("slow", (name, cancellation) -> { started.countDown(); release.await(); return false; });
		FlowRuntimeWarmup.projectLoaded(project("slow"));
		assertTrue(started.await(10, TimeUnit.SECONDS));
		assertEquals("slow", FlowRuntimeWarmup.current());
		for (int i = 0; i < 3; i++) {
			FlowRuntimeWarmup.projectLoaded(project("app"));
		}
		release.countDown();
		idle();
		assertEquals(List.of("slow", "app"), prepared);
	}

	@Test
	public void aProjectRequestedBeforeItsPreparationIsPreparedFirst() throws Exception {
		FlowRuntimeWarmup.projectLoaded(project("a"));
		FlowRuntimeWarmup.projectLoaded(project("b"));
		FlowRuntimeWarmup.projectLoaded(project("c"));
		FlowRuntimeWarmup.requested(project("c"));
		FlowRuntimeWarmup.requested(project("notLoaded"));
		FlowRuntimeWarmup.projectsLoaded();
		idle();
		assertEquals(List.of("c", "a", "b"), prepared);
	}

	@Test
	public void theProjectInProgressGivesWayToAProjectRequestedThenGoesOn() throws Exception {
		var started = new CountDownLatch(1);
		var passes = new java.util.concurrent.atomic.AtomicInteger();
		var firstPassFlows = new java.util.concurrent.atomic.AtomicInteger();
		behaviours.put("long", (name, cancellation) -> {
			var pass = passes.incrementAndGet();
			started.countDown();
			for (int flow = 1; flow <= 200; flow++) {
				Thread.sleep(5);
				if (pass == 1) {
					firstPassFlows.set(flow);
				}
				if (cancellation.givingWay()) {
					return false;
				}
			}
			return false;
		});
		FlowRuntimeWarmup.projectLoaded(project("long"));
		FlowRuntimeWarmup.projectLoaded(project("other"));
		FlowRuntimeWarmup.projectLoaded(project("requested"));
		FlowRuntimeWarmup.projectsLoaded();
		assertTrue(started.await(10, TimeUnit.SECONDS));
		// the project in progress, or a project not waiting: nothing changes
		FlowRuntimeWarmup.requested(project("long"));
		FlowRuntimeWarmup.requested(project("notLoaded"));
		Thread.sleep(50);
		FlowRuntimeWarmup.requested(project("requested"));
		idle();
		assertEquals(List.of("long", "requested", "long", "other"), prepared);
		assertTrue("The project in progress did not give way after its current Flow: " + firstPassFlows, firstPassFlows.get() < 200);
		assertEquals(2, passes.get());
	}

	@Test
	public void aFailingProjectIsSkippedAndTheNextOnesArePrepared() throws Exception {
		behaviours.put("broken", (name, cancellation) -> { throw new IllegalStateException("cannot prepare"); });
		behaviours.put("worse", (name, cancellation) -> { throw new LinkageError("missing class"); });
		FlowRuntimeWarmup.projectLoaded(project("broken"));
		FlowRuntimeWarmup.projectLoaded(project("worse"));
		FlowRuntimeWarmup.projectLoaded(project("app"));
		FlowRuntimeWarmup.projectsLoaded();
		idle();
		assertEquals(List.of("broken", "worse", "app"), prepared);
	}

	@Test
	public void aProjectWhoseRuntimeChangedDuringItsPreparationIsPreparedAgainOnce() throws Exception {
		behaviours.put("moving", (name, cancellation) -> true);
		FlowRuntimeWarmup.projectsLoaded();
		FlowRuntimeWarmup.projectLoaded(project("moving"));
		idle();
		assertEquals(List.of("moving", "moving"), prepared);
		// loaded again: prepared again, once more at most
		FlowRuntimeWarmup.projectLoaded(project("moving"));
		idle();
		assertEquals(List.of("moving", "moving", "moving", "moving"), prepared);
	}

	@Test
	public void stoppingTheEngineStopsThePreparationBetweenTwoFlows() throws Exception {
		var started = new CountDownLatch(1);
		var stoppedBetweenFlows = new AtomicBoolean();
		var worker = new AtomicReference<Thread>();
		behaviours.put("long", (name, cancellation) -> {
			worker.set(Thread.currentThread());
			started.countDown();
			// a preparation of many Flows, checking between two of them
			for (int flow = 0; flow < 2_000; flow++) {
				if (cancellation.cancelled()) {
					stoppedBetweenFlows.set(true);
					return false;
				}
				try { Thread.sleep(5); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
			}
			return false;
		});
		FlowRuntimeWarmup.projectLoaded(project("long"));
		FlowRuntimeWarmup.projectLoaded(project("next"));
		FlowRuntimeWarmup.projectsLoaded();
		assertTrue(started.await(10, TimeUnit.SECONDS));
		assertEquals(Thread.MIN_PRIORITY, worker.get().getPriority());
		assertTrue(worker.get().isDaemon());
		FlowRuntimeWarmup.stop();
		worker.get().join(5_000);
		assertFalse("The preparation thread is still running", worker.get().isAlive());
		assertTrue(stoppedBetweenFlows.get());
		assertEquals("A project pending at the stop is not prepared", List.of("long"), prepared);
		// the next start prepares nothing before its projects are loaded
		FlowRuntimeWarmup.projectLoaded(project("again"));
		Thread.sleep(200);
		assertEquals(List.of("long"), prepared);
		FlowRuntimeWarmup.projectsLoaded();
		idle();
		assertEquals(List.of("long", "again"), prepared);
	}

	@Test
	public void offPreparesNothingAndStopsAPreparationInProgress() throws Exception {
		EnginePropertiesManager.setProperty(PropertyName.JAVASCRIPT_RUNTIME_WARMUP, "off");
		assertFalse(FlowRuntimeWarmup.enabled());
		FlowRuntimeWarmup.projectLoaded(project("app"));
		FlowRuntimeWarmup.projectsLoaded();
		idle();
		Thread.sleep(100);
		assertEquals(List.of(), prepared);

		EnginePropertiesManager.setProperty(PropertyName.JAVASCRIPT_RUNTIME_WARMUP, "on");
		var started = new CountDownLatch(1);
		var sawOff = new CountDownLatch(1);
		behaviours.put("first", (name, cancellation) -> {
			started.countDown();
			for (long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10); System.nanoTime() < deadline;) {
				if (cancellation.cancelled()) { sawOff.countDown(); return false; }
				Thread.sleep(5);
			}
			return false;
		});
		FlowRuntimeWarmup.projectLoaded(project("first"));
		FlowRuntimeWarmup.projectLoaded(project("second"));
		assertTrue(started.await(10, TimeUnit.SECONDS));
		EnginePropertiesManager.setProperty(PropertyName.JAVASCRIPT_RUNTIME_WARMUP, "off");
		assertTrue("A preparation in progress sees the property turned off", sawOff.await(10, TimeUnit.SECONDS));
		idle();
		assertEquals(List.of("first"), prepared);
	}

	@Test
	public void autoPreparesOnAServerOnly() {
		EnginePropertiesManager.setProperty(PropertyName.JAVASCRIPT_RUNTIME_WARMUP, "auto");
		assertEquals(!Engine.isStudioMode(), FlowRuntimeWarmup.enabled());
		EnginePropertiesManager.setProperty(PropertyName.JAVASCRIPT_RUNTIME_WARMUP, "on");
		assertTrue(FlowRuntimeWarmup.enabled());
		EnginePropertiesManager.setProperty(PropertyName.JAVASCRIPT_RUNTIME_WARMUP, "off");
		assertFalse(FlowRuntimeWarmup.enabled());
		assertEquals("auto", PropertyName.JAVASCRIPT_RUNTIME_WARMUP.getDefaultValue());
	}

	@Test
	public void aPreloadOfPlansRunsInTheSharedRuntimeOfTheRuns() throws Exception {
		assertTrue(FlowEngineBridge.sharesRuntime("run", new JSONObject()));
		assertTrue(FlowEngineBridge.sharesRuntime("preload", new JSONObject().put("flows", new org.codehaus.jettison.json.JSONArray())));
		assertFalse("A preload of the catalog keeps its runtime", FlowEngineBridge.sharesRuntime("preload", new JSONObject()));
		assertTrue(FlowEngineBridge.sharesRuntime("syncInputs", new JSONObject().put("metadataOnly", true)));
		assertFalse(FlowEngineBridge.sharesRuntime("syncInputs", new JSONObject()));
		assertFalse(FlowEngineBridge.sharesRuntime("prepare", new JSONObject()));
	}

	@Test
	public void theOutcomeOfAPreloadTellsPreparedFailedOrNotSupported() throws Exception {
		assertEquals("", FlowRuntimeWarmup.planOutcome(new JSONObject("{ok:true,plans:[{flowQName:'P.F',ok:true}]}")));
		assertEquals("FLOW_SYNTAX: Unexpected token", FlowRuntimeWarmup.planOutcome(new JSONObject(
				"{ok:true,plans:[{flowQName:'P.F',ok:false,error:{code:'FLOW_SYNTAX',message:'Unexpected token'}}]}")));
		assertEquals("FLOW_ENGINE_ERROR: broken catalog", FlowRuntimeWarmup.planOutcome(new JSONObject(
				"{ok:false,error:{code:'FLOW_ENGINE_ERROR',message:'broken catalog'}}")));
		assertNull("A provider before plan preloads answers without plans",
				FlowRuntimeWarmup.planOutcome(new JSONObject("{ok:true,blockCount:12}")));
	}

	@Test
	public void aProjectBeingDeployedIsPreparedAfterItsImportOnly() throws Exception {
		FlowRuntimeWarmup.projectsLoaded();
		using.put("lib", List.of("app", "deployed"));
		FlowRuntimeWarmup.projectDeploying("deployed");
		FlowRuntimeWarmup.projectLoaded(project("lib"));
		idle();
		assertEquals("Not prepared while its files are replaced", List.of("lib", "app"), prepared);
		FlowRuntimeWarmup.projectLoaded(project("deployed"));
		idle();
		assertEquals("Prepared after its import", List.of("lib", "app", "deployed"), prepared);
		// its deployment ended with its import
		FlowRuntimeWarmup.projectDeployed("deployed");
		idle();
		assertEquals(List.of("lib", "app", "deployed"), prepared);
	}

	@Test
	public void theProjectsUsingAProjectWhoseDeploymentFailedArePrepared() throws Exception {
		FlowRuntimeWarmup.projectsLoaded();
		using.put("broken", List.of("app"));
		FlowRuntimeWarmup.projectDeploying("broken");
		FlowRuntimeWarmup.projectDeployed("broken");
		idle();
		assertEquals(List.of("broken", "app"), prepared);
	}

	/** A loaded project of the actual pass, with these Flows, referencing these projects. */
	private static Project loadedProject(Map<String, Project> loaded, java.io.File directory, String name, List<String> references,
			java.util.function.Supplier<String> source) throws Exception {
		var list = new ArrayList<com.twinsoft.convertigo.beans.core.Reference>();
		for (var referenced : references) {
			var reference = new com.twinsoft.convertigo.beans.references.ProjectSchemaReference();
			reference.setProjectName(referenced);
			list.add(reference);
		}
		var loader = new ClassLoader(FlowRuntimeWarmupTest.class.getClassLoader()) { };
		var holder = new AtomicReference<Project>();
		var flows = new ArrayList<com.twinsoft.convertigo.beans.core.Sequence>();
		var project = new Project() {
			@Override public String getDirPath() { return directory.getAbsolutePath(); }
			@Override public java.io.File getDirFile() { return directory; }
			@Override public ClassLoader getProjectClassLoader() { return loader; }
			@Override public List<java.io.File> getLibrariesDirectories() { return List.of(); }
			@Override public List<com.twinsoft.convertigo.beans.core.Reference> getReferenceList() { return list; }
			@Override public List<com.twinsoft.convertigo.beans.core.Sequence> getSequencesList() { return flows; }
		};
		project.setName(name);
		holder.set(project);
		if (source != null) {
			var flow = new com.twinsoft.convertigo.beans.flow.Flow() {
				@Override public Project getProject() { return holder.get(); }
				@Override public String getQName() { return name + ".Check"; }
				@Override public String getFlowSource() { return source.get(); }
			};
			flow.setName("Check");
			flows.add(flow);
		}
		loaded.put(name, project);
		return project;
	}

	@Test
	public void theActualPassPutsOffAProjectWhileItOrAProjectItUsesIsDeployedWithoutWarnings() throws Exception {
		var warnings = Collections.synchronizedList(new ArrayList<String>());
		var appender = new org.apache.log4j.AppenderSkeleton() {
			@Override protected void append(org.apache.log4j.spi.LoggingEvent event) {
				if (event.getLevel().isGreaterOrEqual(org.apache.log4j.Level.WARN)) warnings.add(String.valueOf(event.getMessage()));
			}
			@Override public void close() { }
			@Override public boolean requiresLayout() { return false; }
		};
		var previousApp = Engine.theApp;
		var workspace = java.nio.file.Files.createTempDirectory("runtime-warmup-deploy-");
		var loaded = new ConcurrentHashMap<String, Project>();
		var reads = new java.util.concurrent.atomic.AtomicInteger();
		var deployedDuringRead = new AtomicReference<String>();
		var actual = new AtomicBoolean(true);
		var real = previousPass;
		var level = Engine.logEngine.getLevel();
		Engine.logEngine.setLevel(org.apache.log4j.Level.WARN);
		Engine.logEngine.addAppender(appender);
		try {
			Engine.theApp = new Engine();
			Engine.theApp.databaseObjectsManager = new com.twinsoft.convertigo.engine.DatabaseObjectsManager() {
				@Override public Project getLoadedProjectByName(String name) { return name == null ? null : loaded.get(name); }
			};
			FlowRuntimeWarmup.pass = (name, cancellation) -> {
				prepared.add(name);
				return actual.get() && real.prepare(name, cancellation);
			};
			loadedProject(loaded, java.nio.file.Files.createDirectories(workspace.resolve("lib")).toFile(), "lib", List.of(), null);
			var app = loadedProject(loaded, java.nio.file.Files.createDirectories(workspace.resolve("app")).toFile(), "app", List.of("lib"), () -> {
				reads.incrementAndGet();
				var deployed = deployedDuringRead.get();
				if (deployed != null) {
					// its deployment starts: its files are removed
					FlowRuntimeWarmup.projectDeploying(deployed);
				}
				throw new java.io.UncheckedIOException(new java.nio.file.NoSuchFileException("app/_flow/blocks/native/sourceRequest.block.js"));
			});
			FlowRuntimeWarmup.projectsLoaded();
			// outside a deployment, a Flow that cannot be prepared is a warning
			FlowRuntimeWarmup.projectLoaded(app);
			idle();
			assertTrue("Its Flow was read", reads.get() > 0);
			assertTrue(warnings.toString(), !warnings.isEmpty() && warnings.get(0).contains("Unable to prepare the Flow app.Check"));
			warnings.clear();
			// the library it uses is being deployed: put off, none of its Flows read
			reads.set(0);
			FlowRuntimeWarmup.projectDeploying("lib");
			FlowRuntimeWarmup.projectLoaded(app);
			idle();
			assertEquals(0, reads.get());
			assertTrue(warnings.toString(), warnings.isEmpty());
			// the library imported, the project is prepared again, its own deployment starting meanwhile: no warning
			using.put("lib", List.of("app"));
			deployedDuringRead.set("app");
			prepared.clear();
			FlowRuntimeWarmup.projectLoaded(loaded.get("lib"));
			idle();
			assertEquals(List.of("lib", "app"), prepared);
			assertTrue(reads.get() > 0);
			assertTrue(warnings.toString(), warnings.isEmpty());
			// imported, it is prepared again
			actual.set(false);
			prepared.clear();
			FlowRuntimeWarmup.projectLoaded(app);
			idle();
			assertEquals(List.of("app"), prepared);
		} finally {
			Engine.logEngine.removeAppender(appender);
			Engine.logEngine.setLevel(level);
			Engine.theApp = previousApp;
			org.apache.commons.io.FileUtils.deleteDirectory(workspace.toFile());
		}
	}
}
