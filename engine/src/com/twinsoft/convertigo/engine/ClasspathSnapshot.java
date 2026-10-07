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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;

import com.twinsoft.convertigo.engine.util.FileUtils;

/**
 * The libraries of a project (the jars and classes folders of its libs folders) and their immutable copies: a class
 * loader reads a copy, so the libraries can be replaced while it is in use. A completed copy never changes: it is
 * published only once fully copied and verified, and named after the fingerprint of its content.
 */
final class ClasspathSnapshot {
	static final String COMPLETE_MARKER = ".complete";
	private static final String LEGACY_OBSOLETE_MARKER = ".legacy-obsolete";
	private static final String OBSOLETE_MARKER = ".obsolete";
	static final String STAGING_PREFIX = ".staging-";
	private static final int DIGEST_BUFFER_SIZE = 65536;

	/** A jar or classes folder of a libs folder, and its path in a copy. */
	record Entry(File source, String relativePath) {
	}

	private ClasspathSnapshot() {
	}

	static List<Entry> entries(List<File> dirs) {
		var entries = new ArrayList<Entry>();
		for (int dirIndex = 0; dirIndex < dirs.size(); dirIndex++) {
			var dir = dirs.get(dirIndex);
			var list = dir.list();
			if (list == null) {
				continue;
			}
			Arrays.sort(list);
			for (var name : list) {
				if (name.endsWith(".jar") || name.equals("classes")) {
					entries.add(new Entry(new File(dir, name), "source-" + dirIndex + "/" + name));
				}
			}
		}
		return entries;
	}

	/**
	 * @return a cheap stamp of the libraries: their folders, and the path, size and date of each file, including those
	 *         in the sub-folders of a classes folder
	 */
	static String stamp(List<File> dirs, List<Entry> entries) {
		var stamp = new StringBuilder();
		for (var dir : dirs) {
			stamp.append(dir.getAbsolutePath()).append('|');
		}
		for (var entry : entries) {
			appendStamp(stamp, entry.source());
		}
		return stamp.toString();
	}

	private static void appendStamp(StringBuilder stamp, File file) {
		stamp.append(file.getAbsolutePath()).append(':');
		if (file.isDirectory()) {
			stamp.append("D;");
			var children = file.listFiles();
			if (children != null) {
				Arrays.sort(children, (left, right) -> left.getName().compareTo(right.getName()));
				for (var child : children) {
					appendStamp(stamp, child);
				}
			}
		} else {
			stamp.append(file.length()).append(':').append(file.lastModified()).append(';');
		}
	}

	/** @return the fingerprint of the content of the libraries */
	static String fingerprint(List<Entry> entries) throws IOException {
		return fingerprint(entries, null);
	}

	private static String fingerprint(List<Entry> entries, File snapshot) throws IOException {
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 is not available", e);
		}
		for (var entry : entries) {
			var file = snapshot == null ? entry.source() : new File(snapshot, entry.relativePath());
			appendDigest(digest, file, entry.relativePath());
		}
		return HexFormat.of().formatHex(digest.digest());
	}

	private static void appendDigest(MessageDigest digest, File file, String relativePath) throws IOException {
		digest.update(relativePath.getBytes(StandardCharsets.UTF_8));
		digest.update((byte) 0);
		digest.update((byte) (file.isDirectory() ? 'D' : 'F'));
		digest.update((byte) 0);
		if (file.isDirectory()) {
			var children = file.listFiles();
			if (children == null) {
				throw new IOException("Unable to list classpath directory \"" + file + "\"");
			}
			Arrays.sort(children, (left, right) -> left.getName().compareTo(right.getName()));
			for (var child : children) {
				appendDigest(digest, child, relativePath + "/" + child.getName());
			}
		} else {
			digest.update(Long.toString(file.length()).getBytes(StandardCharsets.UTF_8));
			digest.update((byte) 0);
			var buffer = new byte[DIGEST_BUFFER_SIZE];
			try (InputStream input = new FileInputStream(file)) {
				int length;
				while ((length = input.read(buffer)) != -1) {
					digest.update(buffer, 0, length);
				}
			}
		}
	}

	/**
	 * @return the completed copy of the libraries in the snapshots folder, made if needed
	 * @throws IOException when the libraries change during the copy, among others
	 */
	static File prepare(List<Entry> entries, File snapshots, String fingerprint) throws IOException {
		var snapshot = new File(snapshots, fingerprint);
		if (isComplete(snapshot)) {
			return snapshot;
		}
		Files.createDirectories(snapshots.toPath());
		// publish only a fully copied and verified folder, never files one by one
		Path staging = Files.createTempDirectory(snapshots.toPath(), STAGING_PREFIX);
		try {
			for (var entry : entries) {
				var destination = new File(staging.toFile(), entry.relativePath());
				Files.createDirectories(destination.getParentFile().toPath());
				if (entry.source().isDirectory()) {
					FileUtils.copyDirectory(entry.source(), destination);
				} else {
					FileUtils.copyFile(entry.source(), destination);
				}
			}
			if (!fingerprint.equals(fingerprint(entries, staging.toFile()))) {
				throw new IOException("Project classpath changed while its snapshot was being copied");
			}
			Files.createFile(staging.resolve(COMPLETE_MARKER));
			try {
				Files.move(staging, snapshot.toPath());
			} catch (IOException e) {
				if (!isComplete(snapshot)) {
					throw e;
				}
			}
		} finally {
			FileUtils.deleteQuietly(staging.toFile());
		}
		return snapshot;
	}

	static URL[] urls(List<Entry> entries, File snapshot) {
		var urls = new URL[entries.size()];
		for (int i = 0; i < urls.length; i++) {
			var entry = entries.get(i);
			var file = snapshot == null ? entry.source() : new File(snapshot, entry.relativePath());
			try {
				urls[i] = file.toURI().toURL();
			} catch (Exception e) {
				throw new IllegalStateException("Unable to add classpath entry \"" + file + "\"", e);
			}
		}
		return urls;
	}

	static boolean isComplete(File snapshot) {
		return new File(snapshot, COMPLETE_MARKER).isFile();
	}

	/**
	 * In the workspace, shared by the instances of a cluster: the copies made by former versions of the engine and the
	 * snapshots not used here are only marked, as another instance may use them.
	 */
	static void markObsolete(File copyTo, File snapshots, File current) {
		var legacyEntries = copyTo.list((dir, name) -> name.endsWith(".jar") || name.equals("classes"));
		if (legacyEntries != null && legacyEntries.length > 0) {
			try {
				var marker = new File(copyTo, LEGACY_OBSOLETE_MARKER).toPath();
				if (!Files.exists(marker)) {
					Files.createFile(marker);
				}
			} catch (IOException e) {
				// marking is best effort and must not prevent the project from loading
			}
		}
		var completed = snapshots.listFiles(file -> file.isDirectory() && isComplete(file));
		if (completed == null) {
			return;
		}
		for (var snapshot : completed) {
			var obsoleteMarker = new File(snapshot, OBSOLETE_MARKER).toPath();
			try {
				if (snapshot.equals(current)) {
					Files.deleteIfExists(obsoleteMarker);
				} else if (!Files.exists(obsoleteMarker)) {
					Files.createFile(obsoleteMarker);
				}
			} catch (IOException e) {
				// marking is best effort and must not prevent the project from loading
			}
		}
	}
}
