/*
 * Copyright (c) 2001-2026 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this program; if not, see <http://www.gnu.org/licenses/>.
 */

package com.twinsoft.convertigo.engine;

import static org.junit.Assert.*;

import java.io.File;
import java.nio.file.Files;

import org.junit.Test;

import com.twinsoft.convertigo.engine.util.FileUtils;

public class StudioPreferencesTest {
	@Test
	public void workspacePreferenceDefaultsOnAndPersistsChanges() throws Exception {
		var base = Files.createTempDirectory("studio-preferences-test").toFile();
		var file = new File(base, "studio_preferences.json");
		StudioPreferences.preferencesFile = () -> file;
		try {
			assertTrue("missing file defaults on", StudioPreferences.autoBuildProdWhileEditing());
			Files.writeString(file.toPath(), "{}");
			assertTrue("missing key defaults on", StudioPreferences.autoBuildProdWhileEditing());
			StudioPreferences.setAutoBuildProdWhileEditing(false);
			assertFalse(StudioPreferences.autoBuildProdWhileEditing());
			assertTrue(Files.readString(file.toPath()).contains("\"autoBuildProdWhileEditing\":false"));
			StudioPreferences.setAutoBuildProdWhileEditing(true);
			assertTrue(StudioPreferences.autoBuildProdWhileEditing());
		} finally {
			StudioPreferences.preferencesFile =
					() -> new File(Engine.CONFIGURATION_PATH, "studio_preferences.json");
			FileUtils.deleteQuietly(base);
		}
	}
}
