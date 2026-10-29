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

package com.twinsoft.convertigo.engine.flow;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.LocalWorkDirectory;
import com.twinsoft.convertigo.engine.util.RhinoBytecodeCache;

/**
 * Disk cache of the authoring responses of the runtime providers that the bridge keeps in memory (the trees of the
 * projects, the palettes, the catalogs: the cacheable methods of {@link FlowEngineBridge}), so that the first calls
 * of the Studio after a restart, or after the deployment of an unrelated project, do not compute them again. A
 * response found here is the text its provider answered, for the same request and the same sources.
 * <p>
 * <b>Key</b>: given by the bridge, which derives it from everything the response depends on: the format of this
 * cache, the version and the code of the engine, the settings the providers read, the provider, the method and its
 * request, and the state of the files of the provider, of the project and of the projects it uses. Any change is
 * another key: an entry is never updated, the one of a previous state is evicted in time.
 * <p>
 * <b>Where</b>: the folder {@value #FOLDER} of the local working directory (engine property local_work.directory),
 * else of the workspace. One file per entry, written to a temporary file then renamed: threads and JVMs sharing the
 * folder never read a partial entry. Each entry is compressed and signed with an HMAC-SHA256 keyed by a secret of the
 * installation, kept in the configuration folder: an entry damaged, truncated, of another format or written by another
 * installation fails the check and is a miss, removed. The folder is bounded: the entries read least recently go
 * first.
 * <p>
 * <b>What is never written</b>: a response computed faster than reading it back would be, and a response containing
 * the value of a global symbol holding a secret (named *.secret, or naming a password, a token, a key or a credential).
 * The authoring responses show the symbols by reference, never by value: such a response is refused rather than kept.
 * The bridge never asks for a request carrying working copies (drafts), nor for a response its provider marks
 * "cacheable": false.
 * <p>
 * Engine property javascript.authoring_cache: auto (default) in a running engine, on: also outside one (tests,
 * measures), off: never.
 */
final class FlowAuthoringDiskCache {
	/** The folder of the cache, in the local working directory or the workspace. */
	static final String FOLDER = "authoring-cache";
	/** 1: named texts (the response, what it references), deflated */
	static final int FORMAT = 1;
	private static final byte[] MAGIC = { 'C', '8', 'O', 'A', 'R', 'C', '\n', FORMAT };
	private static final String SUFFIX = ".arc";
	private static final String TEMPORARY = ".tmp";
	private static final String KEY_FILE = "authoring-cache.key";
	private static final int MAC_LENGTH = 32;
	/** The size of the folder above which the entries read least recently are removed. */
	static final long DEFAULT_MAX_BYTES = 256L << 20;
	/** The largest response kept, as UTF-8. */
	static final int MAX_TEXT_BYTES = 64 << 20;
	/** A response computed faster is not written: reading it back would not save much. */
	static final long MIN_COMPUTE_MILLIS = 40;
	private static final long TOUCH_AFTER_MILLIS = TimeUnit.HOURS.toMillis(1);
	private static final long STALE_TEMPORARY_MILLIS = TimeUnit.HOURS.toMillis(1);
	private static final int SCAN_EVERY_WRITES = 16;
	private static final Pattern KEY_PATTERN = Pattern.compile("[0-9a-f]{64}");
	/** The names of the global symbols whose values are credentials, besides the *.secret ones. */
	private static final Pattern SECRET_NAME = Pattern.compile("(?i).*(pass(word|wd)?|secret|token|api[._-]?key|private[._-]?key|credential).*");
	/** Shorter values are too common to tell a secret in a response. */
	private static final int SECRET_MIN_LENGTH = 6;

	enum Mode {
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

	/** Counters of this JVM, for diagnostics and tests. */
	static final class Statistics {
		final AtomicLong hits = new AtomicLong(), misses = new AtomicLong(), invalid = new AtomicLong(), writes = new AtomicLong(),
				evictions = new AtomicLong(), refused = new AtomicLong(), dropped = new AtomicLong(), readNanos = new AtomicLong();

		@Override
		public String toString() {
			return "hits=" + hits + ", misses=" + misses + ", invalid=" + invalid + ", writes=" + writes + ", evictions=" + evictions
					+ ", refused=" + refused + ", dropped=" + dropped + ", readMs=" + readNanos.get() / 1_000_000;
		}
	}

