/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this program; if not, see <http://www.gnu.org/licenses/>.
 */

package com.convertigo.gradle;

import static org.junit.Assert.assertEquals;

import java.nio.file.Files;

import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class BooleanTaskPropertiesTest {
	private static final String[] CAR_FLAGS = {
		"includeTestCases", "includeStubs", "includeMobileApp", "includeMobileAppAssets",
		"includeMobileDataset", "includeMobilePlatformsAssets"
	};
	private static final String[] DEPLOY_FLAGS = { "trustAllCertificates", "assembleXsl" };

	@Rule
	public TemporaryFolder projectDir = new TemporaryFolder();

	@Test
	public void accessorsUsePrimitiveBooleans() throws Exception {
		assertAccessors(ProjectCar.class, CAR_FLAGS);
		assertAccessors(ProjectDeploy.class, DEPLOY_FLAGS);
		assertAccessors(ExportDependencies.class, CAR_FLAGS);
	}

	@Test
	public void flagsKeepDefaultsAndSupportGradleAndGroovyAssignments() throws Exception {
		Files.writeString(projectDir.newFile("settings.gradle").toPath(),
			"rootProject.name = 'boolean-task-properties-test'\n");
		Files.writeString(projectDir.newFile("build.gradle").toPath(), """
			plugins { id 'convertigo' }

			def checkFlags = { task, names, defaultValue ->
				for (def name : names) {
					assert task.property(name) == defaultValue
					for (def value : [false, true]) {
						task.setProperty(name, value)
						assert task.property(name) == value
						assert task.inputs.properties[name] == value
						task."${name}" = !value
						assert task."${name}" == !value
						assert task.inputs.properties[name] == !value
					}
				}
			}

			tasks.register('checkBooleanFlags') {
				doLast {
					def carFlags = ['includeTestCases', 'includeStubs', 'includeMobileApp',
						'includeMobileAppAssets', 'includeMobileDataset', 'includeMobilePlatformsAssets']
					checkFlags(tasks.named('car').get(), carFlags, true)
					checkFlags(tasks.named('exportDependencies').get(), carFlags, true)
					checkFlags(tasks.named('deploy').get(), ['trustAllCertificates', 'assembleXsl'], false)
				}
			}
			""");
		var result = GradleRunner.create()
			.withProjectDir(projectDir.getRoot())
			.withPluginClasspath()
			.withArguments("checkBooleanFlags", "--offline", "--stacktrace")
			.build();
		assertEquals(TaskOutcome.SUCCESS, result.task(":checkBooleanFlags").getOutcome());
	}

	private static void assertAccessors(Class<?> type, String[] properties) throws Exception {
		for (String name : properties) {
			String suffix = Character.toUpperCase(name.charAt(0)) + name.substring(1);
			assertEquals(name, boolean.class, type.getMethod("is" + suffix).getReturnType());
			assertEquals(name, void.class, type.getMethod("set" + suffix, boolean.class).getReturnType());
		}
	}
}
