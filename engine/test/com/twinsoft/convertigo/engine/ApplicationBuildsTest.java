/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU  Affero General Public
 * License  as published by  the Free Software Foundation;  either
 * version  3  of  the  License,  or  (at your option)  any  later
 * version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY;  without even the implied warranty of
 * MERCHANTABILITY  or  FITNESS  FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program;
 * if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.engine;

import static org.junit.Assert.*;

import java.io.File;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.engine.util.FileUtils;
import com.twinsoft.convertigo.engine.util.InstanceIdentity;

/** The lock between the instances that build an application, and the publication of a build: no engine is started. */
public class ApplicationBuildsTest {
	private File base;
	private File lock;

	@Before
	public void setUp() throws Exception {
		base = Files.createTempDirectory("application-builds-test").toFile().getCanonicalFile();
		lock = new File(base, "DisplayObjects/" + ApplicationBuilds.LOCK);
		ApplicationBuilds.instanceId = () -> "this-pod";
	}

	@After
	public void tearDown() {
		ApplicationBuilds.lockExpiry = TimeUnit.MINUTES.toMillis(5);
		ApplicationBuilds.frontendDelay = TimeUnit.SECONDS.toMillis(20);
		ApplicationBuilds.stop();
		ApplicationBuilds.instanceId = InstanceIdentity::getLocalInstanceId;
		FileUtils.deleteQuietly(base);
	}

	@Test
	public void theInstanceThatCreatesTheLockBuildsAndTheOthersDoNot() throws Exception {
		assertNull("this instance holds the lock", ApplicationBuilds.acquire(lock));
		assertEquals("this-pod", Files.readString(lock.toPath()));

		Files.writeString(lock.toPath(), "other-pod");
		assertEquals("another instance holds it", "other-pod", ApplicationBuilds.acquire(lock));
		ApplicationBuilds.release(lock);
		assertTrue("the lock of another instance is never released here", lock.exists());
	}

	@Test
	public void aLockNoLongerRenewedIsTakenOver() throws Exception {
		Files.createDirectories(lock.getParentFile().toPath());
		Files.writeString(lock.toPath(), "gone-pod");
		assertTrue(lock.setLastModified(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(10)));
		assertNull("taken over", ApplicationBuilds.acquire(lock));
		ApplicationBuilds.release(lock);
		assertFalse("released once built", lock.exists());
	}

	@Test
	public void aLockLeftByAFormerRunOfThisInstanceIsTakenOver() throws Exception {
		Files.createDirectories(lock.getParentFile().toPath());
		// a container restarted in the same pod keeps its identity
		Files.writeString(lock.toPath(), "this-pod");
		assertNull("a build interrupted by the restart, taken over at once", ApplicationBuilds.acquire(lock));
	}

	@Test
	public void aFailedBuildRestoresThePageDeliveredWithTheProjectAndKeepsItsAssets() throws Exception {
		var mobile = new File(base, "DisplayObjects/mobile");
		Files.createDirectories(new File(mobile, "assets").toPath());
		Files.writeString(new File(mobile, "assets/logo.svg").toPath(), "logo");
		Files.writeString(new File(mobile, "chunk-1.js").toPath(), "half written");
		ApplicationBuilds.restoreIndex(mobile, "unbuilt".getBytes());
		assertEquals("unbuilt", Files.readString(new File(mobile, "index.html").toPath()));
		assertEquals("logo", Files.readString(new File(mobile, "assets/logo.svg").toPath()));
		assertFalse("what the build wrote goes", new File(mobile, "chunk-1.js").exists());
	}

