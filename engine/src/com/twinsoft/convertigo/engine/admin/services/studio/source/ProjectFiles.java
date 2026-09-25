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

package com.twinsoft.convertigo.engine.admin.services.studio.source;

import java.io.File;
import java.io.FileInputStream;
import java.util.Set;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;

/**
 * The text files of a project that the web Studio edits, as the tree names them: "Project/relative/path".
 * A file is always resolved inside the folder of its project.
 */
class ProjectFiles {
	static final long MAX_SIZE = 2 * 1024 * 1024;

	private static final Set<String> BINARY_EXTENSIONS = Set.of("png", "jpg", "jpeg", "gif", "ico", "webp", "bmp",
			"zip", "car", "jar", "gz", "tgz", "pdf", "woff", "woff2", "ttf", "otf", "eot", "mp3", "mp4", "class",
			"db", "sqlite", "xlsx", "docx");

	private ProjectFiles() {
	}

	static boolean isFileId(String id) {
		return id != null && id.contains("/");
	}

	/**
	 * @return the file of a tree id, which must be inside the folder of its project
	 */
	static File resolve(String id) throws Exception {
		if (!isFileId(id)) {
			throw new ServiceException("The id " + id + " is not a project file.");
		}
		var split = id.split("/", 2);
		var project = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(split[0]);
		if (project == null) {
			throw new ServiceException("The project " + split[0] + " does not exist.");
		}
		var root = project.getDirFile().getCanonicalFile();
		var file = new File(root, split[1]).getCanonicalFile();
		if (!file.getPath().startsWith(root.getPath() + File.separator)) {
			throw new ServiceException("The file " + split[1] + " is outside the project " + split[0] + ".");
		}
		return file;
	}

	/**
	 * @return whether the Studio can edit the file as text: not too big and not binary
	 */
	static boolean isText(File file) throws Exception {
		if (!file.isFile() || file.length() > MAX_SIZE) {
			return false;
		}
		var name = file.getName().toLowerCase();
		var dot = name.lastIndexOf('.');
		if (dot >= 0 && BINARY_EXTENSIONS.contains(name.substring(dot + 1))) {
			return false;
		}
		// a NUL byte in the first bytes tells a binary file
		try (var in = new FileInputStream(file)) {
			var buffer = new byte[8192];
			var read = in.read(buffer);
			for (int i = 0; i < read; i++) {
				if (buffer[i] == 0) {
					return false;
				}
			}
		}
		return true;
	}

	static String language(File file) {
		var name = file.getName().toLowerCase();
		var dot = name.lastIndexOf('.');
		var extension = dot < 0 ? "" : name.substring(dot + 1);
		return switch (extension) {
		case "js", "mjs", "cjs" -> "javascript";
		case "ts" -> "typescript";
		case "css" -> "css";
		case "scss" -> "scss";
		case "less" -> "less";
		case "html", "htm" -> "html";
		case "xml", "xsl", "xslt", "xsd", "wsdl", "svg" -> "xml";
		case "json" -> "json";
		case "yaml", "yml" -> "yaml";
		case "md" -> "markdown";
		case "sql" -> "sql";
		case "java" -> "java";
		case "properties", "ini" -> "ini";
		case "sh" -> "shell";
		default -> "plaintext";
		};
	}
}
