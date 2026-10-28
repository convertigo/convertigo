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

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.JSDescriptor;
import org.mozilla.javascript.JSScript;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.optimizer.ClassCompiler;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.LocalWorkDirectory;

/**
 * Disk cache of the classes Rhino generates for a script in compiled mode: after a restart, a script found here is
 * loaded without being parsed nor generated again, which saves CPU and I/O at startup for some disk space. A script
 * gives the same result whether it comes from here or from {@link Context#compileString}: the classes are those
 * Rhino generates for it, written by its {@link ClassCompiler} (patched, see the patches project).
 * <p>
 * <b>How</b>: a script not found here is compiled by {@link Context#compileString}, as before, and used at once; its
 * classes are generated again by the ClassCompiler and written by a background thread, once the compilations of the
 * startup are over (no compilation for 2 s, 2 minutes at most), so that this second compilation does not slow down the
 * startup that needs the script.
 * <p>
 * <b>What is cached</b>: only the classes generated from a source, keyed by the version of Rhino and of its patch, the
 * format of this cache, the compilation options of the context (language version, mode...), the name of the source
 * and the source. Never a value: no exports, scope, request, session or secret. Only the code of a file of a project
 * (an origin is given) is written here; a source without origin, such as a draft checked while authoring, is compiled
 * in memory as before.
 * <p>
 * <b>Where</b>: in {@value #FOLDER} of the project holding the source (the nearest folder with a c8oProject.yaml above
 * its file), like the build of a mobile application: with a local working directory (engine property
 * local_work.directory), this folder is a link to the local disk of the instance, never the shared workspace. One file
 * per source, written to a temporary file then renamed: threads and JVMs sharing the folder never read a partial
 * entry. The folder is bounded ({@link #DEFAULT_MAX_BYTES}): the entries read least recently go first.
 * <p>
 * <b>Security</b>: these classes are executed, and {@code _private} belongs to its project. Each entry is signed with
 * an HMAC-SHA256 keyed by a secret of the installation, kept in the configuration folder of the engine, outside the
 * projects: an entry planted in a project (a copy of the folder, a Git repository; project archives never carry
 * {@code _private}) or damaged (truncated, another version, another source) fails the check and is silently compiled
 * again and replaced. Code able to write files in the JVM (a project script using Java) can run anything anyway:
 * this cache gives it nothing more.
 * <p>
 * <b>Activation</b>: engine property javascript.bytecode_cache. {@code off}: never; {@code on}: always, even outside
 * a running engine (tests, measures); {@code auto} (default): in a running engine, and as long as it pays off. In auto,
 * the JVM compares, for the scripts it loads from here, the cost of loading them (key, read, check, classes defined,
 * script ready) to the cost of compiling them with Context.compileString (kept in their entry when written: the first
 * compilation of the source in the JVM that wrote it, the one a restart saves, not a later one after in-memory caches
 * were cleared, with a warm JIT). Both are the processor time of the thread, which the other threads of a startup do
 * not inflate, except the read of the entry, counted in elapsed time since a slow storage costs in waits. Once
 * {@value #AUTO_MIN_SAMPLES} scripts and {@value #AUTO_MIN_COMPILE_MILLIS} ms of compilation are measured, if loading
 * costs more than {@value #AUTO_MAX_LOAD_PERCENT}% of compiling, the cache is turned off, reading and writing, for the
 * rest of the life of the JVM. Both are measured when they happen, without extra work. Loading takes about a third
 * of compiling on a local disk whatever the processor, up to a half on a shared folder of a container, and about twice
 * compiling on a network file system: the threshold keeps the cache where it saves at least a quarter of the
 * compilation at startup, which repays the second compilation of the background after four starts. The verdict
 * depends on the machine and on the storage, so it stays in memory: it is never written in the project, which other
 * instances may share.
 */
public final class RhinoBytecodeCache {
	/** The folder of the cache in its project, relocated with the local working directory. */
	public static final String FOLDER = "_private/rhino-bytecode";

	/** auto: the cache goes off when loading costs more than this share of compiling. */
	static final int AUTO_MAX_LOAD_PERCENT = 75;
	/** auto: scripts loaded before a verdict. */
	static final int AUTO_MIN_SAMPLES = 16;
	/** auto: compilation time those scripts must represent before a verdict, in milliseconds. */
	static final int AUTO_MIN_COMPILE_MILLIS = 200;

	/** Size of a cache folder above which the entries read least recently are removed. */
	static final long DEFAULT_MAX_BYTES = 128L << 20;

	static final int FORMAT = 1;
	private static final byte[] MAGIC = { 'C', '8', 'O', 'R', 'B', 'C', '\n', FORMAT };
	private static final String SUFFIX = ".rbc";
	private static final String TEMPORARY = ".tmp";
	private static final String KEY_FILE = "rhino-bytecode.key";
	private static final int MAC_LENGTH = 32;
	private static final long MAX_ENTRY_BYTES = 64L << 20;
	private static final long TOUCH_AFTER_MILLIS = TimeUnit.DAYS.toMillis(1);
	private static final long STALE_TEMPORARY_MILLIS = TimeUnit.HOURS.toMillis(1);
	private static final int SCAN_EVERY_WRITES = 32;
	static final byte KIND_CLASSES = 0;
	/** The source is compiled by Context.compileString, for instance by its interpreter beyond class file limits. */
	private static final byte KIND_NOT_CACHEABLE = 1;

	public enum Mode {
		auto, on, off;

