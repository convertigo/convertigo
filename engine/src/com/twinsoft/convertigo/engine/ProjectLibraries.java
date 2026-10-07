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
import java.lang.management.ManagementFactory;
import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.engine.util.FileUtils;
import com.twinsoft.convertigo.engine.util.Log4jHelper;

/**
 * The class paths of the projects, made of their libraries (libs/*.jar, libs/classes and the classes compiled from
 * libs/src, with those of the projects they reference) and searched before the engine (see {@link ProjectClassLoader}
 * and {@link ProjectSources}).
 * <p>
 * A project uses one generation of its class path at a time: an immutable class loader over a copy of its libraries.
 * When the libraries change, the next generation is built, in the background on a server, before answering in the
 * Studio where a library is replaced to be used at once, and it replaces the current one for the next requests. The
 * running requests finish with the generation they started with. A project loaded again with the same libraries keeps
 * its generation. When its sources do not compile, a project keeps its current generation, or gets one without the
 * classes of its sources.
 * <p>
 * A replaced generation is retired: once its resources are no longer in use, or after a time limit, the engine releases
 * what it keeps for it (see {@link Holder}), and the garbage collector reclaims it when nothing else uses it, so an
 * object kept in a session keeps working with its classes. A generation that stays reachable is reported.
 */
public final class ProjectLibraries {
	/** What the engine keeps for a generation, released once it is retired. */
	public interface Holder {
		/** @return whether something kept for the generation is still in use, for instance a pooled connection */
		boolean inUse(ProjectClassLoader generation);

		/** Releases what is kept for the generation. */
		void release(ProjectClassLoader generation);
	}

	static long checkPeriod = 5_000;
	static long releaseGrace = 30_000;
	static long releaseLimit = 600_000;
	static long leakDelay = 600_000;
	static long removedDelay = 30_000;
	static long sweepPeriod = 15_000;
	/** For the tests: runs in a background build, once the libraries are copied, before the new generation is used. */
	static Runnable beforeSwap;
	/** For the tests: receives the reports of the generations that stay reachable. */
	static Consumer<String> reporter;

	private static final Map<String, Classpath> classpaths = new ConcurrentHashMap<>();
	private static final Map<String, Object> locks = new ConcurrentHashMap<>();
	private static final List<Retired> retired = new CopyOnWriteArrayList<>();
	private static final List<Holder> holders = new CopyOnWriteArrayList<>();
	private static ScheduledExecutorService executor;
	private static ScheduledFuture<?> sweeper;

	private ProjectLibraries() {
	}

	/** @return the current generation of the class path of a project, built if needed */
	public static ClassLoader classLoader(Project project) {
		return classLoader(project.getName(), project::getLibrariesDirectories, project.getVersion(), Engine.isStudioMode());
	}

	/**
	 * @param dirs the libs folders of the project and of those it references
	 * @param synchronous to build a new generation before answering, instead of in the background
	 */
	static ProjectClassLoader classLoader(String project, Supplier<List<File>> dirs, String version, boolean synchronous) {
		return classpaths.computeIfAbsent(project, Classpath::new).loader(dirs, version == null ? "" : version, synchronous);
	}

	/**
	 * For the export of a project: the classes compiled from its own libs/src in its current generation, with the copy of
	 * the sources they are compiled from.
	 *
	 * @return the classes folder and the sources folder, or null when the generation has no classes of these sources
	 */
	public static File[] compiledSources(Project project) {
		if (!(classLoader(project) instanceof ProjectClassLoader generation) || generation.getSnapshot() == null) {
			return null;
		}
		var sources = new File(generation.getSnapshot(), "source-0/" + ClasspathSnapshot.SOURCES);
		var classes = new File(sources.getPath() + ClasspathSnapshot.COMPILED_SUFFIX);
		return sources.isDirectory() && classes.isDirectory() ? new File[] { classes, sources } : null;
	}

	/** @return the fingerprint of the content of a src folder, written next to the classes compiled from it */
	public static String sourcesFingerprint(File sources) throws java.io.IOException {
		return ClasspathSnapshot.sourcesFingerprint(sources);
	}

	/** The libraries of a project may have changed (references edited, project loaded again): checked at its next use. */
	public static void checkAtNextUse(String project) {
		var classpath = classpaths.get(project);
		if (classpath != null) {
			classpath.nextCheck = 0;
		}
	}

