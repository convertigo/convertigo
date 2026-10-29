/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */
package com.twinsoft.convertigo.engine.flow;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.FileUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.flow.FlowAuthoringWarmup.Item;

/**
 * The preparation in the background of the plans of the runtime providers: from the first listing of the Studio only,
 * the stage studio of the projects in order, the stage project of an opened project first, once per load; the items by
 * priority whatever their project, after the items they name, skipped when one of them failed; nothing while the
 * Studio calls, nothing when disabled or without the disk cache, and a stop between two requests. The plans are those
 * of a fake provider: what is prepared is never known by the engine.
 */
public class FlowAuthoringWarmupTest {
	private final List<String> prepared = Collections.synchronizedList(new ArrayList<>());
	private final List<String> planned = Collections.synchronizedList(new ArrayList<>());
	/** The plans of the fake provider, by project and stage. */
	private final Map<String, List<Item>> plans = new ConcurrentHashMap<>();
	private Path folder;

	@Before
	public void create() throws Exception {
		Engine.logEngine = org.apache.log4j.Logger.getLogger("authoring-warmup-test");
		EnginePropertiesManager.initProperties();
		EnginePropertiesManager.setProperty(PropertyName.JAVASCRIPT_AUTHORING_WARMUP, "on");
		folder = Files.createTempDirectory("authoring-warmup-");
		FlowAuthoringDiskCache.use(new FlowAuthoringDiskCache(folder, FlowAuthoringDiskCache.DEFAULT_MAX_BYTES,
				"warmup-test-installation-key-32b".getBytes(StandardCharsets.UTF_8)));
		FlowAuthoringWarmup.reset();
		FlowAuthoringWarmup.projects = () -> List.of("lib", "app", "other");
		FlowAuthoringWarmup.ordering = names -> {
			var ordered = new ArrayList<>(names);
			ordered.sort(String::compareTo);
			return ordered;
		};
		// by default: the tree at the stage studio, the palettes at the stage project
		for (var project : List.of("lib", "app", "other")) {
			plans.put(project + "|studio", List.of(item(project, "tree", 100)));
			plans.put(project + "|project", List.of(item(project, "palettes", 60)));
		}
		FlowAuthoringWarmup.planner = (project, stage) -> {
			planned.add(stage + ":" + project);
			return plans.getOrDefault(project + "|" + stage, List.of());
		};
		FlowAuthoringWarmup.preparation = item -> {
			prepared.add(item.id() + ":" + item.project());
			return true;
		};
	}

	private static Item item(String project, String id, int priority, String... after) {
		return new Item(project, id, "method-" + id, "", "{}", priority, List.of(after));
	}

	@After
	public void restore() throws Exception {
		FlowAuthoringWarmup.reset();
		FlowAuthoringWarmup.projects = FlowAuthoringWarmup.LOADED_PROJECTS;
		FlowAuthoringWarmup.ordering = FlowRuntimeWarmup::leastReferencedFirst;
		FlowAuthoringWarmup.planner = FlowAuthoringWarmup.PLANNER;
		FlowAuthoringWarmup.preparation = FlowAuthoringWarmup.PREPARATION;
		FlowAuthoringDiskCache.use(null);
		EnginePropertiesManager.setProperty(PropertyName.JAVASCRIPT_AUTHORING_WARMUP,
				PropertyName.JAVASCRIPT_AUTHORING_WARMUP.getDefaultValue());
		FileUtils.deleteDirectory(folder.toFile());
	}

	@Test
	public void theStageStudioOfTheProjectsIsPreparedOnceFromTheFirstListingOfTheStudio() throws Exception {
		FlowAuthoringWarmup.studioOpened();
		FlowAuthoringWarmup.studioOpened();
		assertTrue(FlowAuthoringWarmup.awaitIdle(30, TimeUnit.SECONDS));
		assertEquals(List.of("studio:app", "studio:lib", "studio:other"), planned);
		assertEquals(List.of("tree:app", "tree:lib", "tree:other"), prepared);
		assertEquals(3, FlowAuthoringWarmup.counts()[0]);
	}

