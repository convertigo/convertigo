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

package com.twinsoft.convertigo.engine.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HexFormat;
import java.util.List;

public class DirClassLoader extends URLClassLoader {
	private static final String COMPLETE_MARKER = ".complete";
	private static final String LEGACY_OBSOLETE_MARKER = ".legacy-obsolete";
	private static final String OBSOLETE_MARKER = ".obsolete";
	private static final String SNAPSHOTS_DIRECTORY = "snapshots";
	private static final int SNAPSHOT_RETRIES = 3;
	private static final int DIGEST_BUFFER_SIZE = 65536;

	private static class ClasspathEntry {
		private final File source;
		private final String relativePath;

		private ClasspathEntry(File source, String relativePath) {
			this.source = source;
			this.relativePath = relativePath;
		}
	}

	private ClassLoader parent;
	
	private List<File> dirs;
	private long nextCheck = 0;
	private String lastContent;
	
	public DirClassLoader(File dir, ClassLoader parent) {
		this(Arrays.asList(dir), parent, null);
	}
	
	public DirClassLoader(List<File> dirs, ClassLoader parent, File copyTo) {
		super(makeURLs(dirs, copyTo), null);
		this.dirs = dirs;
		this.parent = parent;
		isContentChanged();
	}
	
	private static URL[] makeURLs(List<File> dirs, File copyTo) {
		List<ClasspathEntry> entries = listClasspathEntries(dirs);
		if (copyTo == null || entries.isEmpty()) {
			return toURLs(entries, null);
		}

		// A completed snapshot is immutable and can safely be shared by several JVMs.
		File snapshotsDirectory = new File(copyTo, SNAPSHOTS_DIRECTORY);
		for (int retry = 0; retry < SNAPSHOT_RETRIES; retry++) {
			try {
				String fingerprint = fingerprint(entries, null);
				File snapshotDirectory = new File(snapshotsDirectory, fingerprint);
				if (!isComplete(snapshotDirectory)) {
					prepareSnapshot(entries, snapshotsDirectory, snapshotDirectory, fingerprint);
				}
				markLegacyCopyObsolete(copyTo);
				markObsoleteSnapshots(snapshotsDirectory, snapshotDirectory);
				return toURLs(entries, snapshotDirectory);
			} catch (IOException e) {
				if (retry + 1 == SNAPSHOT_RETRIES) {
					throw new IllegalStateException("Unable to prepare an immutable project classpath snapshot", e);
				}
				entries = listClasspathEntries(dirs);
			}
		}
		throw new IllegalStateException("Unable to prepare an immutable project classpath snapshot");
	}

	private static List<ClasspathEntry> listClasspathEntries(List<File> dirs) {
		List<ClasspathEntry> entries = new ArrayList<>();
		for (int dirIndex = 0; dirIndex < dirs.size(); dirIndex++) {
			File dir = dirs.get(dirIndex);
			String[] list = dir.list();
			if (list == null) {
				continue;
			}
			Arrays.sort(list);
			for (String name : list) {
				if (name.endsWith(".jar") || name.equals("classes")) {
					entries.add(new ClasspathEntry(new File(dir, name), "source-" + dirIndex + "/" + name));
				}
			}
		}
		return entries;
	}

	private static URL[] toURLs(List<ClasspathEntry> entries, File snapshotDirectory) {
		ArrayList<URL> urls = new ArrayList<>();
		for (ClasspathEntry entry : entries) {
			File file = snapshotDirectory == null ? entry.source : new File(snapshotDirectory, entry.relativePath);
			try {
				urls.add(file.toURI().toURL());
			} catch (Exception e) {
				throw new IllegalStateException("Unable to add classpath entry \"" + file + "\"", e);
			}
		}
		return urls.toArray(new URL[urls.size()]);
	}

	private static String fingerprint(List<ClasspathEntry> entries, File snapshotDirectory) throws IOException {
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 is not available", e);
		}