	/** A project removed from the engine: its generation is retired, unless it comes back soon, as a new version. */
	public static void projectRemoved(String project) {
		var classpath = classpaths.get(project);
		if (classpath != null) {
			classpath.removedAt = System.currentTimeMillis();
			ensureSweeper();
		}
	}

	public static void addHolder(Holder holder) {
		holders.add(holder);
	}

	public static void removeHolder(Holder holder) {
		holders.remove(holder);
	}

	/** At engine stop: the generations are forgotten, the holders release what they keep themselves. */
	public static void stop() {
		ScheduledExecutorService toStop;
		synchronized (ProjectLibraries.class) {
			toStop = executor;
			executor = null;
			sweeper = null;
		}
		if (toStop != null) {
			toStop.shutdownNow();
		}
		classpaths.clear();
		retired.clear();
	}

	private static final class Classpath {
		private final String name;
		private final AtomicBoolean checking = new AtomicBoolean();
		private volatile ProjectClassLoader current;
		private volatile String stamp;
		private volatile long nextCheck;
		private volatile long removedAt;
		/** the current generation lacks the classes of sources this engine did not compile, as it did not build */
		private volatile boolean waitingForBuild;

		private Classpath(String name) {
			this.name = name;
		}

		private ProjectClassLoader loader(Supplier<List<File>> dirs, String version, boolean synchronous) {
			removedAt = 0;
			var loader = current;
			if (loader == null) {
				synchronized (this) {
					if (current == null) {
						nextCheck = System.currentTimeMillis() + checkPeriod;
						check(dirs, version, false);
					}
					if (current == null) {
						throw new IllegalStateException("Unable to prepare the libraries of the project " + name);
					}
					return current;
				}
			}
			var now = System.currentTimeMillis();
			if (now >= nextCheck) {
				nextCheck = now + checkPeriod;
				if (synchronous) {
					check(dirs, version, false);
				} else if (!checking.get()) {
					executor().execute(() -> {
						Log4jHelper.mdcClear();
						check(dirs, version, true);
					});
				}
			}
			return current;
		}

		/** Builds the next generation if the libraries changed since the current one. */
		private void check(Supplier<List<File>> dirs, String version, boolean background) {
			if (!checking.compareAndSet(false, true)) {
				return;
			}
			try {
				var directories = dirs.get();
				var entries = ClasspathSnapshot.entries(directories);
				var newStamp = ClasspathSnapshot.stamp(directories, entries);
				var mayCompile = ProjectSources.mayCompile();
				if (newStamp.equals(stamp) && !(waitingForBuild && mayCompile)) {
					return;
				}
				var fingerprint = ClasspathSnapshot.fingerprint(entries);
				var previous = current;
				if (previous != null && (fingerprint.equals(previous.getFingerprint())
						|| (fingerprint + ClasspathSnapshot.WITHOUT_SOURCES).equals(previous.getFingerprint()) && !mayCompile)) {
					// files touched, same content
					stamp = newStamp;
					return;
				}
				synchronized (lock(name)) {
					File snapshot = null;
					File snapshots = null;
					var generationFingerprint = fingerprint;
					if (!entries.isEmpty()) {
						snapshots = snapshots(name);
						try {
							snapshot = ClasspathSnapshot.prepare(name, entries, snapshots, fingerprint,
									ProjectSources.Compilation.of(mayCompile));
						} catch (ProjectSources.CompilationException e) {
							// not compiled again until the libraries change
							stamp = newStamp;
							error("Libraries of the project " + name + ": " + e.getMessage() + (previous != null
									? "\nThe project keeps its previous libraries."
									: "\nThe project uses its libraries without the classes of these sources."));
							if (previous != null) {
								return;
							}
							snapshot = ClasspathSnapshot.prepare(name, entries, snapshots, fingerprint, ProjectSources.Compilation.FAILED);
						}
						generationFingerprint = snapshot.getName();
					}
					var next = new ProjectClassLoader(ClasspathSnapshot.urls(entries, snapshot), engineLoader(), name, version,
							generationFingerprint, snapshot);
					if (background && beforeSwap != null) {
						beforeSwap.run();
					}
					current = next;
					stamp = newStamp;
					waitingForBuild = !mayCompile && generationFingerprint.endsWith(ClasspathSnapshot.WITHOUT_SOURCES);
					if (previous != null) {
						// before the cleaning: the running requests still read its copy
						retire(previous);
					}
					if (snapshots != null) {
						if (isLocal(snapshots)) {
							cleanSnapshots(name, snapshots);
						} else {
							ClasspathSnapshot.markObsolete(snapshots.getParentFile(), snapshots, snapshot);
						}
					}
					if (snapshot != null || previous != null) {
						info("Libraries of the project " + name + ": " + next
								+ (previous == null ? "" : " replaces " + previous + ", which is retired"));
					}
				}
			} catch (Exception e) {
				// checked again later, as the stamp is unchanged
				warn("Libraries of the project " + name + ": unable to use the new libraries"
						+ (current == null ? "" : ", the project keeps the previous ones"), e);
			} finally {
				checking.set(false);
			}
		}
	}