	@Test
	public void theItemsRunByPriorityWhateverTheirProject() throws Exception {
		// what each provider asks after the trees of every project, as the frontend runtime of a project with frontends
		plans.put("app|studio", List.of(item("app", "tree", 100), item("app", "runtime", 10)));
		plans.put("other|studio", List.of(item("other", "runtime", 20), item("other", "tree", 100)));
		FlowAuthoringWarmup.studioOpened();
		assertTrue(FlowAuthoringWarmup.awaitIdle(30, TimeUnit.SECONDS));
		assertEquals(List.of("tree:app", "tree:lib", "tree:other", "runtime:other", "runtime:app"), prepared);
	}

	@Test
	public void theStageProjectOfAnOpenedProjectComesFirstOncePerLoad() throws Exception {
		var gate = new CountDownLatch(1);
		var first = new CountDownLatch(1);
		FlowAuthoringWarmup.preparation = item -> {
			first.countDown();
			gate.await(30, TimeUnit.SECONDS);
			prepared.add(item.id() + ":" + item.project());
			return true;
		};
		FlowAuthoringWarmup.studioOpened();
		assertTrue(first.await(30, TimeUnit.SECONDS));
		FlowAuthoringWarmup.projectOpened("other");
		FlowAuthoringWarmup.projectOpened("other");
		gate.countDown();
		assertTrue(FlowAuthoringWarmup.awaitIdle(30, TimeUnit.SECONDS));
		assertEquals(List.of("tree:app", "palettes:other", "tree:lib", "tree:other"), prepared);
		assertEquals(List.of("studio:app", "studio:lib", "studio:other", "project:other"), planned);
		// loaded again: its stage project when it is opened again, and the stage studio of the projects again
		prepared.clear();
		FlowAuthoringWarmup.projectLoaded("other");
		FlowAuthoringWarmup.projectOpened("other");
		assertTrue(FlowAuthoringWarmup.awaitIdle(30, TimeUnit.SECONDS));
		assertEquals(List.of("palettes:other", "tree:app", "tree:lib", "tree:other"), prepared);
	}

	@Test
	public void theLastOpenedProjectComesFirst() throws Exception {
		FlowAuthoringWarmup.studioCall(true);
		try {
			FlowAuthoringWarmup.studioOpened();
			FlowAuthoringWarmup.projectOpened("lib");
			FlowAuthoringWarmup.projectOpened("other");
		} finally {
			FlowAuthoringWarmup.studioCall(false);
		}
		assertTrue(FlowAuthoringWarmup.awaitIdle(30, TimeUnit.SECONDS));
		assertEquals(List.of("palettes:other", "palettes:lib", "tree:app", "tree:lib", "tree:other"), prepared);
	}

	@Test
	public void anItemRunsAfterThoseItNamesAndIsSkippedWhenOneFailed() throws Exception {
		FlowAuthoringWarmup.projects = () -> List.of("app");
		// "after" wins over a higher priority; a dependency failing skips its dependents, transitively
		plans.put("app|studio", List.of(item("app", "late", 90, "base"), item("app", "base", 10), item("app", "broken", 50),
				item("app", "dependent", 40, "broken"), item("app", "transitive", 30, "dependent"),
				item("app", "orphan", 80, "unknown")));
		FlowAuthoringWarmup.preparation = item -> {
			prepared.add(item.id());
			if (item.id().equals("broken")) {
				throw new IllegalStateException("failure of the provider");
			}
			return true;
		};
		FlowAuthoringWarmup.studioOpened();
		assertTrue(FlowAuthoringWarmup.awaitIdle(30, TimeUnit.SECONDS));
		assertEquals(List.of("broken", "base", "late"), prepared);
		assertEquals("only base and late computed", 2, FlowAuthoringWarmup.counts()[0]);
	}

	@Test
	public void theOrderOfAPlan() {
		var ordered = FlowAuthoringWarmup.ordered(List.of(item("p", "c", 5, "b"), item("p", "a", 5), item("p", "b", 9, "a"),
				item("p", "a", 99), item("p", "x", 1, "y"), item("p", "y", 1, "x")));
		var ids = new ArrayList<String>();
		ordered.forEach(item -> ids.add(item.id()));
		// a, then b (higher than c, after a), then c; the duplicate and the cycle dropped
		assertEquals(List.of("a", "b", "c"), ids);
	}