		static Mode parse(String value) {
			if (value != null) {
				switch (value.trim().toLowerCase()) {
				case "on": case "true": return on;
				case "off": case "false": return off;
				default: break;
				}
			}
			return auto;
		}
	}

	/** Thrown for an entry that cannot be used: it is compiled again and replaced. */
	static final class InvalidEntry extends Exception {
		private static final long serialVersionUID = 1L;

		InvalidEntry(String reason) {
			super(reason, null, false, false);
		}
	}

	/** The verdict of auto for this JVM. */
	static final class Measure {
		private int samples;
		private long loadNanos, compileNanos;
		private boolean reported;
		private volatile boolean off;

		boolean isOff() {
			return off;
		}

		/** A script loaded from the cache in loadNanos, that took compileNanos to compile when its entry was written. */
		synchronized void loaded(long loadNanos, long compileNanos) {
			if (off || compileNanos <= 0 || loadNanos < 0) {
				return;
			}
			samples++;
			this.loadNanos += loadNanos;
			this.compileNanos += compileNanos;
			if (samples < AUTO_MIN_SAMPLES || this.compileNanos < TimeUnit.MILLISECONDS.toNanos(AUTO_MIN_COMPILE_MILLIS)) {
				return;
			}
			if (this.loadNanos * 100 > this.compileNanos * AUTO_MAX_LOAD_PERCENT) {
				off = true;
				warn("JavaScript bytecode cache: turned off for this run, loading takes " + summary() + " (auto keeps it below "
						+ AUTO_MAX_LOAD_PERCENT + "%): the projects may be on a slow storage, see the local_work.directory engine property");
			} else if (!reported) {
				reported = true;
				info("JavaScript bytecode cache: kept, loading takes " + summary());
			}
		}

		private String summary() {
			return (loadNanos * 100 / compileNanos) + "% of compiling (" + samples + " scripts loaded in "
					+ TimeUnit.NANOSECONDS.toMillis(loadNanos) + " ms, compiled in " + TimeUnit.NANOSECONDS.toMillis(compileNanos) + " ms)";
		}

		synchronized String state() {
			return "samples=" + samples + ", loadMs=" + TimeUnit.NANOSECONDS.toMillis(loadNanos) + ", compileMs="
					+ TimeUnit.NANOSECONDS.toMillis(compileNanos) + ", off=" + off;
		}

		/** @return whether enough scripts were measured for a verdict */
		synchronized boolean verdict() {
			return samples >= AUTO_MIN_SAMPLES && compileNanos >= TimeUnit.MILLISECONDS.toNanos(AUTO_MIN_COMPILE_MILLIS);
		}
	}

	/** Counters of this JVM, whatever the mode, for diagnostics and tests. */
	static final class Statistics {
		final AtomicLong hits = new AtomicLong(), misses = new AtomicLong(), invalid = new AtomicLong(),
				writes = new AtomicLong(), evictions = new AtomicLong(), bypasses = new AtomicLong();
		/** time to load the scripts found here, time their entries say they took to compile, time to compile the others */
		final AtomicLong loadNanos = new AtomicLong(), loadedCompileNanos = new AtomicLong(), compileNanos = new AtomicLong();
		/** what auto compares to the compilation: processor time, and the time of the reads */
		final AtomicLong loadCostNanos = new AtomicLong();
		/** parts of the loading time: the key of the source, reading the entry, checking it, defining its classes */
		final AtomicLong keyNanos = new AtomicLong(), readNanos = new AtomicLong(), checkNanos = new AtomicLong(), defineNanos = new AtomicLong();

		@Override
		public String toString() {
			return "hits=" + hits + ", misses=" + misses + ", invalid=" + invalid + ", writes=" + writes + ", evictions="
					+ evictions + ", bypasses=" + bypasses + ", loadMs=" + loadNanos.get() / 1_000_000 + " (key "
					+ keyNanos.get() / 1_000_000 + ", read " + readNanos.get() / 1_000_000 + ", check " + checkNanos.get() / 1_000_000
					+ ", define " + defineNanos.get() / 1_000_000 + "), loadCostMs=" + loadCostNanos.get() / 1_000_000
					+ ", loadedCompileMs=" + loadedCompileNanos.get() / 1_000_000 + ", compileMs=" + compileNanos.get() / 1_000_000;
		}
	}

	private static final String ENVIRONMENT = environment();
	private static final ConcurrentHashMap<String, RhinoBytecodeCache> caches = new ConcurrentHashMap<>();
	/**
	 * The compilation time of the entries waiting to be written, by key: a source compiled again before its entry is
	 * written (in-memory caches cleared during the startup) keeps the time of its first compilation, the one a restart
	 * saves, and is written once.
	 */
	private static final ConcurrentHashMap<String, AtomicLong> pending = new ConcurrentHashMap<>();
	/** For the tests: behaves as in a running engine. */
	static volatile boolean runningEngineForTests;
	private static final Measure measure = new Measure();
	static final Statistics statistics = new Statistics();
	private static final AtomicInteger temporaryCounter = new AtomicInteger();
	private static final Object keyLock = new Object();
	private static volatile byte[] installationKey;
	private static volatile boolean flushOnExit;
	/** when a script was last compiled, and how many waits for the writes are in progress */
	private static volatile long lastCompiled;
	private static final AtomicInteger flushing = new AtomicInteger();
	/** the classes of a compiled script are generated once no script was compiled for this time, after startup */
	static final long QUIET_NANOS = TimeUnit.SECONDS.toNanos(2);
	/** but no later than this time after its compilation */
	static final long MAX_DEFER_NANOS = TimeUnit.MINUTES.toNanos(2);
	private static final ThreadPoolExecutor writer = new ThreadPoolExecutor(0, 1, 5, TimeUnit.SECONDS,
			new LinkedBlockingQueue<>(4096), runnable -> {
				var thread = new Thread(runnable, "JavaScript bytecode cache writer");
				thread.setDaemon(true);
				return thread;
			}, new ThreadPoolExecutor.DiscardPolicy());