	@Test
	public void theStatusPageTellsTheStateOfTheBuildUntilItIsBuilt() throws Exception {
		var projectDir = new File(base, "App");
		assertNull("unknown: the files are served", ApplicationBuilds.statusPage("App", projectDir));
		var mobile = new File(projectDir, "DisplayObjects/mobile");
		Files.createDirectories(mobile.toPath());
		Files.writeString(new File(mobile, "index.html").toPath(), "This is an unbuilt application");
		try {
			ApplicationBuilds.setState("App", ApplicationBuilds.State.notAllowed);
			assertNotNull("not built by this server, without build: the page", ApplicationBuilds.statusPage("App", projectDir));
			Files.writeString(new File(mobile, "env.json").toPath(), "{\"appGenerationTime\": 1791450000000}");
			assertNull("built since, as by the Build of a Studio: the application", ApplicationBuilds.statusPage("App", projectDir));
			FileUtils.deleteQuietly(new File(mobile, "env.json"));

			ApplicationBuilds.setState("App", ApplicationBuilds.State.pending);
			assertNotNull("waiting, without build: the page", ApplicationBuilds.statusPage("App", projectDir));

			Files.createDirectories(new File(mobile, "_app/immutable").toPath());
			assertNotNull("_app alone is not a completed build", ApplicationBuilds.statusPage("App", projectDir));
			Files.writeString(new File(mobile, "env.json").toPath(), "{\"projectName\":\"App\"}");
			assertNotNull("env.json without the stamp is incomplete", ApplicationBuilds.statusPage("App", projectDir));

			FileUtils.deleteQuietly(new File(mobile, "_app"));
			Files.writeString(new File(mobile, "env.json").toPath(), "{\"appGenerationTime\": 1791450000000}");
			assertNull("an NGX build is there: the application", ApplicationBuilds.statusPage("App", projectDir));

			ApplicationBuilds.setState("App", ApplicationBuilds.State.failed);
			assertNotNull("a failed build is still told", ApplicationBuilds.statusPage("App", projectDir));
		} finally {
			ApplicationBuilds.setState("App", null);
		}
	}

	@Test
	public void eachWriteRearmsTheDelayAndOnlyOneBuildRuns() throws Exception {
		ApplicationBuilds.frontendDelay = 200;
		var started = new CountDownLatch(1);
		var builds = new AtomicInteger();
		ApplicationBuilds.frontendWritten("App", () -> true, () -> { builds.incrementAndGet(); started.countDown(); });
		Thread.sleep(110);
		ApplicationBuilds.frontendWritten("App", () -> true, () -> { builds.incrementAndGet(); started.countDown(); });
		assertFalse("the first deadline was cancelled", started.await(130, TimeUnit.MILLISECONDS));
		assertTrue("the last write builds", started.await(2, TimeUnit.SECONDS));
		assertEquals(1, builds.get());
	}

