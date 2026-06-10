/*
 * Copyright (c) 2001-2026 Convertigo SA.
 * 
 * This program  is free software; you  can redistribute it and/or
 * Modify  it  under the  terms of the  GNU Affero General Public
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

package com.twinsoft.convertigo.engine.servlets;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jgit.ignore.IgnoreNode;

final class ProjectHttpIgnore {
	static final String FILENAME = ".httpignore";

	private static final Map<String, CacheEntry> cache = new ConcurrentHashMap<String, CacheEntry>();

	private ProjectHttpIgnore() {
	}

	static boolean isIgnored(File projectDir, String relativePath, boolean isDirectory) throws IOException {
		if (projectDir == null || relativePath == null || relativePath.isEmpty()) {
			return false;
		}

		relativePath = normalize(relativePath);
		if (relativePath.isEmpty()) {
			return false;
		}
		if (isHttpIgnorePath(relativePath)) {
			return true;
		}

		CacheEntry entry = getCacheEntry(new File(projectDir, FILENAME));
		if (entry.ignoreNode == null) {
			return false;
		}

		return entry.isIgnored(relativePath, isDirectory);
	}

	static String getRelativePath(File projectDir, File file) {
		if (projectDir == null || file == null) {
			return null;
		}

		Path projectPath = projectDir.getAbsoluteFile().toPath().normalize();
		Path filePath = file.getAbsoluteFile().toPath().normalize();
		if (!filePath.startsWith(projectPath)) {
			return null;
		}
		return projectPath.relativize(filePath).toString().replace(File.separatorChar, '/');
	}

	private static CacheEntry getCacheEntry(File ignoreFile) throws IOException {
		String key = ignoreFile.getCanonicalPath();
		boolean exists = ignoreFile.isFile();
		long lastModified = exists ? ignoreFile.lastModified() : -1;
		long length = exists ? ignoreFile.length() : -1;

		CacheEntry entry = cache.get(key);
		if (entry != null && entry.matches(exists, lastModified, length)) {
			return entry;
		}

		CacheEntry updated = CacheEntry.load(ignoreFile, exists, lastModified, length);
		cache.put(key, updated);
		return updated;
	}

	private static String normalize(String relativePath) {
		while (relativePath.startsWith("/")) {
			relativePath = relativePath.substring(1);
		}
		while (relativePath.endsWith("/")) {
			relativePath = relativePath.substring(0, relativePath.length() - 1);
		}
		return relativePath.replace('\\', '/');
	}

	private static boolean isHttpIgnorePath(String relativePath) {
		return relativePath.equals(FILENAME)
				|| relativePath.startsWith(FILENAME + "/")
				|| relativePath.endsWith("/" + FILENAME)
				|| relativePath.contains("/" + FILENAME + "/");
	}

	private static String getParentPath(String path) {
		int slash = path.lastIndexOf('/');
		return slash == -1 ? null : path.substring(0, slash);
	}

	private static class CacheEntry {
		private final boolean exists;
		private final long lastModified;
		private final long length;
		private final IgnoreNode ignoreNode;

		private CacheEntry(boolean exists, long lastModified, long length, IgnoreNode ignoreNode) {
			this.exists = exists;
			this.lastModified = lastModified;
			this.length = length;
			this.ignoreNode = ignoreNode;
		}

		private static CacheEntry load(File ignoreFile, boolean exists, long lastModified, long length) throws IOException {
			IgnoreNode ignoreNode = null;
			if (exists) {
				ignoreNode = new IgnoreNode();
				try (FileInputStream inputStream = new FileInputStream(ignoreFile)) {
					ignoreNode.parse(ignoreFile.getAbsolutePath(), inputStream);
				}
			}
			return new CacheEntry(exists, lastModified, length, ignoreNode);
		}

		private boolean matches(boolean exists, long lastModified, long length) {
			return this.exists == exists && this.lastModified == lastModified && this.length == length;
		}

		private boolean isIgnored(String relativePath, boolean isDirectory) {
			synchronized (ignoreNode) {
				String parentPath = getParentPath(relativePath);
				if (parentPath != null && isDirectoryIgnored(parentPath)) {
					return true;
				}

				Boolean ignored = ignoreNode.checkIgnored(relativePath, isDirectory);
				return Boolean.TRUE.equals(ignored);
			}
		}

		private boolean isDirectoryIgnored(String relativePath) {
			for (String path = relativePath; path != null && !path.isEmpty(); path = getParentPath(path)) {
				Boolean ignored = ignoreNode.checkIgnored(path, true);
				if (ignored != null) {
					return ignored.booleanValue();
				}
			}
			return false;
		}
	}
}
