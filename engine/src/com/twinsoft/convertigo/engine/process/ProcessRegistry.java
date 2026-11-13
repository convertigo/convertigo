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

package com.twinsoft.convertigo.engine.process;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Predicate;

/**
 * Records the processes launched for an owner (a workspace) in a directory, one properties file per
 * process, so that a later session, or the process reaper when the engine died, can find the processes
 * left by an engine that did not stop normally.
 * <p>
 * A recorded process is only considered the same process when its PID, its start instant and its command
 * all match: a PID alone can be reused by the system. The arguments are recorded as a fingerprint, never
 * in clear: a command line can carry a secret.
 * <p>
 * The format is the one of the PouchDB server of 8.4.8 (function, pid, start, command, commandLine,
 * ownerPid, ownerStart), whose entries are still understood. This class only uses the JDK: the process
 * reaper runs it in its own small JVM.
 */
public final class ProcessRegistry {
	static final String SUFFIX = ".properties";

	public interface Log {
		void info(String message);

		void warn(String message);
	}

	public static final class Entry {
		final Path file;
		final String function;
		final long pid;
		final long start;
		final String command;
		/** the arguments of an entry written by 8.4.8, in clear */
		final String commandLine;
		/** the fingerprint of the command line, empty when the system does not give the arguments (Windows) */
		final String commandLineHash;
		final long ownerPid;
		final long ownerStart;
		final Properties properties;

		private Entry(Path file, Properties properties) {
			this.file = file;
			this.properties = properties;
			function = properties.getProperty("function", "");
			pid = parseLong(properties.getProperty("pid"));
			start = parseLong(properties.getProperty("start"));
			command = properties.getProperty("command", "");
			commandLine = properties.getProperty("commandLine", "");
			commandLineHash = properties.getProperty("commandLineHash", "");
			ownerPid = parseLong(properties.getProperty("ownerPid"));
			ownerStart = parseLong(properties.getProperty("ownerStart"));
		}

		public String getFunction() {
			return function;
		}

		public long getPid() {
			return pid;
		}

		public long getStart() {
			return start;
		}

		public String getCommand() {
			return command;
		}

		public long getOwnerPid() {
			return ownerPid;
		}

		public long getOwnerStart() {
			return ownerStart;
		}

		public Path getFile() {
			return file;
		}

		/** @return a recorded free field, such as port or dir */
		public String getProperty(String name) {
			return properties.getProperty(name);
		}

		public boolean isOwnedBy(long pid, long start) {
			return ownerPid == pid && ownerStart == start;
		}

		public String describe() {
			return "PID " + pid + " started at " + (start > 0 ? Instant.ofEpochMilli(start) : "unknown")
					+ " (" + command + ")";
		}

		@Override
		public String toString() {
			return function + " " + describe();
		}
	}

	public enum Outcome {
		/** The recorded process was still running and its tree has been stopped. */
		STOPPED,
		/** The recorded process was running but could not be stopped. */
		STILL_ALIVE,
		/** The recorded process belongs to an engine that is still running: left untouched. */
		OWNER_ALIVE,
		/** The recorded process is gone, or its PID now belongs to another process: left untouched. */
		GONE
	}

	public static final class Cleanup {
		final Entry entry;
		final Outcome outcome;

		Cleanup(Entry entry, Outcome outcome) {
			this.entry = entry;
			this.outcome = outcome;
		}

		public Entry getEntry() {
			return entry;
		}

		public Outcome getOutcome() {
			return outcome;
		}

		@Override
		public String toString() {
			return outcome + " " + entry;
		}
	}

	private final Path dir;

	public ProcessRegistry(Path dir) {
		this.dir = dir;
	}

	public Path getDir() {
		return dir;
	}

	/**
	 * @return the name of the file of a function, valid on every system
	 */
	static String fileName(String function) {
		return function.replaceAll("[^A-Za-z0-9._-]", "_");
	}

	/**
	 * Records a process that has just been started, with the identity reported by the system.
	 */
	public Entry record(String function, ProcessHandle handle, Map<String, String> extra) throws IOException {
		Properties properties = new Properties();
		if (extra != null) {
			extra.forEach((key, value) -> {
				if (key != null && value != null) {
					properties.setProperty(key, value);
				}
			});
		}
		properties.setProperty("function", function);
		identify(properties, handle);
		ProcessHandle owner = ProcessHandle.current();
		properties.setProperty("ownerPid", Long.toString(owner.pid()));
		properties.setProperty("ownerStart", Long.toString(startOf(owner.info())));
		Path file = dir.resolve(fileName(function) + "-" + handle.pid() + SUFFIX);
		write(file, properties);
		return new Entry(file, properties);
	}