	/** A replaced generation, kept until released, then followed until collected. */
	private static final class Retired {
		private final String project;
		private final String description;
		private final File snapshot;
		private final long retiredAt = System.currentTimeMillis();
		private final WeakReference<ProjectClassLoader> reference;
		private volatile ProjectClassLoader generation;
		private long releasedAt;
		private long collectionsAtRelease;
		private boolean reported;

		private Retired(ProjectClassLoader generation) {
			this.project = generation.getProject();
			this.description = generation.toString();
			this.snapshot = generation.getSnapshot();
			this.reference = new WeakReference<>(generation);
			this.generation = generation;
		}
	}

	private static void retire(ProjectClassLoader generation) {
		retired.add(new Retired(generation));
		ensureSweeper();
	}

	/** Releases the retired generations no longer in use, then follows them until they are collected. */
	static void sweep() {
		var now = System.currentTimeMillis();
		for (var entry : classpaths.entrySet()) {
			var classpath = entry.getValue();
			var removedAt = classpath.removedAt;
			if (removedAt > 0 && now - removedAt >= removedDelay && classpaths.remove(entry.getKey(), classpath)) {
				var current = classpath.current;
				if (current != null) {
					info("Libraries of the project " + entry.getKey() + ": the project is removed, " + current + " is retired");
					retire(current);
				}
			}
		}
		for (var item : retired) {
			var generation = item.generation;
			if (generation != null) {
				var age = now - item.retiredAt;
				if (age >= releaseGrace && (age >= releaseLimit || !inUse(generation))) {
					for (var holder : holders) {
						try {
							holder.release(generation);
						} catch (Exception e) {
							warn("Libraries of the project " + item.project + ": unable to release " + item.description, e);
						}
					}
					item.generation = null;
					item.releasedAt = now;
					item.collectionsAtRelease = oldCollections();
					info("Libraries of the project " + item.project + ": " + item.description
							+ " is released, what the engine kept for it is closed");
				}
			} else if (item.reference.get() == null) {
				retired.remove(item);
				deleteSnapshot(item);
			} else if (!item.reported && now - item.releasedAt >= leakDelay && oldCollections() > item.collectionsAtRelease) {
				item.reported = true;
				report(item);
			}
		}
		synchronized (ProjectLibraries.class) {
			if (sweeper != null && retired.isEmpty() && classpaths.values().stream().noneMatch(c -> c.removedAt > 0)) {
				sweeper.cancel(false);
				sweeper = null;
			}
		}
	}

	private static boolean inUse(ProjectClassLoader generation) {
		for (var holder : holders) {
			try {
				if (holder.inUse(generation)) {
					return true;
				}
			} catch (Exception e) {
				// released anyway at the time limit
			}
		}
		return false;
	}

	private static void report(Retired item) {
		var loader = item.reference.get();
		var threads = loader == null ? "" : Thread.getAllStackTraces().keySet().stream()
				.filter(thread -> thread.getContextClassLoader() == loader).map(Thread::getName).limit(5)
				.collect(Collectors.joining(", "));
		var message = "Libraries of the project " + item.project + ": " + item.description
				+ " was replaced but is still in memory: something keeps its classes, such as a static field of the"
				+ " engine, a thread started by the project, or an object kept in a session or a shared project map"
				+ (threads.isEmpty() ? "" : "; threads using it: " + threads);
		if (reporter != null) {
			reporter.accept(message);
		}
		warn(message, null);
	}