	@Test
	public void aQueuedBuildAbsorbsNewRequestsAndTheQueueRemainsSerial() throws Exception {
		ApplicationBuilds.frontendDelay = 20;
		var firstStarted = new CountDownLatch(1);
		var releaseFirst = new CountDownLatch(1);
		var secondStarted = new CountDownLatch(1);
		var builds = new AtomicInteger();
		var active = new AtomicInteger();
		var maximum = new AtomicInteger();
		ApplicationBuilds.frontendWritten("First", () -> true, () -> {
			maximum.accumulateAndGet(active.incrementAndGet(), Math::max);
			firstStarted.countDown();
			try { releaseFirst.await(2, TimeUnit.SECONDS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
			active.decrementAndGet();
		});
		assertTrue(firstStarted.await(2, TimeUnit.SECONDS));
		try {
			for (int i = 0; i < 3; i++) {
				ApplicationBuilds.frontendWritten("Second", () -> true, () -> {
					maximum.accumulateAndGet(active.incrementAndGet(), Math::max);
					builds.incrementAndGet();
					secondStarted.countDown();
					active.decrementAndGet();
				});
				Thread.sleep(30);
			}
			Thread.sleep(50);
			assertEquals("the second build waits", 0, builds.get());
		} finally {
			releaseFirst.countDown();
		}
		assertTrue(secondStarted.await(2, TimeUnit.SECONDS));
		assertEquals("one queued build absorbs further writes", 1, builds.get());
		assertEquals("only one build at a time", 1, maximum.get());
	}

	@Test
	public void aBuildAlreadyRunningDoesNotAbsorbLaterWrites() throws Exception {
		ApplicationBuilds.frontendDelay = 20;
		var started = new CountDownLatch(1);
		var release = new CountDownLatch(1);
		var second = new CountDownLatch(1);
		var builds = new AtomicInteger();
		ApplicationBuilds.frontendWritten("App", () -> true, () -> {
			builds.incrementAndGet();
			started.countDown();
			try { release.await(2, TimeUnit.SECONDS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
		});
		assertTrue(started.await(2, TimeUnit.SECONDS));
		try {
			ApplicationBuilds.frontendWritten("App", () -> true, () -> {
				builds.incrementAndGet();
				second.countDown();
			});
			Thread.sleep(80);
			assertEquals("the second build waits for the first", 1, builds.get());
		} finally {
			release.countDown();
		}
		assertTrue("the later write builds after the running one", second.await(2, TimeUnit.SECONDS));
		assertEquals(2, builds.get());
	}

	@Test
	public void disabledPreferenceOrStoppedDevModeDoNotQueueABuild() throws Exception {
		ApplicationBuilds.frontendDelay = 20;
		var builds = new AtomicInteger();
		ApplicationBuilds.frontendWritten("Disabled", () -> false, builds::incrementAndGet);
		ApplicationBuilds.frontendWritten("Stopped", () -> false, builds::incrementAndGet);
		Thread.sleep(100);
		assertEquals(0, builds.get());
	}

	@Test
	public void editBuildRequiresThePreferenceStudioLevelAndNoCli() {
		var levels = EnginePropertiesManager.ServerBuild.values();
		for (var level : levels) {
			assertEquals(level.allows(EnginePropertiesManager.ServerBuild.studio),
					ApplicationBuilds.frontendAllowed(true, false, level));
			assertFalse(ApplicationBuilds.frontendAllowed(false, false, level));
			assertFalse(ApplicationBuilds.frontendAllowed(true, true, level));
		}
	}

	@Test
	public void onlyAnAvailableBuilderInDevModeAllowsTheEditBuild() throws Exception {
		assertFalse(ApplicationBuilds.builderServing(
				new JSONObject("{\"builders\":[{\"available\":true,\"state\":{\"serving\":false}}]}")));
		assertFalse(ApplicationBuilds.builderServing(
				new JSONObject("{\"builders\":[{\"available\":false,\"state\":{\"serving\":true}}]}")));
		assertTrue(ApplicationBuilds.builderServing(
				new JSONObject("{\"builders\":[{\"available\":true,\"state\":{\"serving\":true}}]}")));
	}

	@Test
	public void eachServerBuildLevelAllowsThoseBeforeIt() {
		var none = EnginePropertiesManager.ServerBuild.none;
		var sources = EnginePropertiesManager.ServerBuild.sources;
		var studio = EnginePropertiesManager.ServerBuild.studio;
		var all = EnginePropertiesManager.ServerBuild.all;
		assertFalse("none compiles nothing", none.allows(sources));
		assertTrue("sources compiles the Java sources", sources.allows(sources));
		assertFalse("sources runs no build of the web Studio", sources.allows(studio));
		assertTrue("studio compiles too", studio.allows(sources));
		assertFalse("studio does not build the applications deployed without their build", studio.allows(all));
		assertTrue("all builds them", all.allows(all) && all.allows(studio) && all.allows(sources));
		assertEquals("none", EnginePropertiesManager.PropertyName.SERVER_BUILD.getDefaultValue());
		assertEquals("disabled", EnginePropertiesManager.PropertyName.WEB_STUDIO.getDefaultValue());
	}
}