	private final File projectRoot;
	private final String projectName;
	private final long maxBytes;
	private final byte[] macKey;
	private final Object scanLock = new Object();
	private volatile Path folder;
	/** after a failure to create the folder (read-only project), no new attempt before this time */
	private volatile long unavailableUntil;
	private int writesSinceScan = -1;

	/** A cache in a folder of a project, relocated with the local working directory. */
	private RhinoBytecodeCache(File projectRoot, String projectName, byte[] macKey) {
		this.projectRoot = projectRoot;
		this.projectName = projectName;
		this.maxBytes = DEFAULT_MAX_BYTES;
		this.macKey = macKey;
	}

	/** For the tests: a cache in this folder. */
	RhinoBytecodeCache(Path folder, long maxBytes, byte[] macKey) {
		this.projectRoot = null;
		this.projectName = null;
		this.folder = folder;
		this.maxBytes = maxBytes;
		this.macKey = macKey;
	}

	/**
	 * Compiles a script as {@code cx.compileString(source, sourceName, 1, null)} does, through the disk cache of the
	 * project holding origin when it is enabled.
	 *
	 * @param origin the file of the source, or any file or folder of its project; null for a source of no file (a draft,
	 *            generated code): compiled in memory only
	 */
	public static Script compile(Context cx, String source, String sourceName, File origin) {
		var mode = mode();
		var cache = origin == null ? null : forOrigin(cx, origin, mode);
		if (cache == null) {
			return cx.compileString(source, sourceName, 1, null);
		}
		return cache.get(cx, source, sourceName, mode == Mode.auto ? measure : null);
	}

	/** @return an absolute path as the file of a source, else null (a source of no file) */
	public static File originOf(String sourceName) {
		if (sourceName == null || sourceName.isEmpty()) {
			return null;
		}
		var file = new File(sourceName);
		return file.isAbsolute() ? file : null;
	}

	/** @return the mode of the engine property, auto when it cannot be read */
	public static Mode mode() {
		String value;
		try {
			value = EnginePropertiesManager.getProperty(PropertyName.JAVASCRIPT_BYTECODE_CACHE);
		} catch (Exception notInitialized) {
			value = System.getProperty("convertigo.engine." + PropertyName.JAVASCRIPT_BYTECODE_CACHE.getKey());
		}
		return Mode.parse(value);
	}

	/** @return the verdict of auto and the counters of this JVM, for diagnostics */
	public static String state() {
		return "mode=" + mode() + ", environment=" + ENVIRONMENT + ", " + measure.state() + ", " + statistics;
	}

	/** @return the cache of the project holding origin, null when the disk cache does not apply */
	private static RhinoBytecodeCache forOrigin(Context cx, File origin, Mode mode) {
		if (ENVIRONMENT == null || cx.isInterpretedMode() || cx.getDebugger() != null) {
			return null;
		}
		if (mode == Mode.off || mode == Mode.auto && (!inRunningEngine() || measure.isOff())) {
			return null;
		}
		var root = projectRootAbove(origin);
		if (root == null) {
			return null;
		}
		var cache = caches.get(root.getPath());
		if (cache == null) {
			var key = installationKey();
			if (key == null) {
				return null;
			}
			cache = caches.computeIfAbsent(root.getPath(), path -> new RhinoBytecodeCache(root, projectName(root), key));
		}
		return cache;
	}

	private static boolean inRunningEngine() {
		return Engine.theApp != null || runningEngineForTests;
	}

	/** The nearest folder with a c8oProject.yaml above the file, as the frontend builds find their project. */
	static File projectRootAbove(File file) {
		var current = file.getAbsoluteFile();
		for (int depth = 0; current != null && depth < 8; current = current.getParentFile(), depth++) {
			if (new File(current, "c8oProject.yaml").isFile()) {
				try {
					return current.getCanonicalFile();
				} catch (IOException e) {
					return current;
				}
			}
		}
		return null;
	}

	/** The name of the project of a folder: its name, or in the Studio the one registered for its location. */
	private static String projectName(File root) {
		try {
			if (Engine.theApp != null && Engine.theApp.databaseObjectsManager != null && Engine.isStudioMode()) {
				for (var entry : Engine.theApp.databaseObjectsManager.getStudioProjects().getProjects(false).entrySet()) {
					if (root.equals(entry.getValue().getCanonicalFile())) {
						return entry.getKey();
					}
				}
			}
		} catch (Exception e) {
			// the name of the folder
		}
		return root.getName();
	}

	/** The compilation options of the context that change the generated classes, null when they cannot be cached. */
	static String options(Context cx, CompilerEnvirons env) {
		if (env.isInterpretedMode() || env.securityController() != null || env.getActivationNames() != null
				|| cx.getDebugger() != null) {
			return null;
		}
		return "lang=" + env.getLanguageVersion() + ";debug=" + env.isGenerateDebugInfo() + ";source=" + env.isGeneratingSource()
				+ ";javaAccess=" + cx.hasFeature(Context.FEATURE_ENHANCED_JAVA_ACCESS)
				+ ";reserved=" + env.isReservedKeywordAsIdentifier() + ";member=" + env.isAllowMemberExprAsFunctionName()
				+ ";strict=" + env.isStrictMode() + ";warningAsError=" + env.reportWarningAsError() + ";e4x=" + env.isXmlAvailable()
				+ ";observer=" + env.isGenerateObserverCount();
	}

