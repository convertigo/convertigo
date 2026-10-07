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
import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;

import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.util.FileUtils;

/**
 * The local working directory of the engine (engine property "local_work.directory"): fast storage, local to this
 * instance and never shared with the others. Projects keep there the rebuildable folders they would otherwise write
 * in their folder of the workspace, which is shared and slow storage in cloud deployments, and the engine its own
 * rebuildable folders, such as the Node.js distributions.
 * <p>
 * A relocated folder becomes a symbolic link to its place in the directory of its project, here. That directory
 * mirrors the project: its other top-level entries are links to the project ones, so that the relative paths of the
 * folder (../../DisplayObjects) still reach the project.
 * <p>
 * The directory may be reused after a restart, but its content is never trusted as is: at startup, everything
 * produced by another Convertigo version, by a work interrupted during the previous run, or for a project that no
 * longer exists is removed, without delaying the startup: it leaves its place at once for a trash emptied in the
 * background. A lock keeps two live engines configured with the same directory apart. Only the
 * entries created here are ever removed, and links are removed without their targets: a directory set by mistake
 * keeps its other content, and a project its folders.
 */
public final class LocalWorkDirectory {
	private static final String LOCK = ".lock";
	private static final String VERSION = ".convertigo-version";
	private static final String PROJECTS = "projects";
	private static final String WORKING = ".c8o-working";
	private static final String INSTANCE_PREFIX = "instance-";
	private static final String MOVED_SUFFIX = ".moved-";
	private static final String TRASH = ".trash";

	private static volatile LocalWorkDirectory current;

	private final File root;
	private final FileChannel channel;
	private final FileLock lock;
	private final AtomicInteger discarded = new AtomicInteger();

	private LocalWorkDirectory(File root, FileChannel channel, FileLock lock) {
		this.root = root;
		this.channel = channel;
		this.lock = lock;
	}

	/**
	 * Applies the engine property at engine start. Without it, or when it cannot be used, rebuildable folders stay in
	 * the projects, as before.
	 */
	public static synchronized void init() {
		release();
		var configured = EnginePropertiesManager.getProperty(PropertyName.LOCAL_WORK_DIRECTORY);
		if (StringUtils.isBlank(configured)) {
			info("Local working directory: none, rebuildable folders stay in the projects");
			return;
		}
		// the Studio registers the locations of its projects after the engine start: their data is kept
		Predicate<String> projectExists = Engine.isStudioMode() ? projectName -> true : LocalWorkDirectory::projectExists;
		current = open(new File(configured.trim()).getAbsoluteFile(), ProductVersion.fullProductVersionID, projectExists);
	}

	/** Releases the lock of the directory, at engine stop: its content is kept for the next run. */
	public static synchronized void release() {
		if (current != null) {
			current.close();
			current = null;
		}
	}

	/** For the tests: the directory used instead of the engine property, null for none. */
	static synchronized void use(LocalWorkDirectory local) {
		release();
		current = local;
	}

	public static boolean isEnabled() {
		return current != null;
	}

	/**
	 * A rebuildable folder of the engine, kept across Convertigo versions (for instance "nodes", the Node.js
	 * distributions), created if needed.
	 *
	 * @return the folder in the local working directory, or null without local working directory
	 */
	public static File getDirectory(String name) {
		var local = current;
		if (local == null || PROJECTS.equals(name)) {
			return null;
		}
		var directory = new File(local.root, name);
		directory.mkdirs();
		return directory;
	}

	/** @return the directory of a project in the local working directory, or null without local working directory */
	public static File getProjectDirectory(String projectName) {
		var local = current;
		return local == null ? null : local.projectPath(projectName).toFile();
	}

