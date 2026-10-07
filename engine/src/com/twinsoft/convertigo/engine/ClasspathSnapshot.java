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
 * The libraries of a project (the jars, classes and src folders of its libs folders, with the classes compiled before
 * from src, in a build folder) and their immutable copies: a class loader reads a copy, so the libraries can be
 * replaced while it is in use. A completed copy never changes: it is published only once fully copied, verified and
 * its sources compiled (see {@link ProjectSources}), and named after the fingerprint of its content, followed by
 * {@link #WITHOUT_SOURCES} when sources did not get their classes.
 */
final class ClasspathSnapshot {
	static final String COMPLETE_MARKER = ".complete";
	private static final String LEGACY_OBSOLETE_MARKER = ".legacy-obsolete";
	private static final String OBSOLETE_MARKER = ".obsolete";
	static final String STAGING_PREFIX = ".staging-";
	/** The Java sources of a libs folder, compiled with the project. */
	static final String SOURCES = "src";
	/** The classes compiled from the sources of a libs folder before, by the CI or the export of the project. */
	static final String BUILD = "build";
	static final String BUILD_CLASSES = "classes";
	/** In the build folder: the fingerprint of the sources its classes are compiled from. */
	static final String BUILD_FINGERPRINT = "src.sha256";
	/** In a copy: the classes of the sources of a libs folder, next to them. */
	static final String COMPILED_SUFFIX = ".classes";
	/** The name of a copy in which sources did not get their classes ends with it. */
	static final String WITHOUT_SOURCES = "-without-sources";
	private static final int DIGEST_BUFFER_SIZE = 65536;

	enum Kind {
		/** a jar or a classes folder, used as it is */
		LIBRARY,
		/** a src folder, compiled */
		SOURCES,
		/** a build folder, whose classes are those of the src folder when compiled from the same sources */
		BUILD
	}

	/** An item of a libs folder (dir: its index among the libs folders), and its path in a copy. */
	record Entry(File source, String relativePath, Kind kind, int dir) {
		/** @return in a copy, where the classes of a src folder are */
		String compiledPath() {
			return relativePath + COMPILED_SUFFIX;
		}
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
				var file = new File(dir, name);
				var kind = name.endsWith(".jar") || name.equals("classes") ? Kind.LIBRARY
						: name.equals(SOURCES) && file.isDirectory() ? Kind.SOURCES
						: name.equals(BUILD) && new File(file, BUILD_CLASSES).isDirectory() ? Kind.BUILD : null;
				if (kind != null) {
					entries.add(new Entry(file, "source-" + dirIndex + "/" + name, kind, dirIndex));
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

	/**
	 * @return the fingerprint of the content of a src folder, wherever it is: the one written in a build folder with the
	 *         classes compiled from it
	 */
	static String sourcesFingerprint(File sources) throws IOException {
		var digest = sha256();
		appendDigest(digest, sources, SOURCES);
		return HexFormat.of().formatHex(digest.digest());
	}

	private static MessageDigest sha256() {
		try {
			return MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 is not available", e);
		}
	}

	private static String fingerprint(List<Entry> entries, File snapshot) throws IOException {
		var digest = sha256();
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
	 * @param compile whether sources can be compiled; when not, they get only the classes of their build folder, if
	 *        compiled from the same sources
	 * @return the completed copy of the libraries in the snapshots folder, made if needed
	 * @throws IOException when the libraries change during the copy, or sources do not compile, among others
	 */
	static File prepare(String project, List<Entry> entries, File snapshots, String fingerprint, boolean compile)
			throws IOException {
		var snapshot = new File(snapshots, fingerprint);
		if (isComplete(snapshot)) {
			return snapshot;
		}
		var withoutSources = new File(snapshots, fingerprint + WITHOUT_SOURCES);
		if (!compile && isComplete(withoutSources)) {
			return withoutSources;
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
			if (!ProjectSources.build(project, entries, staging.toFile(), compile)) {
				snapshot = withoutSources;
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

	/** @return the class path of a copy: its jars and classes folders, and the classes of its src folders */
	static URL[] urls(List<Entry> entries, File snapshot) {
		var urls = new ArrayList<URL>();
		for (var file : classpath(entries, snapshot)) {
			try {
				urls.add(file.toURI().toURL());
			} catch (Exception e) {
				throw new IllegalStateException("Unable to add classpath entry \"" + file + "\"", e);
			}
		}
		return urls.toArray(new URL[urls.size()]);
	}

	static List<File> classpath(List<Entry> entries, File snapshot) {
		var files = new ArrayList<File>();
		for (var entry : entries) {
			if (entry.kind() == Kind.LIBRARY) {
				files.add(snapshot == null ? entry.source() : new File(snapshot, entry.relativePath()));
			} else if (entry.kind() == Kind.SOURCES && snapshot != null) {
				var compiled = new File(snapshot, entry.compiledPath());
				if (compiled.isDirectory()) {
					files.add(compiled);
				}
			} else if (entry.kind() == Kind.BUILD && snapshot != null
					&& entries.stream().noneMatch(other -> other.kind() == Kind.SOURCES && other.dir() == entry.dir())) {
				// classes delivered without their sources
				files.add(new File(snapshot, entry.relativePath() + "/" + BUILD_CLASSES));
			}
		}
		return files;
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