	/** The key of a source: its signature, in hexadecimal. */
	static String key(String options, String sourceName, String source) {
		try {
			var md = MessageDigest.getInstance("SHA-256");
			md.update((ENVIRONMENT + "\n" + options + "\n" + sourceName.length() + ":" + sourceName + "\n").getBytes(StandardCharsets.UTF_8));
			md.update(source.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(md.digest());
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	/** The name of the classes of a source: as Rhino names them, with the key instead of a counter. */
	static String mainClassName(String sourceName, String key) {
		var base = sourceName.replaceAll("\\W", "_");
		if (base.length() > 80) {
			base = base.substring(base.length() - 80);
		}
		if (base.isEmpty() || !Character.isJavaIdentifierStart(base.charAt(0))) {
			base = "_" + base;
		}
		return "org.mozilla.javascript.gen." + base + "_" + key.substring(0, 20);
	}

	/**
	 * The script of a source: loaded from this folder; else compiled by Context.compileString as before, its classes
	 * being generated again and written in the background, for the next runs.
	 */
	Script get(Context cx, String source, String sourceName, Measure measure) {
		var started = System.nanoTime();
		var startedCpu = cpuTime();
		var env = new CompilerEnvirons();
		env.initFromContext(cx);
		var options = ENVIRONMENT == null ? null : options(cx, env);
		if (options == null) {
			statistics.bypasses.incrementAndGet();
			return cx.compileString(source, sourceName, 1, null);
		}
		var key = key(options, sourceName, source);
		var mainClassName = mainClassName(sourceName, key);
		var file = entryFile(key);
		var keyed = System.nanoTime();
		Entry entry = null;
		if (file != null) {
			try {
				entry = read(file, key, mainClassName);
			} catch (InvalidEntry e) {
				// damaged, foreign or outdated: compiled again and replaced
				statistics.invalid.incrementAndGet();
				debug("JavaScript bytecode cache: entry of " + sourceName + " replaced (" + e.getMessage() + ")");
			}
		}
		if (entry != null && entry.kind == KIND_NOT_CACHEABLE) {
			statistics.hits.incrementAndGet();
			return cx.compileString(source, sourceName, 1, null);
		}
		if (entry != null) {
			try {
				var defining = System.nanoTime();
				var script = instantiate(cx, mainClassName, entry.classes);
				var loaded = System.nanoTime();
				var loadNanos = loaded - started;
				// what loading costs: the processor time of the thread, but the time the entry took to be read, a slow
				// storage costs in waits
				var loadCost = cpuTime() - startedCpu - entry.readCpuNanos + entry.readNanos;
				statistics.keyNanos.addAndGet(keyed - started);
				statistics.readNanos.addAndGet(entry.readNanos);
				statistics.checkNanos.addAndGet(defining - keyed - entry.readNanos);
				statistics.defineNanos.addAndGet(loaded - defining);
				statistics.hits.incrementAndGet();
				statistics.loadNanos.addAndGet(loadNanos);
				statistics.loadCostNanos.addAndGet(loadCost);
				statistics.loadedCompileNanos.addAndGet(entry.compileNanos);
				if (measure != null) {
					measure.loaded(loadCost, entry.compileNanos);
				}
				touch(file, entry.lastModified);
				return script;
			} catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
				// signed by this installation but not loadable: the same classes would fail again, the source is no
				// longer cached until it changes
				statistics.invalid.incrementAndGet();
				warn("JavaScript bytecode cache: the classes of " + sourceName + " cannot be loaded, it is compiled at each start (" + e + ")");
				var script = cx.compileString(source, sourceName, 1, null);
				schedule(key, mainClassName, KIND_NOT_CACHEABLE, 0, null, null, null, false, measure);
				return script;
			}
		}
		statistics.misses.incrementAndGet();
		lastCompiled = System.nanoTime();
		var compilingCpu = cpuTime();
		// as before, with its errors; or its interpreter beyond the class file limits
		var script = cx.compileString(source, sourceName, 1, null);
		// the processor time of the compilation: what a restart saves, whatever the other threads of a startup do
		var compileNanos = cpuTime() - compilingCpu;
		statistics.compileNanos.addAndGet(compileNanos);
		schedule(key, mainClassName, KIND_CLASSES, compileNanos, env, source, sourceName,
				cx.hasFeature(Context.FEATURE_ENHANCED_JAVA_ACCESS), measure);
		return script;
	}

	/** @return the file of an entry, null when the folder of the cache is not available */
	private Path entryFile(String key) {
		var directory = folder(false);
		return directory == null ? null : directory.resolve(key + SUFFIX);
	}

	/**
	 * The folder of the cache, relocated to the local working directory once per run as the folders of the builds, and
	 * again when its place there was removed (project deleted or reset).
	 */
	private Path folder(boolean create) {
		var directory = folder;
		if (projectRoot == null) {
			if (create && directory != null) {
				try {
					Files.createDirectories(directory);
				} catch (IOException e) {
					return null;
				}
			}
			return directory;
		}
		if (directory != null && (!create || Files.isDirectory(directory))) {
			return directory;
		}
		if (create && System.currentTimeMillis() < unavailableUntil) {
			return null;
		}
		synchronized (this) {
			try {
				var relocated = LocalWorkDirectory.relocate(projectName, projectRoot, FOLDER).toPath();
				if (create) {
					Files.createDirectories(relocated);
				}
				folder = relocated;
				return relocated;
			} catch (Exception e) {
				unavailableUntil = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(1);
				debug("JavaScript bytecode cache: unable to use " + new File(projectRoot, FOLDER) + " (" + e + ")");
				return null;
			}
		}
	}

	private record Entry(byte kind, long compileNanos, Map<String, byte[]> classes, long lastModified, long readNanos, long readCpuNanos) { }

	private static final java.lang.management.ThreadMXBean THREADS = java.lang.management.ManagementFactory.getThreadMXBean();
	private static final boolean THREAD_CPU_TIME = cpuTimeSupported();

	private static boolean cpuTimeSupported() {
		try {
			return THREADS.isCurrentThreadCpuTimeSupported() && THREADS.isThreadCpuTimeEnabled();
		} catch (Exception e) {
			return false;
		}
	}

	/** The processor time of the current thread, the elapsed time where the JVM cannot tell it. */
	private static long cpuTime() {
		return THREAD_CPU_TIME ? THREADS.getCurrentThreadCpuTime() : System.nanoTime();
	}

	/** @return the entry of the key, null when missing; InvalidEntry when it cannot be used */
	private Entry read(Path file, String key, String mainClassName) throws InvalidEntry {
		byte[] data;
		long lastModified;
		var reading = System.nanoTime();
		var readingCpu = cpuTime();
		try {
			var attributes = Files.readAttributes(file, java.nio.file.attribute.BasicFileAttributes.class);
			if (!attributes.isRegularFile() || attributes.size() > MAX_ENTRY_BYTES) {
				throw new InvalidEntry("not an entry");
			}
			lastModified = attributes.lastModifiedTime().toMillis();
			data = Files.readAllBytes(file);
		} catch (NoSuchFileException e) {
			return null;
		} catch (IOException e) {
			throw new InvalidEntry("unreadable: " + e);
		}
		var readNanos = System.nanoTime() - reading;
		var readCpuNanos = cpuTime() - readingCpu;
		var decoded = decode(data, key, mainClassName, macKey);
		return new Entry(decoded.kind, decoded.compileNanos, decoded.classes, lastModified, readNanos, readCpuNanos);
	}

	record Decoded(byte kind, long compileNanos, Map<String, byte[]> classes) { }

	static byte[] encode(String key, String mainClassName, byte kind, long compileNanos, Object[] compiled, byte[] macKey) {
		try {
			var bytes = new ByteArrayOutputStream(compiled == null ? 256 : 64 * 1024);
			var out = new DataOutputStream(bytes);
			out.write(MAGIC);
			out.writeUTF(ENVIRONMENT);
			out.writeUTF(key);
			out.writeUTF(mainClassName);
			out.writeByte(kind);
			out.writeLong(compileNanos);
			int count = compiled == null ? 0 : compiled.length / 2;
			out.writeInt(count);
			for (int i = 0; i < count; i++) {
				var data = (byte[]) compiled[2 * i + 1];
				out.writeUTF((String) compiled[2 * i]);
				out.writeInt(data.length);
				out.write(data);
			}
			out.flush();
			var mac = mac(macKey);
			mac.update(bytes.toByteArray());
			bytes.write(mac.doFinal());
			return bytes.toByteArray();
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	static Decoded decode(byte[] data, String key, String mainClassName, byte[] macKey) throws InvalidEntry {
		if (data.length < MAGIC.length + MAC_LENGTH) {
			throw new InvalidEntry("truncated");
		}
		var mac = mac(macKey);
		mac.update(data, 0, data.length - MAC_LENGTH);
		var expected = mac.doFinal();
		var actual = java.util.Arrays.copyOfRange(data, data.length - MAC_LENGTH, data.length);
		if (!MessageDigest.isEqual(expected, actual)) {
			throw new InvalidEntry("signature mismatch");
		}
		try (var in = new DataInputStream(new java.io.ByteArrayInputStream(data, 0, data.length - MAC_LENGTH))) {
			var magic = in.readNBytes(MAGIC.length);
			if (!java.util.Arrays.equals(magic, MAGIC)) {
				throw new InvalidEntry("other format");
			}
			if (!ENVIRONMENT.equals(in.readUTF())) {
				throw new InvalidEntry("other Rhino or patch");
			}
			if (!key.equals(in.readUTF())) {
				throw new InvalidEntry("other source");
			}
			if (!mainClassName.equals(in.readUTF())) {
				throw new InvalidEntry("other classes");
			}
			var kind = in.readByte();
			var compileNanos = in.readLong();
			int count = in.readInt();
			if (kind != KIND_CLASSES && kind != KIND_NOT_CACHEABLE || count < 0 || count > 1 << 20
					|| kind == KIND_CLASSES && count == 0) {
				throw new InvalidEntry("damaged");
			}
			var classes = new HashMap<String, byte[]>(count * 2);
			for (int i = 0; i < count; i++) {
				var name = in.readUTF();
				int length = in.readInt();
				if (!name.startsWith(mainClassName) || length < 0 || length > in.available()) {
					throw new InvalidEntry("damaged");
				}
				classes.put(name, in.readNBytes(length));
			}
			if (in.available() != 0) {
				throw new InvalidEntry("damaged");
			}
			return new Decoded(kind, compileNanos, classes);
		} catch (IOException e) {
			throw new InvalidEntry("damaged");
		}
	}

	private static Mac mac(byte[] key) {
		try {
			var mac = Mac.getInstance("HmacSHA256");
			mac.init(new SecretKeySpec(key, "HmacSHA256"));
			return mac;
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	/** Defines the classes of a script in their own loader, as Rhino does after generating them in memory. */
	@SuppressWarnings("unchecked")
	static Script instantiate(Context cx, String mainClassName, Map<String, byte[]> classes) throws ReflectiveOperationException {
		var loader = new EntryLoader(mainClassName, classes);
		var scriptClass = loader.loadClass(mainClassName);
		// its static initialization builds the descriptors of the script and its functions into the script class
		Class.forName(mainClassName + "Main", true, loader);
		var descriptors = scriptClass.getField("_descriptors").get(null);
		if (!(descriptors instanceof JSDescriptor<?>[] all) || all.length == 0 || all[0] == null || !all[0].isScript()) {
			throw new IllegalStateException("No descriptor of script in " + mainClassName);
		}
		// as Codegen.defineClass: the regular expressions and template literals of the script
		try {
			var regExpInit = method(scriptClass, "_reInit", Context.class);
			if (regExpInit != null) {
				regExpInit.invoke(null, cx);
			}
			var templateLiteralInit = method(scriptClass, "_qInit");
			if (templateLiteralInit != null) {
				templateLiteralInit.invoke(null);
			}
		} catch (InvocationTargetException e) {
			if (e.getCause() instanceof RuntimeException runtime) {
				throw runtime;
			}
			throw e;
		}
		return new JSScript((JSDescriptor<JSScript>) all[0], null);
	}

	private static Method method(Class<?> type, String name, Class<?>... parameters) {
		try {
			return type.getMethod(name, parameters);
		} catch (NoSuchMethodException e) {
			return null;
		}
	}

	/** The classes of one script, defined here; the others come from the loader of Rhino. */
	private static final class EntryLoader extends ClassLoader {
		private final String prefix;
		private final Map<String, byte[]> classes;

		EntryLoader(String prefix, Map<String, byte[]> classes) {
			super(Context.class.getClassLoader());
			this.prefix = prefix;
			this.classes = classes;
		}

		@Override
		protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
			if (!name.startsWith(prefix)) {
				return super.loadClass(name, resolve);
			}
			synchronized (getClassLoadingLock(name)) {
				var loaded = findLoadedClass(name);
				if (loaded == null) {
					loaded = findClass(name);
				}
				if (resolve) {
					resolveClass(loaded);
				}
				return loaded;
			}
		}

		/** Their bytes go once defined: all of them once the script is built. */
		@Override
		protected synchronized Class<?> findClass(String name) throws ClassNotFoundException {
			var bytes = classes.remove(name);
			if (bytes == null) {
				throw new ClassNotFoundException(name);
			}
			return defineClass(name, bytes, 0, bytes.length);
		}
	}

	/**
	 * Generates (for KIND_CLASSES) and writes an entry in the background. The classes are generated again by the
	 * ClassCompiler, out of the critical path: the script in use is the one of Context.compileString, and the time kept in
	 * the entry is its compilation time, compared to the loading time by auto.
	 */
	private void schedule(String key, String mainClassName, byte kind, long compileNanos, CompilerEnvirons env, String source,
			String sourceName, boolean enhancedJavaAccess, Measure measure) {
		if (!flushOnExit && !inRunningEngine()) {
			// outside an engine (tests, tools), the JVM may end right after: the pending entries are written first
			synchronized (keyLock) {
				if (!flushOnExit) {
					flushOnExit = true;
					Runtime.getRuntime().addShutdownHook(new Thread(RhinoBytecodeCache::flush, "JavaScript bytecode cache flush"));
				}
			}
		}
		var time = new AtomicLong(compileNanos);
		var previous = pending.putIfAbsent(key, time);
		if (previous != null) {
			// compiled again before written: one entry, with the longest time (the first, colder, compilation)
			previous.accumulateAndGet(compileNanos, Math::max);
			return;
		}
		var queued = System.nanoTime();
		writer.execute(() -> {
			try {
				if (measure != null && measure.isOff()) {
					return;
				}
				var written = kind;
				Object[] compiled = null;
				if (kind == KIND_CLASSES) {
					awaitQuiet(queued);
					if (measure != null && measure.isOff()) {
						return;
					}
					compiled = generate(env, source, sourceName, mainClassName, enhancedJavaAccess);
					if (compiled == null) {
						written = KIND_NOT_CACHEABLE;
					}
				}
				write(key, encode(key, mainClassName, written, time.get(), compiled, macKey));
			} finally {
				pending.remove(key, time);
			}
		});
	}

	/** Code generation reads one feature from the current context: the background one has the same. */
	private static final ContextFactory[] GENERATION_FACTORIES = { generationFactory(false), generationFactory(true) };

	private static ContextFactory generationFactory(boolean enhancedJavaAccess) {
		return new ContextFactory() {
			@Override
			protected boolean hasFeature(Context cx, int featureIndex) {
				return featureIndex == Context.FEATURE_ENHANCED_JAVA_ACCESS ? enhancedJavaAccess : super.hasFeature(cx, featureIndex);
			}
		};
	}

	/** @return the classes the patched ClassCompiler generates for the source, null when it cannot (class file limits) */
	static Object[] generate(CompilerEnvirons env, String source, String sourceName, String mainClassName, boolean enhancedJavaAccess) {
		// the source compiled already: no message to report again
		env.setErrorReporter(SilentReporter.INSTANCE);
		GENERATION_FACTORIES[enhancedJavaAccess ? 1 : 0].enterContext();
		try {
			return new ClassCompiler(env).compileToClassFiles(source, sourceName, 1, mainClassName);
		} catch (RuntimeException | LinkageError e) {
			debug("JavaScript bytecode cache: " + sourceName + " is compiled at each start (" + e + ")");
			return null;
		} finally {
			Context.exit();
		}
	}

	private static final class SilentReporter implements ErrorReporter {
		static final SilentReporter INSTANCE = new SilentReporter();

		@Override
		public void warning(String message, String sourceName, int line, String lineSource, int lineOffset) {
		}

		@Override
		public void error(String message, String sourceName, int line, String lineSource, int lineOffset) {
			throw runtimeError(message, sourceName, line, lineSource, lineOffset);
		}

		@Override
		public EvaluatorException runtimeError(String message, String sourceName, int line, String lineSource, int lineOffset) {
			return new EvaluatorException(message, sourceName, line, lineSource, lineOffset);
		}
	}

	/** Writes an entry: a temporary file renamed over the previous one, readers never see a partial entry. */
	void write(String key, byte[] data) {
		var directory = folder(true);
		if (directory == null) {
			return;
		}
		var target = directory.resolve(key + SUFFIX);
		var temporary = directory.resolve("." + key + "." + ProcessHandle.current().pid() + "-" + temporaryCounter.incrementAndGet() + TEMPORARY);
		try {
			try {
				Files.createFile(temporary, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
			} catch (UnsupportedOperationException notPosix) {
				Files.createFile(temporary);
			}
			Files.write(temporary, data, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
			try {
				Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			} catch (AtomicMoveNotSupportedException e) {
				return;
			}
			statistics.writes.incrementAndGet();
		} catch (FileAlreadyExistsException e) {
			return;
		} catch (IOException e) {
			debug("JavaScript bytecode cache: unable to write " + target + " (" + e + ")");
			if (projectRoot != null) {
				folder = null;
			}
			return;
		} finally {
			try {
				Files.deleteIfExists(temporary);
			} catch (IOException e) {
				// removed by a later scan
			}
		}
		synchronized (scanLock) {
			if (writesSinceScan >= 0 && ++writesSinceScan < SCAN_EVERY_WRITES) {
				return;
			}
			writesSinceScan = 0;
		}
		evict(directory);
	}

	/** Keeps the folder below its size: the entries read least recently go, and the temporary files left by a crash. */
	void evict(Path directory) {
		var entries = new ArrayList<Object[]>();
		long total = 0;
		var now = System.currentTimeMillis();
		try (var files = Files.newDirectoryStream(directory)) {
			for (var file : files) {
				var name = file.getFileName().toString();
				try {
					var attributes = Files.readAttributes(file, java.nio.file.attribute.BasicFileAttributes.class,
							java.nio.file.LinkOption.NOFOLLOW_LINKS);
					var modified = attributes.lastModifiedTime().toMillis();
					if (name.endsWith(TEMPORARY)) {
						if (now - modified > STALE_TEMPORARY_MILLIS) {
							Files.deleteIfExists(file);
						}
					} else if (name.endsWith(SUFFIX) && attributes.isRegularFile()) {
						entries.add(new Object[] { file, attributes.size(), modified });
						total += attributes.size();
					}
				} catch (IOException e) {
					// removed meanwhile
				}
			}
		} catch (IOException e) {
			return;
		}
		if (total <= maxBytes) {
			return;
		}
		entries.sort((a, b) -> Long.compare((Long) a[2], (Long) b[2]));
		var goal = maxBytes / 4 * 3;
		for (var entry : entries) {
			if (total <= goal) {
				break;
			}
			try {
				Files.deleteIfExists((Path) entry[0]);
				total -= (Long) entry[1];
				statistics.evictions.incrementAndGet();
			} catch (IOException e) {
				// next one
			}
		}
	}

	/** Marks an entry as read, once a day at most: eviction removes the entries read least recently. */
	private static void touch(Path file, long lastModified) {
		var now = System.currentTimeMillis();
		if (now - lastModified > TOUCH_AFTER_MILLIS) {
			writer.execute(() -> {
				try {
					Files.setLastModifiedTime(file, FileTime.fromMillis(now));
				} catch (IOException e) {
					// evicted meanwhile
				}
			});
		}
	}

	/** Waits for the pending entries to be written. */
	static void flush() {
		var done = new java.util.concurrent.CountDownLatch(1);
		flushing.incrementAndGet();
		try {
			writer.execute(done::countDown);
			done.await(10, TimeUnit.SECONDS);
		} catch (Exception e) {
			// best effort
		} finally {
			flushing.decrementAndGet();
		}
	}

	/**
	 * Waits, in the writer, for the end of the compilations of a startup: generating the classes again is a second
	 * compilation, which would compete with the startup for the processors.
	 */
	private static void awaitQuiet(long queued) {
		while (flushing.get() == 0) {
			var now = System.nanoTime();
			var idle = now - lastCompiled;
			if (idle >= QUIET_NANOS || now - queued >= MAX_DEFER_NANOS) {
				return;
			}
			try {
				Thread.sleep(Math.max(1, Math.min(200, TimeUnit.NANOSECONDS.toMillis(QUIET_NANOS - idle))));
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			}
		}
	}

	/** For the tests: the verdict of auto and the counters start again, the caches of projects are forgotten. */
	static void reset() {
		flush();
		caches.clear();
		synchronized (measure) {
			measure.samples = 0;
			measure.loadNanos = 0;
			measure.compileNanos = 0;
			measure.reported = false;
			measure.off = false;
		}
		statistics.hits.set(0);
		statistics.misses.set(0);
		statistics.invalid.set(0);
		statistics.writes.set(0);
		statistics.evictions.set(0);
		statistics.bypasses.set(0);
		statistics.loadNanos.set(0);
		statistics.loadCostNanos.set(0);
		statistics.loadedCompileNanos.set(0);
		statistics.compileNanos.set(0);
		statistics.keyNanos.set(0);
		statistics.readNanos.set(0);
		statistics.checkNanos.set(0);
		statistics.defineNanos.set(0);
		pending.clear();
		runningEngineForTests = false;
	}

	static Measure measure() {
		return measure;
	}

	static String environmentForTests() {
		return ENVIRONMENT;
	}

	/**
	 * The version of Rhino and of its patch, and the format of this cache: an entry of another one is compiled again.
	 * Null without the patched ClassCompiler: no disk cache.
	 */
	private static String environment() {
		try {
			var patch = (String) ClassCompiler.class.getField("C8O_PATCH").get(null);
			var md = MessageDigest.getInstance("SHA-256");
			var loader = Context.class.getClassLoader();
			for (var name : new String[] { "Context", "ScriptRuntime", "JSDescriptor", "JSScript", "JSFunction", "IRFactory", "Parser",
					"optimizer/ClassCompiler", "optimizer/Codegen", "optimizer/BodyCodegen", "optimizer/OptJSCode",
					"optimizer/OptJSFunctionCode", "optimizer/OptJSScriptCode", "optimizer/OptRuntime", "optimizer/Optimizer" }) {
				try (InputStream in = loader.getResourceAsStream("org/mozilla/javascript/" + name + ".class")) {
					if (in == null) {
						return null;
					}
					md.update(in.readAllBytes());
				}
			}
			try (InputStream in = loader.getResourceAsStream("org/mozilla/classfile/ClassFileWriter.class")) {
				if (in != null) {
					md.update(in.readAllBytes());
				}
			}
			var version = Context.class.getPackage() == null ? null : Context.class.getPackage().getImplementationVersion();
			return "rhino=" + (version == null ? "?" : version) + ";classes=" + HexFormat.of().formatHex(md.digest()).substring(0, 16)
					+ ";patch=" + patch + ";format=" + FORMAT;
		} catch (NoSuchFieldException e) {
			return null;
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * The secret signing the entries of this installation, kept in the configuration folder of the engine, outside the
	 * projects and shared by the instances of the workspace. Outside an engine (tests, tools), a constant: the entries are
	 * only checked against damage, and never accepted by an engine. Null when the secret cannot be kept: no disk cache.
	 */
	static byte[] installationKey() {
		var key = installationKey;
		if (key != null) {
			return key;
		}
		synchronized (keyLock) {
			if (installationKey != null) {
				return installationKey;
			}
			var configuration = Engine.CONFIGURATION_PATH;
			if (!inRunningEngine() || configuration == null || configuration.isEmpty() || !new File(configuration).isDirectory()) {
				installationKey = "convertigo-rhino-bytecode-outside-engine".getBytes(StandardCharsets.UTF_8);
				return installationKey;
			}
			try {
				installationKey = readOrCreateKey(Path.of(configuration, KEY_FILE));
			} catch (Exception e) {
				warn("JavaScript bytecode cache: off, unable to keep its key in " + configuration + " (" + e + ")");
			}
			return installationKey;
		}
	}

	static byte[] readOrCreateKey(Path file) throws IOException {
		for (int attempt = 0; attempt < 5; attempt++) {
			try {
				var key = Files.readAllBytes(file);
				if (key.length == MAC_LENGTH) {
					return key;
				}
				// damaged: replaced, the entries signed with it are compiled again
				Files.deleteIfExists(file);
			} catch (NoSuchFileException e) {
				// created below
			}
			var key = new byte[MAC_LENGTH];
			new SecureRandom().nextBytes(key);
			var temporary = file.resolveSibling(file.getFileName() + "." + ProcessHandle.current().pid() + TEMPORARY);
			try {
				try {
					Files.createFile(temporary, PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
				} catch (UnsupportedOperationException notPosix) {
					Files.deleteIfExists(temporary);
					Files.createFile(temporary);
				}
				Files.write(temporary, key, StandardOpenOption.WRITE);
				try {
					// another instance creating it at the same time: the first one wins, both read it
					Files.createLink(file, temporary);
				} catch (FileAlreadyExistsException e) {
					continue;
				} catch (UnsupportedOperationException | IOException noHardLink) {
					try {
						Files.move(temporary, file);
					} catch (FileAlreadyExistsException e) {
						continue;
					}
				}
			} finally {
				Files.deleteIfExists(temporary);
			}
		}
		throw new IOException("unable to read or create " + file);
	}

	private static void info(String message) {
		if (Engine.logEngine != null) {
			Engine.logEngine.info(message);
		}
	}

	private static void warn(String message) {
		if (Engine.logEngine != null) {
			Engine.logEngine.warn(message);
		}
	}

	private static void debug(String message) {
		if (Engine.logEngine != null && Engine.logEngine.isDebugEnabled()) {
			Engine.logEngine.debug(message);
		}
	}
}