	/**
	 * Keeps a rebuildable folder of a project in the local working directory, to call before its builder writes in
	 * it: the folder becomes a symbolic link to its place in the directory of the project, created if needed, and
	 * keeps its path for everything that uses it. A folder previously written in the project is set aside and
	 * removed in the background, to be rebuilt locally. Without local working directory, a link left by a previous
	 * configuration is removed, the folder being rebuilt in the project.
	 *
	 * @param relativePath the folder in the project, for instance "_private/ionic"
	 * @return the folder, to use as before
	 */
	public static File relocate(String projectName, File projectDir, String relativePath) {
		var folder = new File(projectDir, relativePath);
		var local = current;
		var link = folder.toPath();
		try {
			if (local == null) {
				if (Files.isSymbolicLink(link)) {
					Files.delete(link);
					info("Local working directory: " + folder + " is rebuilt in its project");
				}
			} else {
				var directory = local.projectPath(projectName);
				var own = Path.of(relativePath).getName(0).toString();
				var top = directory.resolve(own);
				if (Files.isSymbolicLink(top)) {
					// the mirror of that entry, made for another folder of the project kept here, would lead back to the
					// project: the entry becomes a folder here
					Files.delete(top);
				}
				var target = directory.resolve(relativePath);
				Files.createDirectories(target);
				mirror(projectDir.toPath(), directory, own);
				if (!Files.isSymbolicLink(link) || !Files.readSymbolicLink(link).equals(target)) {
					if (Files.isSymbolicLink(link)) {
						Files.delete(link);
					} else if (Files.exists(link, LinkOption.NOFOLLOW_LINKS)) {
						Files.move(link, link.resolveSibling(link.getFileName() + MOVED_SUFFIX + System.currentTimeMillis()));
					}
					Files.createDirectories(link.getParent());
					Files.createSymbolicLink(link, target);
					info("Local working directory: " + folder + " is kept in " + target);
				}
			}
		} catch (Exception e) {
			warn("Local working directory: unable to keep " + folder + " there, it stays in its project", e);
		}
		removeSetAside(folder);
		return folder;
	}

	/**
	 * Called before rebuilding a folder kept in the local working directory, or one of its subfolders, for instance
	 * before installing packages: marks the work in progress. A work that does not reach {@link #endWork(File)} is
	 * removed at the next startup, instead of being reused half done. Does nothing for a folder elsewhere.
	 *
	 * @return the folder
	 */
	public static File beginWork(File folder) {
		var local = current;
		var marker = local == null ? null : local.markerFor(folder);
		if (marker != null) {
			try {
				Files.createDirectories(marker.getParent());
				Files.writeString(marker, ProductVersion.fullProductVersionID);
			} catch (IOException e) {
				warn("Local working directory: unable to mark the work on " + folder, e);
			}
		}
		return folder;
	}

	/** Called after a successful {@link #beginWork(File)}: the folder can be reused after a restart. */
	public static void endWork(File folder) {
		var local = current;
		var marker = local == null ? null : local.markerFor(folder);
		if (marker != null) {
			try {
				Files.deleteIfExists(marker);
			} catch (IOException e) {
				warn("Local working directory: unable to complete the work on " + folder, e);
			}
		}
	}

	/**
	 * Removes the data of a project, deleted or reset: it is rebuilt when needed. It leaves its place at once, as the
	 * project may come back before its deletion ends, when a new version is deployed, and rebuild it there.
	 */
	public static void projectRemoved(String projectName) {
		var local = current;
		if (local != null) {
			local.discard(local.projectPath(projectName).toFile());
		}
	}

	static LocalWorkDirectory open(File configured, String version, Predicate<String> projectExists) {
		try {
			Files.createDirectories(configured.toPath());
			var opened = tryOpen(configured);
			if (opened == null) {
				var instance = new File(configured, INSTANCE_PREFIX + ProcessHandle.current().pid());
				warn("Local working directory: " + configured + " is used by another engine, this one works in " + instance, null);
				Files.createDirectories(instance.toPath());
				opened = tryOpen(instance);
				if (opened == null) {
					warn("Local working directory: " + instance + " is locked, rebuildable folders stay in the projects", null);
					return null;
				}
			} else {
				opened.removeStaleInstances();
			}
			if (!opened.supportsLinks()) {
				opened.close();
				warn("Local working directory: symbolic links are not available in " + configured + ", rebuildable folders stay in the projects", null);
				return null;
			}
			// nothing here delays the startup: what goes is renamed at once and deleted in the background
			opened.emptyTrash();
			opened.checkVersion(version);
			opened.removeInterruptedWork();
			opened.removeProjects(projectExists);
			info("Local working directory: " + opened.root);
			return opened;
		} catch (Exception e) {
			warn("Local working directory: unable to use " + configured + ", rebuildable folders stay in the projects", e);
			return null;
		}
	}

