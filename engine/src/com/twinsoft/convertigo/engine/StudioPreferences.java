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

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.codehaus.jettison.json.JSONObject;

/** Workspace preferences shared by the engine and both Studios. */
public final class StudioPreferences {
	public static final boolean DEFAULT_AUTO_BUILD_PROD_WHILE_EDITING = true;
	private static long modified = Long.MIN_VALUE;
	private static String cachedPath;
	private static boolean autoBuild = DEFAULT_AUTO_BUILD_PROD_WHILE_EDITING;
	static java.util.function.Supplier<File> preferencesFile =
			() -> new File(Engine.CONFIGURATION_PATH, "studio_preferences.json");

	private StudioPreferences() {
	}

	private static File file() {
		return preferencesFile.get();
	}

	public static synchronized boolean autoBuildProdWhileEditing() {
		var file = file();
		var date = file.exists() ? file.lastModified() : -1;
		if (date != modified || !file.getAbsolutePath().equals(cachedPath)) {
			autoBuild = DEFAULT_AUTO_BUILD_PROD_WHILE_EDITING;
			if (file.isFile()) {
				try {
					var json = new JSONObject(Files.readString(file.toPath(), StandardCharsets.UTF_8));
					autoBuild = json.optBoolean("autoBuildProdWhileEditing", DEFAULT_AUTO_BUILD_PROD_WHILE_EDITING);
				} catch (Exception e) {
					if (Engine.logEngine != null) Engine.logEngine.warn("Unable to read Studio preferences", e);
				}
			}
			modified = date;
			cachedPath = file.getAbsolutePath();
		}
		return autoBuild;
	}

	public static synchronized void setAutoBuildProdWhileEditing(boolean enabled) throws Exception {
		var file = file();
		Files.createDirectories(file.getParentFile().toPath());
		var temporary = Files.createTempFile(file.getParentFile().toPath(), "studio_preferences-", ".json");
		try {
			Files.writeString(temporary, new JSONObject().put("autoBuildProdWhileEditing", enabled).toString(),
					StandardCharsets.UTF_8);
			Files.move(temporary, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
		} finally {
			Files.deleteIfExists(temporary);
		}
		autoBuild = enabled;
		modified = file.lastModified();
		cachedPath = file.getAbsolutePath();
	}
}