	/** @return the number of collections of the old generation, which can unload classes, since the JVM start */
	private static long oldCollections() {
		long count = 0;
		for (var collector : ManagementFactory.getGarbageCollectorMXBeans()) {
			var name = collector.getName().toLowerCase();
			if (!(name.contains("young") || name.contains("minor") || name.contains("scavenge") || name.contains("parnew")
					|| name.equals("copy"))) {
				count += Math.max(0, collector.getCollectionCount());
			}
		}
		return count;
	}

	/**
	 * @return the folder of the copies of the libraries of a project: in the local working directory when the engine
	 *         has one, else in the workspace, shared with the other instances
	 */
	private static File snapshots(String project) {
		var local = LocalWorkDirectory.getDirectory("libs");
		if (local != null) {
			return new File(local, project);
		}
		return new File(new File(Engine.USER_WORKSPACE_PATH, "libs/" + project), "snapshots");
	}

	private static boolean isLocal(File snapshots) {
		var local = LocalWorkDirectory.getDirectory("libs");
		return local != null && snapshots.toPath().startsWith(local.toPath());
	}

	/** @return the copies used by the generations of a project still alive */
	private static Set<File> liveSnapshots(String project) {
		var live = new HashSet<File>();
		var classpath = classpaths.get(project);
		var current = classpath == null ? null : classpath.current;
		if (current != null && current.getSnapshot() != null) {
			live.add(current.getSnapshot());
		}
		for (var item : retired) {
			if (item.project.equals(project) && item.snapshot != null && item.reference.get() != null) {
				live.add(item.snapshot);
			}
		}
		return live;
	}

	/** In the local working directory: the copies no generation uses, or left by an interrupted build, go. */
	private static void cleanSnapshots(String project, File snapshots) {
		var live = liveSnapshots(project);
		var children = snapshots.listFiles(File::isDirectory);
		if (children != null) {
			for (var child : children) {
				if (!live.contains(child)) {
					FileUtils.deleteQuietly(child);
				}
			}
		}
	}

	private static void deleteSnapshot(Retired item) {
		if (item.snapshot == null || !isLocal(item.snapshot)) {
			return;
		}
		synchronized (lock(item.project)) {
			if (!liveSnapshots(item.project).contains(item.snapshot)) {
				FileUtils.deleteQuietly(item.snapshot);
			}
		}
	}

	private static Object lock(String project) {
		return locks.computeIfAbsent(project, name -> new Object());
	}

	private static ClassLoader engineLoader() {
		var loader = Engine.getEngineClassLoader();
		return loader != null ? loader : ProjectLibraries.class.getClassLoader();
	}

	private static synchronized ScheduledExecutorService executor() {
		if (executor == null) {
			executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
				var thread = new Thread(runnable, "ProjectLibraries");
				thread.setDaemon(true);
				// a thread keeps its context class loader: never a generation
				thread.setContextClassLoader(ProjectLibraries.class.getClassLoader());
				return thread;
			});
		}
		return executor;
	}

	private static synchronized void ensureSweeper() {
		if (sweeper == null || sweeper.isDone()) {
			sweeper = executor().scheduleWithFixedDelay(() -> {
				try {
					Log4jHelper.mdcClear();
					sweep();
				} catch (Throwable t) {
					warn("Libraries of the projects: unable to release the retired generations", t);
				}
			}, sweepPeriod, sweepPeriod, TimeUnit.MILLISECONDS);
		}
	}

	private static void info(String message) {
		if (Engine.logEngine != null) {
			Engine.logEngine.info(message);
		}
	}

	private static void error(String message) {
		if (Engine.logEngine != null) {
			Engine.logEngine.error(message);
		}
	}

	private static void warn(String message, Throwable t) {
		if (Engine.logEngine != null) {
			Engine.logEngine.warn(message, t);
		}
	}

	/** For the tests: the generations replaced and not yet collected. */
	static int retiredCount() {
		return retired.size();
	}

	/** For the tests: forgets everything. */
	static void reset() {
		stop();
		locks.clear();
		holders.clear();
	}
}