	File getRoot() {
		return root;
	}

	void close() {
		try {
			lock.release();
		} catch (Exception e) {
			// the channel closed below releases it anyway
		}
		try {
			channel.close();
		} catch (Exception e) {
			warn("Local working directory: unable to release " + root, e);
		}
	}

	private static LocalWorkDirectory tryOpen(File directory) throws IOException {
		var channel = FileChannel.open(new File(directory, LOCK).toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
		FileLock lock = null;
		try {
			lock = channel.tryLock();
		} catch (OverlappingFileLockException e) {
			// already locked by this JVM
		}
		if (lock == null) {
			channel.close();
			return null;
		}
		return new LocalWorkDirectory(directory, channel, lock);
	}

	/**
	 * The top-level entries of the project, except the one holding the relocated folder, as links in its directory
	 * here: a relative path from the folder reaches the project. Links to entries no longer in the project go.
	 */
	private static void mirror(Path projectDir, Path directory, String own) throws IOException {
		Files.createDirectories(directory);
		try (var entries = Files.list(projectDir)) {
			for (var entry : (Iterable<Path>) entries::iterator) {
				var name = entry.getFileName().toString();
				var mirrored = directory.resolve(name);
				if (name.equals(own) || name.equals(WORKING)) {
					continue;
				}
				if (Files.isSymbolicLink(mirrored)) {
					if (Files.readSymbolicLink(mirrored).equals(entry)) {
						continue;
					}
					Files.delete(mirrored);
				} else if (Files.isDirectory(mirrored, LinkOption.NOFOLLOW_LINKS)) {
					// it holds another folder of the project kept here
					continue;
				} else if (Files.exists(mirrored, LinkOption.NOFOLLOW_LINKS)) {
					warn("Local working directory: " + mirrored + " is not a link to " + entry + ", left as is", null);
					continue;
				}
				Files.createSymbolicLink(mirrored, entry);
			}
		}
		try (var mirrors = Files.list(directory)) {
			for (var mirrored : (Iterable<Path>) mirrors::iterator) {
				if (Files.isSymbolicLink(mirrored) && !Files.exists(projectDir.resolve(mirrored.getFileName()), LinkOption.NOFOLLOW_LINKS)) {
					Files.delete(mirrored);
				}
			}
		}
	}

	/** Relocated folders are symbolic links: without them (Windows without the privilege), nothing is relocated. */
	private boolean supportsLinks() {
		var test = root.toPath().resolve(".link-test");
		try {
			Files.deleteIfExists(test);
			Files.createSymbolicLink(test, root.toPath());
			return true;
		} catch (Exception e) {
			return false;
		} finally {
			try {
				Files.deleteIfExists(test);
			} catch (IOException e) {
				// best effort
			}
		}
	}

	private Path projectPath(String projectName) {
		return root.toPath().resolve(PROJECTS).resolve(projectName);
	}

	/**
	 * @return the marker of a work in progress on a folder, by its real place: in the directory of its project for a
	 *         project folder, at the root for a folder of the engine; null for a folder outside this directory
	 */
	private Path markerFor(File folder) {
		Path real, top;
		try {
			var parent = folder.getAbsoluteFile().getParentFile();
			real = parent.toPath().toRealPath().resolve(folder.getName());
			top = root.toPath().toRealPath();
		} catch (Exception e) {
			return null;
		}
		var projects = top.resolve(PROJECTS);
		if (real.startsWith(projects)) {
			if (real.getNameCount() - projects.getNameCount() < 2) {
				return null;
			}
			var relative = projects.relativize(real);
			return marker(projects.resolve(relative.getName(0)), relative.subpath(1, relative.getNameCount()));
		}
		return real.startsWith(top) && !real.equals(top) ? marker(top, top.relativize(real)) : null;
	}

	private static Path marker(Path directory, Path key) {
		return directory.resolve(WORKING).resolve(URLEncoder.encode(key.toString().replace(File.separatorChar, '/'), StandardCharsets.UTF_8));
	}

	/** Instances of engines no longer running: their lock is free. */
	private void removeStaleInstances() {
		var instances = root.listFiles(file -> file.isDirectory() && file.getName().startsWith(INSTANCE_PREFIX));
		if (instances == null) {
			return;
		}
		for (var instance : instances) {
			try {
				var stale = tryOpen(instance);
				if (stale != null) {
					stale.close();
					discard(instance);
					info("Local working directory: removed " + instance + ", left by a stopped engine");
				}
			} catch (IOException e) {
				warn("Local working directory: unable to check " + instance, e);
			}
		}
	}

	private void checkVersion(String version) throws IOException {
		var stamp = new File(root, VERSION);
		var previous = stamp.isFile() ? Files.readString(stamp.toPath(), StandardCharsets.UTF_8).trim() : null;
		if (version.equals(previous)) {
			return;
		}
		var projects = new File(root, PROJECTS);
		if (projects.exists()) {
			discard(projects);
			info("Local working directory: data of " + (previous == null ? "an unknown version" : previous) + " removed");
		}
		Files.writeString(stamp.toPath(), version, StandardCharsets.UTF_8);
	}

	private void removeInterruptedWork() {
		removeInterruptedWork(root);
		var projects = new File(root, PROJECTS).listFiles(File::isDirectory);
		if (projects != null) {
			for (var project : projects) {
				removeInterruptedWork(project);
			}
		}
	}

	private void removeInterruptedWork(File directory) {
		var markers = new File(directory, WORKING).listFiles(File::isFile);
		if (markers == null) {
			return;
		}
		for (var marker : markers) {
			var key = URLDecoder.decode(marker.getName(), StandardCharsets.UTF_8);
			var target = directory.toPath().resolve(key).normalize();
			if (target.startsWith(directory.toPath()) && !target.equals(directory.toPath())) {
				discard(target.toFile());
				info("Local working directory: removed " + target + ", interrupted during the previous run");
			}
			marker.delete();
		}
	}

	private void removeProjects(Predicate<String> projectExists) {
		var projects = new File(root, PROJECTS).listFiles(File::isDirectory);
		if (projects == null) {
			return;
		}
		for (var project : projects) {
			if (!projectExists.test(project.getName())) {
				discard(project);
				info("Local working directory: removed the data of the project " + project.getName() + ", no longer in the workspace");
			}
		}
	}

	/**
	 * Removes a folder at once, without waiting for its deletion: it goes to the trash of this directory, a rename on
	 * the same storage, and is deleted in the background. Links inside are removed without their targets.
	 */
	private void discard(File file) {
		if (!Files.exists(file.toPath(), LinkOption.NOFOLLOW_LINKS)) {
			return;
		}
		var target = new File(new File(root, TRASH), System.currentTimeMillis() + "-" + discarded.incrementAndGet() + "-" + file.getName());
		try {
			Files.createDirectories(target.getParentFile().toPath());
			Files.move(file.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE);
			deleteInBackground(target);
		} catch (IOException e) {
			deleteInBackground(file);
		}
	}

	/** What a previous run left in the trash. */
	private void emptyTrash() {
		var left = new File(root, TRASH).listFiles();
		if (left != null) {
			for (var file : left) {
				deleteInBackground(file);
			}
		}
	}

	/** The folders set aside by {@link #relocate(String, File, String)} and not removed yet. */
	private static void removeSetAside(File folder) {
		var prefix = folder.getName() + MOVED_SUFFIX;
		var parent = folder.getParentFile();
		var asides = parent == null ? null : parent.listFiles(file -> file.getName().startsWith(prefix));
		if (asides != null) {
			for (var aside : asides) {
				deleteInBackground(aside);
			}
		}
	}

	private static void deleteInBackground(File file) {
		if (Engine.logEngine != null) {
			FileUtils.deleteAsync(file);
		} else {
			FileUtils.deleteQuietly(file);
		}
	}

	private static boolean projectExists(String projectName) {
		try {
			return new File(Engine.projectDir(projectName)).isDirectory();
		} catch (Exception e) {
			return true;
		}
	}

	private static void info(String message) {
		if (Engine.logEngine != null) {
			Engine.logEngine.info(message);
		}
	}

	private static void warn(String message, Throwable t) {
		if (Engine.logEngine != null) {
			Engine.logEngine.warn(message, t);
		}
	}
}