	static final Statistics statistics = new Statistics();
	private static final Object lock = new Object();
	/** The cache of the engine, resolved once per start; null until then. */
	private static volatile FlowAuthoringDiskCache current;
	/** For the tests: the cache used whatever the engine, null for none. */
	private static volatile FlowAuthoringDiskCache forced;
	private static final AtomicInteger temporaryCounter = new AtomicInteger();
	/** The keys waiting for their write: written once. */
	private static final Set<String> pending = ConcurrentHashMap.newKeySet();
	private static final ThreadPoolExecutor writer = new ThreadPoolExecutor(0, 1, 5, TimeUnit.SECONDS,
			new LinkedBlockingQueue<>(64), runnable -> {
				var thread = new Thread(runnable, "Authoring cache writer");
				thread.setDaemon(true);
				thread.setPriority(Thread.MIN_PRIORITY);
				return thread;
			}, new ThreadPoolExecutor.AbortPolicy());
	/** The values of the secrets of the global symbols, for the symbols generation they were read at. */
	private static volatile SecretValues secretValues;
	/** The values no entry may show, replaced by the tests. */
	static volatile java.util.function.Supplier<List<String>> secrets = FlowAuthoringDiskCache::secretSymbolValues;

	private record SecretValues(long generation, List<String> values) { }

	private final Path folder;
	private final long maxBytes;
	private final byte[] macKey;
	private final Object scanLock = new Object();
	private int writesSinceScan = -1;

	FlowAuthoringDiskCache(Path folder, long maxBytes, byte[] macKey) {
		this.folder = folder;
		this.maxBytes = maxBytes;
		this.macKey = macKey;
	}

	/** @return the mode of the engine property, auto when it cannot be read */
	static Mode mode() {
		try {
			return Mode.parse(EnginePropertiesManager.getProperty(PropertyName.JAVASCRIPT_AUTHORING_CACHE));
		} catch (Exception notInitialized) {
			return Mode.parse(System.getProperty("convertigo.engine." + PropertyName.JAVASCRIPT_AUTHORING_CACHE.getKey()));
		}
	}

	/** @return the cache of the engine, null when it is off or cannot be used */
	static FlowAuthoringDiskCache get() {
		var test = forced;
		if (test != null) {
			return test;
		}
		var mode = mode();
		if (mode == Mode.off || mode == Mode.auto && (Engine.theApp == null || Engine.isCliMode())) {
			return null;
		}
		var cache = current;
		if (cache != null) {
			return cache;
		}
		synchronized (lock) {
			if (current == null) {
				current = open();
			}
			return current;
		}
	}

	/** For the tests: this cache whatever the engine and its property, null to go back to the engine one. */
	static void use(FlowAuthoringDiskCache cache) {
		flush();
		forced = cache;
	}

	/** The engine stops: the next start resolves its folder again (another local working directory). */
	static void stop() {
		synchronized (lock) {
			current = null;
		}
	}

	private static FlowAuthoringDiskCache open() {
		var key = installationKey();
		if (key == null) {
			return null;
		}
		var directory = LocalWorkDirectory.getDirectory(FOLDER);
		if (directory == null) {
			if (Engine.USER_WORKSPACE_PATH == null || Engine.USER_WORKSPACE_PATH.isBlank()) {
				return null;
			}
			directory = new File(Engine.USER_WORKSPACE_PATH, FOLDER);
		}
		try {
			Files.createDirectories(directory.toPath());
		} catch (IOException e) {
			warn("Authoring cache: unable to use " + directory + ", the responses are computed at each start (" + e + ")");
			return null;
		}
		info("Authoring cache: " + directory);
		return new FlowAuthoringDiskCache(directory.toPath(), DEFAULT_MAX_BYTES, key);
	}

	Path folder() {
		return folder;
	}