	/**
	 * Records the identity the system now gives to the process of entry, when it changed since it was
	 * recorded and the process is the same (same PID and start instant): a launcher script that executes
	 * another program (#!/usr/bin/env node), a process that changes its title.
	 *
	 * @return the entry, updated or not
	 */
	public Entry refresh(Entry entry) {
		if (entry == null) {
			return null;
		}
		Optional<ProcessHandle> handle = ProcessHandle.of(entry.pid);
		if (handle.isEmpty() || !handle.get().isAlive()) {
			return entry;
		}
		ProcessHandle.Info info = handle.get().info();
		if (startOf(info) != entry.start) {
			return entry;
		}
		String command = info.command().orElse("");
		String hash = hash(info.commandLine().orElse(""));
		if (command.equals(entry.command) && hash.equals(entry.commandLineHash) && entry.commandLine.isEmpty()) {
			return entry;
		}
		if (!Files.exists(entry.file)) {
			// removed: the process stopped meanwhile
			return entry;
		}
		Properties properties = new Properties();
		properties.putAll(entry.properties);
		properties.remove("commandLine");
		identify(properties, handle.get());
		try {
			write(entry.file, properties);
			return new Entry(entry.file, properties);
		} catch (IOException e) {
			return entry;
		}
	}

	private static void identify(Properties properties, ProcessHandle handle) {
		ProcessHandle.Info info = handle.info();
		properties.setProperty("pid", Long.toString(handle.pid()));
		properties.setProperty("start", Long.toString(startOf(info)));
		properties.setProperty("command", info.command().orElse(""));
		properties.setProperty("commandLineHash", hash(info.commandLine().orElse("")));
	}

	private void write(Path file, Properties properties) throws IOException {
		Files.createDirectories(dir);
		Path tmp = Files.createTempFile(dir, ".entry-", ".tmp");
		try {
			try (OutputStream out = Files.newOutputStream(tmp)) {
				properties.store(out, "Process launched by Convertigo, used to stop it if it outlives its engine");
			}
			try {
				Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			} catch (AtomicMoveNotSupportedException e) {
				Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
			}
		} finally {
			Files.deleteIfExists(tmp);
		}
	}

	public void remove(Entry entry) {
		if (entry != null) {
			try {
				Files.deleteIfExists(entry.file);
			} catch (IOException e) {
				// next cleanup will retry
			}
		}
	}

	/**
	 * @return the entries of all the functions
	 */
	public List<Entry> entries() {
		return entries(null);
	}

	/**
	 * @param function null for all the functions
	 * @return the recorded entries of function
	 */
	public List<Entry> entries(String function) {
		List<Entry> entries = new ArrayList<>();
		if (!Files.isDirectory(dir)) {
			return entries;
		}
		String glob = (function == null ? "" : fileName(function) + "-") + "*" + SUFFIX;
		try (DirectoryStream<Path> files = Files.newDirectoryStream(dir, glob)) {
			for (Path file : files) {
				Properties properties = new Properties();
				try (InputStream in = Files.newInputStream(file)) {
					properties.load(in);
				} catch (IOException e) {
					continue;
				}
				Entry entry = new Entry(file, properties);
				if (entry.pid > 0 && (function == null || function.equals(entry.function))) {
					entries.add(entry);
				}
			}
		} catch (IOException e) {
			// nothing recorded
		}
		return entries;
	}

	/**
	 * @return the running process described by entry, only if its PID, start instant and command all match
	 *         the recorded ones, and the arguments too when the system gives them
	 */
	public static Optional<ProcessHandle> match(Entry entry) {
		if (entry.pid <= 0 || entry.start <= 0 || entry.command.isEmpty()) {
			return Optional.empty();
		}
		return ProcessHandle.of(entry.pid).filter(handle -> {
			if (!handle.isAlive() || handle.pid() == ProcessHandle.current().pid()) {
				return false;
			}
			ProcessHandle.Info info = handle.info();
			if (startOf(info) != entry.start) {
				return false;
			}
			if (!info.command().map(entry.command::equals).orElse(false)) {
				return false;
			}
			String commandLine = info.commandLine().orElse("");
			if (commandLine.isEmpty()) {
				return true;
			}
			if (!entry.commandLine.isEmpty()) {
				return entry.commandLine.equals(commandLine);
			}
			return entry.commandLineHash.isEmpty() || entry.commandLineHash.equals(hash(commandLine));
		});
	}

