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

package com.convertigo.gradle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.zip.ZipFile;

import org.gradle.testkit.runner.GradleRunner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.twinsoft.convertigo.engine.ProjectLibraries;

/** The car task exports the classes compiled from libs/src, for the servers that do not build. */
public class CarSourcesTest {
	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	@Test
	public void theArchiveCarriesTheClassesOfTheSourcesWithTheirFingerprint() throws Exception {
		var projectDir = folder.newFolder("CarSources");
		Files.writeString(new File(projectDir, "settings.gradle").toPath(), "rootProject.name = 'CarSources'\n");
		Files.writeString(new File(projectDir, "build.gradle").toPath(), "plugins { id 'convertigo' }\n");
		Files.writeString(new File(projectDir, "c8oProject.yaml").toPath(), """
				↑convertigo: 8.5.0.m006
				↓CarSources [core.Project]:\s
				  comment: sources compiled by the car task
				""");
		var src = new File(projectDir, "libs/src/probe");
		src.mkdirs();
		Files.writeString(new File(src, "Greeter.java").toPath(), """
				package probe;
				public class Greeter {
					public static String greet() {
						return "hello from " + com.twinsoft.convertigo.engine.Engine.class.getSimpleName();
					}
				}
				""");
		Files.writeString(new File(src, "greeting.txt").toPath(), "bonjour");
		// a former build folder of the project is never exported
		new File(projectDir, "libs/build/classes/stale").mkdirs();
		Files.writeString(new File(projectDir, "libs/build/classes/stale/Old.class").toPath(), "stale");

		var result = GradleRunner.create()
			.withProjectDir(projectDir)
			.withPluginClasspath()
			.withArguments("car", "--stacktrace")
			.forwardOutput()
			.build();
		assertNotNull(result.task(":car"));

		var archives = new File(projectDir, "build").listFiles((dir, name) -> name.endsWith(".car"));
		assertNotNull(archives);
		assertEquals(1, archives.length);
		try (var car = new ZipFile(archives[0])) {
			assertNotNull("the sources", car.getEntry("CarSources/libs/src/probe/Greeter.java"));
			assertNotNull("their classes", car.getEntry("CarSources/libs/build/classes/probe/Greeter.class"));
			assertNotNull("their resources", car.getEntry("CarSources/libs/build/classes/probe/greeting.txt"));
			assertNull("never a former build folder", car.getEntry("CarSources/libs/build/classes/stale/Old.class"));
			var fingerprint = car.getEntry("CarSources/libs/build/src.sha256");
			assertNotNull("the fingerprint of the sources", fingerprint);
			try (var input = car.getInputStream(fingerprint)) {
				assertEquals("the fingerprint a server compares with the sources of the archive",
						ProjectLibraries.sourcesFingerprint(new File(projectDir, "libs/src")),
						new String(input.readAllBytes(), StandardCharsets.UTF_8).trim());
			}
		}
		assertTrue("nothing is written in the project", !new File(projectDir, "libs/src/probe/Greeter.class").exists());
	}
}