	/**
	 * @return the texts of the entry of a key by their names (the response, what it references), null when missing or
	 *         unusable (then removed)
	 */
	Map<String, String> read(String key) {
		if (!KEY_PATTERN.matcher(key).matches()) {
			return null;
		}
		var started = System.nanoTime();
		var file = folder.resolve(key + SUFFIX);
		try {
			byte[] data;
			long lastModified;
			try {
				var attributes = Files.readAttributes(file, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
				if (!attributes.isRegularFile() || attributes.size() > MAX_TEXT_BYTES + 4096L) {
					throw new InvalidEntry("not an entry");
				}
				lastModified = attributes.lastModifiedTime().toMillis();
				data = Files.readAllBytes(file);
			} catch (NoSuchFileException e) {
				statistics.misses.incrementAndGet();
				return null;
			} catch (IOException e) {
				throw new InvalidEntry("unreadable: " + e);
			}
			var parts = decode(data, key, macKey);
			statistics.hits.incrementAndGet();
			touch(file, lastModified);
			return parts;
		} catch (InvalidEntry e) {
			statistics.invalid.incrementAndGet();
			debug("Authoring cache: entry " + key + " removed (" + e.getMessage() + ")");
			try {
				Files.deleteIfExists(file);
			} catch (IOException ignored) {
				// replaced by its next write
			}
			return null;
		} finally {
			statistics.readNanos.addAndGet(System.nanoTime() - started);
		}
	}

	/** @return whether an entry exists for a key, without reading it */
	boolean contains(String key) {
		return KEY_PATTERN.matcher(key).matches() && Files.isRegularFile(folder.resolve(key + SUFFIX), LinkOption.NOFOLLOW_LINKS);
	}

	/**
	 * Writes the entry of a key in the background, once.
	 *
	 * @param parts its texts by their names, given by a supplier called in the background (the response, what it
	 *            references), null not to write it
	 * @param computeNanos the time its provider took to compute it: a response computed faster than
	 *            {@value #MIN_COMPUTE_MILLIS} ms is not written
	 */
	void write(String key, java.util.function.Supplier<Map<String, String>> parts, long computeNanos) {
		if (computeNanos < TimeUnit.MILLISECONDS.toNanos(MIN_COMPUTE_MILLIS) || parts == null || !KEY_PATTERN.matcher(key).matches()
				|| !pending.add(key)) {
			return;
		}
		try {
			writer.execute(() -> {
				try {
					var texts = parts.get();
					if (texts != null) {
						writeNow(key, texts);
					}
				} catch (Throwable t) {
					debug("Authoring cache: unable to write " + key + " (" + t + ")");
				} finally {
					pending.remove(key);
				}
			});
		} catch (RejectedExecutionException e) {
			// too many writes waiting: computed again at the next start
			pending.remove(key);
			statistics.dropped.incrementAndGet();
		}
	}

	/** Writes an entry now: a temporary file renamed over the previous one, readers never see a partial entry. */
	void writeNow(String key, Map<String, String> parts) {
		for (var text : parts.values()) {
			if (containsSecret(text)) {
				statistics.refused.incrementAndGet();
				debug("Authoring cache: a response showing the value of a secret is not written (" + key + ")");
				return;
			}
		}
		var raw = payload(parts);
		if (raw == null) {
			return;
		}
		var data = encode(key, raw, macKey);
		var target = folder.resolve(key + SUFFIX);
		var temporary = folder.resolve("." + key + "." + ProcessHandle.current().pid() + "-" + temporaryCounter.incrementAndGet() + TEMPORARY);
		try {
			Files.createDirectories(folder);
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
		} catch (IOException e) {
			debug("Authoring cache: unable to write " + target + " (" + e + ")");
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
		evict();
	}

	/** Keeps the folder below its size: the entries read least recently go, and the temporary files left by a crash. */
	void evict() {
		var entries = new ArrayList<Object[]>();
		long total = 0;
		var now = System.currentTimeMillis();
		try (var files = Files.newDirectoryStream(folder)) {
			for (var file : files) {
				var name = file.getFileName().toString();
				try {
					var attributes = Files.readAttributes(file, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
					var modified = attributes.lastModifiedTime().toMillis();
					if (name.endsWith(TEMPORARY)) {
						if (now - modified > STALE_TEMPORARY_MILLIS) {
							Files.deleteIfExists(file);
						}
					} else if (name.endsWith(SUFFIX) && attributes.isRegularFile()) {
						if (outdated(file)) {
							// another format: never read again
							Files.deleteIfExists(file);
							statistics.evictions.incrementAndGet();
							continue;
						}
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

	/** Removes every entry: the responses are computed again (the cache clear action of the Studio). */
	void clear() {
		flush();
		try (var files = Files.newDirectoryStream(folder, "*" + SUFFIX)) {
			for (var file : files) {
				try {
					Files.deleteIfExists(file);
					statistics.evictions.incrementAndGet();
				} catch (IOException e) {
					// removed meanwhile
				}
			}
		} catch (IOException e) {
			// no folder yet
		}
	}

	/** @return the size of the entries of the folder, in bytes */
	long size() {
		long total = 0;
		try (var files = Files.newDirectoryStream(folder, "*" + SUFFIX)) {
			for (var file : files) {
				try {
					total += Files.size(file);
				} catch (IOException e) {
					// removed meanwhile
				}
			}
		} catch (IOException e) {
			return 0;
		}
		return total;
	}

	/** Waits for the pending entries to be written. */
	static void flush() {
		var done = new CountDownLatch(1);
		try {
			writer.execute(done::countDown);
			done.await(10, TimeUnit.SECONDS);
		} catch (Exception e) {
			// best effort
		}
	}

	/** Thrown for an entry that cannot be used: a miss, removed. */
	static final class InvalidEntry extends Exception {
		private static final long serialVersionUID = 1L;

		InvalidEntry(String reason) {
			super(reason, null, false, false);
		}
	}

	/** @return the texts of an entry, each by its name and its UTF-8 bytes; null beyond {@value #MAX_TEXT_BYTES} bytes */
	static byte[] payload(Map<String, String> parts) {
		try {
			var bytes = new ByteArrayOutputStream(64 * 1024);
			var out = new DataOutputStream(bytes);
			out.writeInt(parts.size());
			for (var part : new java.util.TreeMap<>(parts).entrySet()) {
				var text = part.getValue().getBytes(StandardCharsets.UTF_8);
				out.writeUTF(part.getKey());
				out.writeInt(text.length);
				out.write(text);
				if (out.size() > MAX_TEXT_BYTES) {
					return null;
				}
			}
			out.flush();
			return bytes.toByteArray();
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	/** @return the texts of a payload by their names */
	static Map<String, String> parts(byte[] raw) throws InvalidEntry {
		try (var in = new DataInputStream(new ByteArrayInputStream(raw))) {
			var count = in.readInt();
			if (count < 0 || count > 64) {
				throw new InvalidEntry("damaged");
			}
			var parts = new java.util.LinkedHashMap<String, String>();
			for (var i = 0; i < count; i++) {
				var name = in.readUTF();
				var length = in.readInt();
				if (length < 0 || length > in.available()) {
					throw new InvalidEntry("damaged");
				}
				parts.put(name, new String(in.readNBytes(length), StandardCharsets.UTF_8));
			}
			if (in.available() != 0) {
				throw new InvalidEntry("damaged");
			}
			return parts;
		} catch (IOException e) {
			throw new InvalidEntry("damaged");
		}
	}

	static byte[] encode(String key, byte[] raw, byte[] macKey) {
		try {
			var deflater = new Deflater(Deflater.BEST_SPEED);
			byte[] zipped;
			try {
				deflater.setInput(raw);
				deflater.finish();
				var out = new ByteArrayOutputStream(Math.max(64, raw.length / 4));
				var buffer = new byte[64 * 1024];
				while (!deflater.finished()) {
					out.write(buffer, 0, deflater.deflate(buffer));
				}
				zipped = out.toByteArray();
			} finally {
				deflater.end();
			}
			var bytes = new ByteArrayOutputStream(zipped.length + 256);
			var out = new DataOutputStream(bytes);
			out.write(MAGIC);
			out.writeUTF(key);
			out.writeInt(raw.length);
			out.writeInt(zipped.length);
			out.write(zipped);
			out.flush();
			var mac = mac(macKey);
			mac.update(bytes.toByteArray());
			bytes.write(mac.doFinal());
			return bytes.toByteArray();
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	static Map<String, String> decode(byte[] data, String key, byte[] macKey) throws InvalidEntry {
		if (data.length < MAGIC.length + MAC_LENGTH) {
			throw new InvalidEntry("truncated");
		}
		var mac = mac(macKey);
		mac.update(data, 0, data.length - MAC_LENGTH);
		if (!MessageDigest.isEqual(mac.doFinal(), Arrays.copyOfRange(data, data.length - MAC_LENGTH, data.length))) {
			throw new InvalidEntry("signature mismatch");
		}
		try (var in = new DataInputStream(new ByteArrayInputStream(data, 0, data.length - MAC_LENGTH))) {
			if (!Arrays.equals(in.readNBytes(MAGIC.length), MAGIC)) {
				throw new InvalidEntry("other format");
			}
			if (!key.equals(in.readUTF())) {
				throw new InvalidEntry("other key");
			}
			var rawLength = in.readInt();
			var zippedLength = in.readInt();
			if (rawLength < 0 || rawLength > MAX_TEXT_BYTES || zippedLength < 0 || zippedLength != in.available()) {
				throw new InvalidEntry("damaged");
			}
			var zipped = in.readNBytes(zippedLength);
			var inflater = new Inflater();
			try {
				inflater.setInput(zipped);
				var raw = new byte[rawLength];
				var read = 0;
				while (read < rawLength) {
					var count = inflater.inflate(raw, read, rawLength - read);
					if (count == 0 && (inflater.finished() || inflater.needsInput() || inflater.needsDictionary())) {
						break;
					}
					read += count;
				}
				if (read != rawLength || !inflater.finished()) {
					throw new InvalidEntry("damaged");
				}
				return parts(raw);
			} catch (DataFormatException e) {
				throw new InvalidEntry("damaged");
			} finally {
				inflater.end();
			}
		} catch (IOException e) {
			throw new InvalidEntry("damaged");
		}
	}

	/** @return whether the header of an entry names another format than this one */
	static boolean outdated(Path file) {
		try (var in = Files.newInputStream(file)) {
			var magic = in.readNBytes(MAGIC.length);
			return magic.length == MAGIC.length && Arrays.equals(magic, 0, MAGIC.length - 1, MAGIC, 0, MAGIC.length - 1)
					&& magic[MAGIC.length - 1] != FORMAT;
		} catch (IOException e) {
			return false;
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

	/** Marks an entry as read, once an hour at most: eviction removes the entries read least recently. */
	private static void touch(Path file, long lastModified) {
		var now = System.currentTimeMillis();
		if (now - lastModified > TOUCH_AFTER_MILLIS) {
			try {
				writer.execute(() -> {
					try {
						Files.setLastModifiedTime(file, FileTime.fromMillis(now));
					} catch (IOException e) {
						// evicted meanwhile
					}
				});
			} catch (RejectedExecutionException e) {
				// marked at its next read
			}
		}
	}

	/**
	 * @return whether a text shows the value of a global symbol holding a secret: one named *.secret, or whose name
	 *         tells a password, a token, a key or a credential, of {@value #SECRET_MIN_LENGTH} characters at least
	 */
	static boolean containsSecret(String text) {
		for (var value : secrets.get()) {
			if (text.contains(value)) {
				return true;
			}
		}
		return false;
	}

	private static List<String> secretSymbolValues() {
		var app = Engine.theApp;
		var manager = app == null ? null : app.databaseObjectsManager;
		if (manager == null) {
			return List.of();
		}
		var generation = manager.getSymbolsGeneration();
		var known = secretValues;
		if (known != null && known.generation() == generation) {
			return known.values();
		}
		var values = new ArrayList<String>();
		try {
			for (var name : manager.symbolsGetNames()) {
				if (name.endsWith(".secret") || SECRET_NAME.matcher(name).matches()) {
					var value = manager.symbolsGetValue(name);
					if (value != null && value.length() >= SECRET_MIN_LENGTH) {
						values.add(value);
						// as a JSON string shows it
						var escaped = org.codehaus.jettison.json.JSONObject.quote(value);
						escaped = escaped.substring(1, escaped.length() - 1);
						if (!escaped.equals(value)) {
							values.add(escaped);
						}
					}
				}
			}
		} catch (RuntimeException e) {
			// symbols being reloaded: read again at the next write
			return values;
		}
		secretValues = new SecretValues(generation, List.copyOf(values));
		return values;
	}

	/**
	 * The secret signing the entries of this installation, kept in the configuration folder of the engine, outside the
	 * projects and shared by the instances of the workspace. Outside an engine, a constant: the entries are only checked
	 * against damage. Null when the secret cannot be kept: no disk cache.
	 */
	private static byte[] installationKey() {
		var configuration = Engine.CONFIGURATION_PATH;
		if (Engine.theApp == null || configuration == null || configuration.isEmpty() || !new File(configuration).isDirectory()) {
			return "convertigo-authoring-cache-outside-engine".getBytes(StandardCharsets.UTF_8);
		}
		try {
			return RhinoBytecodeCache.readOrCreateKey(Path.of(configuration, KEY_FILE));
		} catch (Exception e) {
			warn("Authoring cache: off, unable to keep its key in " + configuration + " (" + e + ")");
			return null;
		}
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