	/**
	 * @return whether the engine that recorded entry is still running, and is not this one
	 */
	public static boolean otherOwnerAlive(Entry entry) {
		ProcessHandle current = ProcessHandle.current();
		if (entry.ownerPid <= 0 || entry.ownerStart <= 0
				|| (entry.ownerPid == current.pid() && entry.ownerStart == startOf(current.info()))) {
			return false;
		}
		return isAlive(entry.ownerPid, entry.ownerStart);
	}

	/**
	 * @return whether the process pid, started at start, is alive: a PID now used by another process is not
	 */
	public static boolean isAlive(long pid, long start) {
		return ProcessHandle.of(pid)
				.filter(ProcessHandle::isAlive)
				.map(handle -> startOf(handle.info()) == start)
				.orElse(false);
	}

	/**
	 * Stops the recorded processes still running whose engine is gone. A process is stopped only when PID,
	 * start instant and command match; entries of processes that are gone or replaced are removed without
	 * touching anything; the processes of an engine still running are left untouched.
	 *
	 * @param function null for all the functions
	 */
	public List<Cleanup> cleanup(String function, Duration grace, Log log) {
		return cleanup(function, grace, log, entry -> false);
	}

	/**
	 * As {@link #cleanup(String, Duration, Log)}, without touching the entries skip accepts: the processes
	 * this engine runs now.
	 */
	public List<Cleanup> cleanup(String function, Duration grace, Log log, Predicate<Entry> skip) {
		List<Cleanup> result = new ArrayList<>();
		for (Entry entry : entries(function)) {
			if (skip.test(entry)) {
				continue;
			}
			if (otherOwnerAlive(entry)) {
				log.warn("Process " + entry + " belongs to a running engine (PID " + entry.ownerPid + "), left untouched");
				result.add(new Cleanup(entry, Outcome.OWNER_ALIVE));
				continue;
			}
			result.add(stopOrForget(entry, grace, log, "left by a previous session"));
		}
		return result;
	}

	/**
	 * Stops the recorded processes of the engine ownerPid, started at ownerStart, which died: only the ones
	 * whose PID, start instant and command match.
	 */
	public List<Cleanup> reap(long ownerPid, long ownerStart, Duration grace, Log log) {
		List<Cleanup> result = new ArrayList<>();
		for (Entry entry : entries()) {
			if (entry.isOwnedBy(ownerPid, ownerStart)) {
				result.add(stopOrForget(entry, grace, log, "of the engine " + ownerPid + " that died"));
			}
		}
		return result;
	}

	private Cleanup stopOrForget(Entry entry, Duration grace, Log log, String why) {
		Optional<ProcessHandle> handle = match(entry);
		if (handle.isPresent()) {
			log.info("Stopping the process " + why + ": " + entry);
			List<ProcessHandle> alive = ProcessTrees.stop(handle.get(), grace);
			if (alive.isEmpty()) {
				remove(entry);
				return new Cleanup(entry, Outcome.STOPPED);
			}
			log.warn("Cannot stop the process " + why + ": " + entry + ", still alive: " + alive);
			return new Cleanup(entry, Outcome.STILL_ALIVE);
		}
		if (ProcessHandle.of(entry.pid).map(ProcessHandle::isAlive).orElse(false)) {
			log.info("PID " + entry.pid + " is now used by another process, left untouched; forgetting " + entry);
		}
		remove(entry);
		return new Cleanup(entry, Outcome.GONE);
	}

	public static long startOf(ProcessHandle.Info info) {
		return info.startInstant().map(Instant::toEpochMilli).orElse(-1L);
	}

	/**
	 * @return the SHA-256 fingerprint of a command line, empty for an empty one
	 */
	static String hash(String commandLine) {
		if (commandLine == null || commandLine.isEmpty()) {
			return "";
		}
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			return HexFormat.of().formatHex(digest.digest(commandLine.getBytes(StandardCharsets.UTF_8)));
		} catch (NoSuchAlgorithmException e) {
			return "";
		}
	}

	private static long parseLong(String value) {
		try {
			return value == null ? -1 : Long.parseLong(value.trim());
		} catch (NumberFormatException e) {
			return -1;
		}
	}
}
