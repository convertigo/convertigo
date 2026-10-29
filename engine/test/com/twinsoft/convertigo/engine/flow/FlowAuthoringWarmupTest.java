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
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;

/**
 * The preparation in the background of the authoring responses: from the first listing of the Studio only, the trees
 * of the projects in order, the palettes of an opened project first, once per load; nothing while the Studio calls,
 * nothing when disabled or without the disk cache, and a stop between two responses.
 */
public class FlowAuthoringWarmupTest {
	private final List<String> prepared = Collections.synchronizedList(new ArrayList<>());
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
		FlowAuthoringWarmup.preparation = item -> {
			prepared.add((item.palettes() ? "palettes:" : "tree:") + item.project());
			return true;
		};
	}

	@After
	public void restore() throws Exception {
		FlowAuthoringWarmup.reset();
		FlowAuthoringWarmup.projects = FlowAuthoringWarmup.LOADED_PROJECTS;
		FlowAuthoringWarmup.ordering = FlowRuntimeWarmup::leastReferencedFirst;
		FlowAuthoringWarmup.preparation = FlowAuthoringWarmup.PREPARATION;
		FlowAuthoringDiskCache.use(null);
		EnginePropertiesManager.setProperty(PropertyName.JAVASCRIPT_AUTHORING_WARMUP,
				PropertyName.JAVASCRIPT_AUTHORING_WARMUP.getDefaultValue());
		FileUtils.deleteDirectory(folder.toFile());
	}

	@Test
	public void theTreesOfTheProjectsArePreparedOnceFromTheFirstListingOfTheStudio() throws Exception {
		FlowAuthoringWarmup.studioOpened();
		FlowAuthoringWarmup.studioOpened();
		assertTrue(FlowAuthoringWarmup.awaitIdle(30, TimeUnit.SECONDS));
		assertEquals(List.of("tree:app", "tree:lib", "tree:other"), prepared);
		assertEquals(3, FlowAuthoringWarmup.counts()[0]);
	}

	@Test
	public void thePalettesOfAnOpenedProjectComeFirstOncePerLoad() throws Exception {
		var gate = new CountDownLatch(1);
		var first = new CountDownLatch(1);
		FlowAuthoringWarmup.preparation = item -> {
			first.countDown();
			gate.await(30, TimeUnit.SECONDS);
			prepared.add((item.palettes() ? "palettes:" : "tree:") + item.project());
			return true;
		};
		FlowAuthoringWarmup.studioOpened();
		assertTrue(first.await(30, TimeUnit.SECONDS));
		FlowAuthoringWarmup.projectOpened("other");
		FlowAuthoringWarmup.projectOpened("other");
		gate.countDown();
		assertTrue(FlowAuthoringWarmup.awaitIdle(30, TimeUnit.SECONDS));
		assertEquals(List.of("tree:app", "palettes:other", "tree:lib", "tree:other"), prepared);
		// loaded again: its palettes when it is opened again, and the trees checked again
		prepared.clear();
		FlowAuthoringWarmup.projectLoaded("other");
		FlowAuthoringWarmup.projectOpened("other");
		assertTrue(FlowAuthoringWarmup.awaitIdle(30, TimeUnit.SECONDS));
		assertEquals(List.of("palettes:other", "tree:app", "tree:lib", "tree:other"), prepared);
	}

	@Test
	public void nothingIsPreparedWhileTheStudioCalls() throws Exception {
		FlowAuthoringWarmup.studioCall(true);
		try {
			FlowAuthoringWarmup.studioOpened();
			Thread.sleep(FlowAuthoringWarmup.QUIET_MILLIS * 2);
			assertEquals("a Studio call in progress", List.of(), prepared);
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
		} finally {
			EnginePropertiesManager.setProperty(PropertyName.JAVASCRIPT_AUTHORING_CACHE,
					PropertyName.JAVASCRIPT_AUTHORING_CACHE.getDefaultValue());
		}
	}

	@Test
	public void aStopEndsThePreparationBetweenTwoResponses() throws Exception {
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