	@Test
	public void thePlanOfAProviderAndThePlanWithoutProvider() throws Exception {
		var answer = new JSONArray()
				.put(new JSONObject().put("id", "tree").put("method", "describeTree").put("priority", 100)
						.put("request", new JSONObject().put("includeBindings", false)))
				.put(new JSONObject().put("method", "authoringPalette").put("flow", "Hello").put("after", new JSONArray().put("tree")))
				.put(new JSONObject().put("id", "nothing"));
		var items = FlowAuthoringWarmup.items("app", answer);
		assertEquals(2, items.size());
		assertEquals(new Item("app", "tree", "describeTree", "", "{\"includeBindings\":false}", 100, List.of()), items.get(0));
		assertEquals(new Item("app", "item1", "authoringPalette", "Hello", "{}", 0, List.of("tree")), items.get(1));
		assertNull("not a plan", FlowAuthoringWarmup.items("app", null));
		// an older provider: the tree of the project, as the Studio asks it, then nothing
		var fallback = FlowAuthoringWarmup.fallback("app", FlowAuthoringWarmup.STUDIO);
		assertEquals(1, fallback.size());
		assertEquals("describeTree", fallback.get(0).method());
		assertNull("the request of the Studio itself", fallback.get(0).request());
		assertEquals(List.of(), FlowAuthoringWarmup.fallback("app", FlowAuthoringWarmup.PROJECT));
	}

	@Test
	public void aPlanThatCannotBeAskedPreparesNothingElse() throws Exception {
		var asked = new AtomicInteger();
		FlowAuthoringWarmup.planner = (project, stage) -> {
			if (asked.incrementAndGet() == 1) {
				throw new IllegalStateException("provider unavailable");
			}
			return plans.get(project + "|" + stage);
		};
		FlowAuthoringWarmup.studioOpened();
		assertTrue(FlowAuthoringWarmup.awaitIdle(30, TimeUnit.SECONDS));
		assertEquals(List.of("tree:lib", "tree:other"), prepared);
	}

	@Test
	public void nothingIsPreparedWhileTheStudioCalls() throws Exception {
		FlowAuthoringWarmup.studioCall(true);
		try {
			FlowAuthoringWarmup.studioOpened();
			Thread.sleep(FlowAuthoringWarmup.QUIET_MILLIS * 2);
			assertEquals("a Studio call in progress", List.of(), prepared);
			assertEquals("nor planned", List.of(), planned);
		} finally {
			FlowAuthoringWarmup.studioCall(false);
		}
		var ended = System.nanoTime();
		assertTrue(FlowAuthoringWarmup.awaitIdle(30, TimeUnit.SECONDS));
		assertEquals(3, prepared.size());
		assertTrue("prepared once the Studio was quiet",
				TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - ended) >= FlowAuthoringWarmup.QUIET_MILLIS - 50);
	}

	@Test
	public void nothingIsPreparedWhenDisabledOrWithoutDiskCache() throws Exception {
		EnginePropertiesManager.setProperty(PropertyName.JAVASCRIPT_AUTHORING_WARMUP, "off");
		FlowAuthoringWarmup.studioOpened();
		FlowAuthoringWarmup.projectOpened("app");
		EnginePropertiesManager.setProperty(PropertyName.JAVASCRIPT_AUTHORING_WARMUP, "on");
		EnginePropertiesManager.setProperty(PropertyName.JAVASCRIPT_AUTHORING_CACHE, "off");
		FlowAuthoringDiskCache.use(null);
		try {
			FlowAuthoringWarmup.studioOpened();
			assertTrue(FlowAuthoringWarmup.awaitIdle(5, TimeUnit.SECONDS));
			assertEquals(List.of(), prepared);
			assertEquals(List.of(), planned);
		} finally {
			EnginePropertiesManager.setProperty(PropertyName.JAVASCRIPT_AUTHORING_CACHE,
					PropertyName.JAVASCRIPT_AUTHORING_CACHE.getDefaultValue());
		}
	}

	@Test
	public void aStopEndsThePreparationBetweenTwoRequests() throws Exception {
		var gate = new CountDownLatch(1);
		var first = new CountDownLatch(1);
		FlowAuthoringWarmup.preparation = item -> {
			first.countDown();
			gate.await(30, TimeUnit.SECONDS);
			prepared.add(item.project());
			return true;
		};
		FlowAuthoringWarmup.studioOpened();
		assertTrue(first.await(30, TimeUnit.SECONDS));
		FlowAuthoringWarmup.stop();
		gate.countDown();
		Thread.sleep(FlowAuthoringWarmup.QUIET_MILLIS * 2);
		assertTrue("one response at most after the stop: " + prepared, prepared.size() <= 1);
	}
}