		for (ClasspathEntry entry : entries) {
			File file = snapshotDirectory == null ? entry.source : new File(snapshotDirectory, entry.relativePath);
			appendDigest(digest, file, entry.relativePath);
		}
		return HexFormat.of().formatHex(digest.digest());
	}

	private static void appendDigest(MessageDigest digest, File file, String relativePath) throws IOException {
		digest.update(relativePath.getBytes(StandardCharsets.UTF_8));
		digest.update((byte) 0);
		digest.update((byte) (file.isDirectory() ? 'D' : 'F'));
		digest.update((byte) 0);

		if (file.isDirectory()) {
			File[] children = file.listFiles();
			if (children == null) {
				throw new IOException("Unable to list classpath directory \"" + file + "\"");
			}
			Arrays.sort(children, (left, right) -> left.getName().compareTo(right.getName()));
			for (File child : children) {
				appendDigest(digest, child, relativePath + "/" + child.getName());
			}
		} else {
			digest.update(Long.toString(file.length()).getBytes(StandardCharsets.UTF_8));
			digest.update((byte) 0);
			byte[] buffer = new byte[DIGEST_BUFFER_SIZE];
			try (InputStream input = new FileInputStream(file)) {
				int length;
				while ((length = input.read(buffer)) != -1) {
					digest.update(buffer, 0, length);
				}
			}
		}
	}

	private static void prepareSnapshot(List<ClasspathEntry> entries, File snapshotsDirectory,
			File snapshotDirectory, String fingerprint) throws IOException {
		Files.createDirectories(snapshotsDirectory.toPath());
		// Publish only a fully copied and verified directory, never files one by one.
		Path staging = Files.createTempDirectory(snapshotsDirectory.toPath(), ".staging-");
		try {
			for (ClasspathEntry entry : entries) {
				File destination = new File(staging.toFile(), entry.relativePath);
				Files.createDirectories(destination.getParentFile().toPath());
				if (entry.source.isDirectory()) {
					FileUtils.copyDirectory(entry.source, destination);
				} else {
					FileUtils.copyFile(entry.source, destination);
				}
			}

			if (!fingerprint.equals(fingerprint(entries, staging.toFile()))) {
				throw new IOException("Project classpath changed while its snapshot was being copied");
			}
			Files.createFile(staging.resolve(COMPLETE_MARKER));
			try {
				Files.move(staging, snapshotDirectory.toPath());
			} catch (IOException e) {
				if (!isComplete(snapshotDirectory)) {
					throw e;
				}
			}
		} finally {
			FileUtils.deleteQuietly(staging.toFile());
		}
	}

	private static boolean isComplete(File snapshotDirectory) {
		return new File(snapshotDirectory, COMPLETE_MARKER).isFile();
	}

	private static void markLegacyCopyObsolete(File copyTo) {
		String[] legacyEntries = copyTo.list((dir, name) -> name.endsWith(".jar") || name.equals("classes"));
		if (legacyEntries == null || legacyEntries.length == 0) {
			return;
		}
		try {
			Path marker = new File(copyTo, LEGACY_OBSOLETE_MARKER).toPath();
			if (!Files.exists(marker)) {
				Files.createFile(marker);
			}
		} catch (IOException e) {
			// Marking is best effort and must not prevent the project from loading.
		}
	}

	private static void markObsoleteSnapshots(File snapshotsDirectory, File currentSnapshot) {
		// Cleanup is deliberately deferred until a cluster-wide retention policy exists.
		File[] snapshots = snapshotsDirectory.listFiles(file -> file.isDirectory() && isComplete(file));
		if (snapshots == null) {
			return;
		}
		for (File snapshot : snapshots) {
			Path obsoleteMarker = new File(snapshot, OBSOLETE_MARKER).toPath();
			try {
				if (snapshot.equals(currentSnapshot)) {
					Files.deleteIfExists(obsoleteMarker);
				} else if (!Files.exists(obsoleteMarker)) {
					Files.createFile(obsoleteMarker);
				}
			} catch (IOException e) {
				// Marking is best effort and must not prevent the project from loading.
			}
		}
	}

	private static void appendContent(StringBuilder content, File file) {
		content.append(file.getAbsolutePath())
			.append(':').append(file.length())
			.append(':').append(file.lastModified())
			.append(';');
	}
	
	public synchronized boolean isContentChanged() {
		long now = System.currentTimeMillis();
		if (now < nextCheck) {
			return false;
		}
		nextCheck = now + 5000;
		StringBuilder sb = new StringBuilder();
		for (ClasspathEntry entry : listClasspathEntries(dirs)) {
			appendContent(sb, entry.source);
		}
		String content = sb.toString();
		if (content.equals(lastContent)) {
			return false;
		}
		lastContent = content;
		return true;
	}

	@Override
	public Class<?> loadClass(String name) throws ClassNotFoundException {
		Class<?> cls = null;
		try {
			cls = super.loadClass(name);
		} catch (ClassNotFoundException e) {
			if (parent != null) {
				cls = parent.loadClass(name);
			}
		}
		return cls;
	}

	@Override
	public URL getResource(String name) {
		URL url = super.getResource(name);
		if (url == null && parent != null) {
			url = parent.getResource(name);
		}
		return url;
	}

	@Override
	public Enumeration<URL> getResources(String name) throws IOException {
		Enumeration<URL> eu = super.getResources(name);
		if (!eu.hasMoreElements() && parent != null) {
			eu = parent.getResources(name);
		}
		return eu;
	}

	@Override
	public InputStream getResourceAsStream(String name) {
		InputStream is = super.getResourceAsStream(name);
		if (is == null && parent != null) {
			is = parent.getResourceAsStream(name);
		}
		return is;
	}
	
}
