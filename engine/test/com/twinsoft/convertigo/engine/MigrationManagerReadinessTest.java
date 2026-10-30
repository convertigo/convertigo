/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */

package com.twinsoft.convertigo.engine;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * The supervision reports convertigo.projects=KO until the projects of the workspace are loaded (#1263): a pod is
 * not ready for its readiness probe while the background jobs that load them have not all been added and finished.
 */
public class MigrationManagerReadinessTest {
	private Map<String, MigrationJob> jobs;

	@SuppressWarnings("unchecked")
	@Before
	public void reset() throws Exception {
		jobs = (Map<String, MigrationJob>) field("jobs").get(null);
		jobs.clear();
		field("jobsAdded").set(null, false);
	}

	@After
	public void clear() throws Exception {
		reset();
	}

	private static Field field(String name) throws Exception {
		Field field = MigrationManager.class.getDeclaredField(name);
		field.setAccessible(true);
		return field;
	}

	@Test
	public void projectsAreLoadingUntilTheirJobsAreAddedAndFinished() throws Exception {
		// before the migration thread adds its jobs (it waits for the engine to be started), the map is empty
		assertTrue(MigrationManager.isMigrationFinished());
		assertFalse(MigrationManager.areProjectsLoaded());

		MigrationJob first = new MigrationJob("first");
		MigrationJob second = new MigrationJob("second");
		jobs.put("first", first);
		jobs.put("second", second);
		field("jobsAdded").set(null, true);
		assertFalse(MigrationManager.areProjectsLoaded());

		first.isFinished = true;
		assertFalse(MigrationManager.areProjectsLoaded());
		second.isFinished = true;
		assertTrue(MigrationManager.areProjectsLoaded());
	}

	@Test
	public void theJobsCanBeReadWhileTheyAreAdded() throws Exception {
		AtomicReference<Throwable> failure = new AtomicReference<>();
		Thread reader = new Thread(() -> {
			try {
				for (int i = 0; i < 20000; i++) {
					MigrationManager.areProjectsLoaded();
				}
			} catch (Throwable t) {
				failure.set(t);
			}
		});
		reader.start();
		for (int i = 0; i < 20000; i++) {
			jobs.put("project" + i, new MigrationJob("project" + i));
		}
		reader.join();
		assertTrue(String.valueOf(failure.get()), failure.get() == null);
	}
}
