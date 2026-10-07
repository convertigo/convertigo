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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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
	}
}
