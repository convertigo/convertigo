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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import org.apache.log4j.Logger;

/**
 * The processes the engine launches (builds, development servers, the PouchDB server of FullSync, the
 * Process execute steps): one service for the engine, the Studio and the providers, so that none of them
 * outlives the engine, and none is ever stopped on a name, a pattern or a PID alone.
 * <ul>
 * <li>{@link #start(ManagedProcessSpec)} launches a process, keeps it referenced, records it in the
 * registry of its workspace (&lt;workspace&gt;/processes) with its PID, its start instant and the
 * fingerprint of its command, and, for Node.js, adds a preload that stops it when the engine is gone;</li>
 * <li>{@link ManagedProcess#stop()} stops its whole tree, gracefully then forcibly, with the ProcessHandle
 * API only;</li>
 * <li>{@link #stopAll()}, called first by Engine.stop and by a single shutdown hook, stops all of them;</li>
 * <li>{@link #open(File)}, called by Engine.start, stops the processes a previous session left running, only
 * when their PID, start instant and command match the registry;</li>
 * <li>a process reaper, a small JVM started with the first recorded process, stops the recorded processes
 * when the engine dies abruptly (see {@link ProcessReaper}); it is useless, and off by default (auto), in
 * a container whose main process is the engine: the container stops with it;</li>
 * <li>on Windows, the processes also join a job object that the system closes, killing them, when the JVM
 * exits.</li>
 * </ul>
 * The service is the same for the whole JVM: {@link #get()} works before the engine starts and after it
 * stops (CLI, shutdown hook).
 */
public final class ManagedProcesses {
	/** the folder of the registry, in the workspace */
	public static final String REGISTRY_DIR = "processes";

	private static final ManagedProcesses INSTANCE = new ManagedProcesses();

	public static ManagedProcesses get() {
		return INSTANCE;
	}

	private final Logger logger = Logger.getLogger("cems.ManagedProcesses");
	private final Object lock = new Object();
	private final Map<Long, ManagedProcess> running = new LinkedHashMap<>();
	private boolean accepting = true;
	private boolean shutdown = false;
	private File workspace = null;
	private Thread hook = null;
	private ScheduledExecutorService scheduler = null;

	private final Object reaperLock = new Object();
	private String reaperMode = "auto";
	private Process reaper = null;
	private OutputStream reaperInput = null;
	private final Set<Path> reaperRegistries = new LinkedHashSet<>();

	private final ProcessRegistry.Log registryLog = new ProcessRegistry.Log() {

		@Override
		public void info(String message) {
			logger.info(message);
		}

		@Override
		public void warn(String message) {
			logger.warn(message);
		}
	};

	ManagedProcesses() {
	}

	Logger log() {
		return logger;
	}

	/**
	 * Accepts new processes again, whose default registry is the one of workspace, and stops the processes
	 * a previous session left running there, only when their PID, start instant and command match the
	 * registry. Called by Engine.start.
	 *
	 * @return what the cleanup did
	 */
	public List<ProcessRegistry.Cleanup> open(File workspace) {
		synchronized (lock) {
			this.workspace = workspace;
			if (!shutdown) {
				accepting = true;
			}
		}
		return workspace == null ? List.of() : cleanup(workspace, null);
	}

	/**
	 * @return the workspace whose registry records the processes by default, null when none
	 */
	public File getWorkspace() {
		synchronized (lock) {
			return workspace;
		}
	}

	/**
	 * @return whether a process can be started: false while the engine stops, or after it stopped
	 */
	public boolean isAccepting() {
		synchronized (lock) {
			return accepting;
		}
	}

	/**
	 * Stops the processes of function a previous session left running in the registry of owner, only when
	 * their PID, start instant and command match; never the processes this engine runs, nor the ones of
	 * another engine still running.
	 *
	 * @param function null for all the functions
	 */
	public List<ProcessRegistry.Cleanup> cleanup(File owner, String function) {
		ProcessRegistry registry = registry(owner);
		long self = ProcessHandle.current().pid();
		return registry.cleanup(function, Duration.ofSeconds(5), registryLog, entry -> {
			if (entry.ownerPid != self) {
				return false;
			}
			synchronized (lock) {
				return running.containsKey(entry.pid);
			}
		});
	}

	public static ProcessRegistry registry(File owner) {
		return new ProcessRegistry(new File(owner, REGISTRY_DIR).toPath());
	}

	/**
	 * Starts a process, see {@link ManagedProcessSpec}.
	 *
	 * @throws IOException when the process cannot start, or the engine stops
	 */
	public ManagedProcess start(ManagedProcessSpec spec) throws IOException {
		ProcessBuilder builder = spec.builder();
		List<String> command = builder.command();
		if (command.isEmpty()) {
			throw new IOException("No command for the process " + spec.function);
		}
		File owner = spec.record ? (spec.ownerSet ? spec.owner : getWorkspace()) : null;
		ProcessRegistry registry = owner == null ? null : registry(owner);
		if (spec.nodePreload) {
			File preload = NodePreload.install(preloadDir(registry));
			String path = preload.getAbsolutePath();
			if (NodePreload.isNode(command.get(0))) {
				if (!command.contains(path)) {
					command.add(1, "--require");
					command.add(2, path);
				}
			} else {
				NodePreload.addToNodeOptions(builder.environment(), preload);
			}
			builder.environment().put(NodePreload.ENV_PARENT_PID, Long.toString(ProcessHandle.current().pid()));
			// its end tells the preload that the engine is gone
			builder.redirectInput(ProcessBuilder.Redirect.PIPE);
		}
		installHook();
		if (isAccepting()) {
			beforeStart(registry);
		}
		ManagedProcess managed;
		synchronized (lock) {
			if (!accepting) {
				throw new IOException("The engine is stopping: the process " + spec.function + " is not started");
			}
			Process process = builder.start();
			managed = new ManagedProcess(this, spec, process);
			running.put(process.pid(), managed);
		}
		afterStart(managed);
		if (logger.isDebugEnabled()) {
			logger.debug("Started " + managed.describe() + ": " + command.get(0));
		}
		if (registry != null) {
			Map<String, String> properties = new LinkedHashMap<>(spec.properties);
			if (builder.directory() != null) {
				properties.putIfAbsent("dir", builder.directory().getAbsolutePath());
			}
			try {
				managed.registry = registry;
				managed.entry = registry.record(spec.function, managed.toHandle(), properties);
				scheduleRefresh(managed);
			} catch (IOException e) {
				logger.warn("Cannot record the process " + managed.describe() + " in " + registry.getDir() + ": " + e);
			}
		}
		if (spec.output != null) {
			managed.readOutput(spec.charset == null ? StandardCharsets.UTF_8 : spec.charset);
		}
		managed.getProcess().onExit().thenRun(managed::exited);
		return managed;
	}

	/**
	 * @param mode on, off or auto (on, except in a container whose main process is the engine, which stops
	 *             with it)
	 */
	public void setReaperMode(String mode) {
		synchronized (reaperLock) {
			reaperMode = mode == null ? "auto" : mode.trim().toLowerCase(Locale.ROOT);
		}
	}

	/**
	 * @return the PID of the process reaper, -1 when none runs
	 */
	public long getReaperPid() {
		synchronized (reaperLock) {
			return reaper != null && reaper.isAlive() ? reaper.pid() : -1;
		}
	}

	private boolean isReaperEnabled() {
		return switch (reaperMode) {
		case "on", "true" -> true;
		case "off", "false" -> false;
		default -> !isContainerMainProcess();
		};
	}

	/**
	 * @return whether the engine is the main process of a container, directly or under a minimal init (tini):
	 *         its death stops the container, with all the processes it launched
	 */
	static boolean isContainerMainProcess() {
		ProcessHandle current = ProcessHandle.current();
		if (current.pid() == 1) {
			return true;
		}
		return current.parent().filter(parent -> parent.pid() == 1).map(parent -> {
			String name = new File(parent.info().command().orElse("")).getName();
			return Set.of("tini", "tini-static", "docker-init", "dumb-init", "catatonit").contains(name);
		}).orElse(false);
	}

	/**
	 * Called before a process with a registry starts: the reaper watches its registry.
	 */
	private void beforeStart(ProcessRegistry registry) {
		if (registry == null) {
			return;
		}
		synchronized (reaperLock) {
			if (!isReaperEnabled()) {
				return;
			}
			Path dir = registry.getDir().toAbsolutePath();
			try {
				if (reaper != null && reaper.isAlive() && reaperInput != null) {
					if (reaperRegistries.add(dir)) {
						reaperInput.write(("registry " + dir + "\n").getBytes(StandardCharsets.UTF_8));
						reaperInput.flush();
					}
					return;
				}
				if (reaper != null) {
					// never two reapers: the previous one leaves
					reaper.waitFor(2, TimeUnit.SECONDS);
				}
				reaperRegistries.clear();
				reaperRegistries.add(dir);
				reaper = startReaper(dir);
				reaperInput = reaper.getOutputStream();
				logger.debug("Started the process reaper (PID " + reaper.pid() + ") for " + dir);
			} catch (Exception e) {
				reaper = null;
				reaperInput = null;
				logger.warn("Cannot start the process reaper: the processes of the engine will not be stopped if it dies abruptly", e);
			}
		}
	}

	private Process startReaper(Path dir) throws Exception {
		Files.createDirectories(dir);
		Path classes = copyReaperClasses(dir);
		ProcessHandle current = ProcessHandle.current();
		String java = new File(System.getProperty("java.home"), "bin/" + (File.separatorChar == '\\' ? "java.exe" : "java")).getAbsolutePath();
		List<String> command = List.of(java, "-Xmx16m", "-Xss256k", "-XX:TieredStopAtLevel=1", "-XX:CICompilerCount=1",
				"-XX:+UseSerialGC", "-XX:-UsePerfData", "-XX:MaxMetaspaceSize=32m", "-Xshare:auto",
				"-cp", classes.toString(), ProcessReaper.class.getName(),
				Long.toString(current.pid()), Long.toString(ProcessRegistry.startOf(current.info())), dir.toString());
		ProcessBuilder builder = new ProcessBuilder(command);
		builder.redirectInput(ProcessBuilder.Redirect.PIPE);
		builder.redirectOutput(ProcessBuilder.Redirect.DISCARD);
		builder.redirectError(ProcessBuilder.Redirect.DISCARD);
		builder.environment().remove("JAVA_TOOL_OPTIONS");
		builder.environment().remove("JDK_JAVA_OPTIONS");
		return builder.start();
	}

	/**
	 * Copies the classes of the reaper in &lt;dir&gt;/.reaper/&lt;their fingerprint&gt;: the class loader of
	 * the engine (Tomcat, OSGi) does not give a class path a new JVM can use.
	 */
	static Path copyReaperClasses(Path dir) throws Exception {
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		Map<String, byte[]> files = new LinkedHashMap<>();
		for (Class<?> type : ProcessReaper.CLASSES) {
			String name = type.getName();
			String simple = name.substring(name.lastIndexOf('.') + 1);
			try (InputStream in = ProcessReaper.class.getResourceAsStream(simple + ".class")) {
				if (in == null) {
					throw new IOException("Missing class " + name);
				}
				byte[] bytes = in.readAllBytes();
				files.put(name.replace('.', '/') + ".class", bytes);
				digest.update(bytes);
			}
		}
		Path root = dir.resolve(".reaper").resolve(HexFormat.of().formatHex(digest.digest()).substring(0, 16));
		for (Map.Entry<String, byte[]> file : files.entrySet()) {
			Path path = root.resolve(file.getKey());
			if (!Files.isRegularFile(path) || Files.size(path) != file.getValue().length) {
				Files.createDirectories(path.getParent());
				Path tmp = Files.createTempFile(path.getParent(), ".class-", ".tmp");
				try {
					Files.write(tmp, file.getValue());
					Files.move(tmp, path, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
				} finally {
					Files.deleteIfExists(tmp);
				}
			}
		}
		return root;
	}

	/**
	 * Tells the reaper to leave, once no recorded process runs: after a normal stop.
	 */
	private void stopReaper() {
		synchronized (lock) {
			if (!running.isEmpty()) {
				return;
			}
		}
		synchronized (reaperLock) {
			if (reaper == null) {
				return;
			}
			try {
				reaperInput.write("exit\n".getBytes(StandardCharsets.UTF_8));
				reaperInput.flush();
				reaperInput.close();
			} catch (IOException e) {
				// already gone
			}
			try {
				reaper.waitFor(2, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			reaperInput = null;
			if (!reaper.isAlive()) {
				reaper = null;
			}
		}
	}

	/**
	 * Called once a process started, before it is recorded: on Windows, it joins the job that the system
	 * closes, killing it, when the JVM exits.
	 */
	private void afterStart(ManagedProcess managed) {
		if (WindowsJob.WINDOWS) {
			WindowsJob.assign(managed.pid(), logger);
		}
	}

	private File preloadDir(ProcessRegistry registry) {
		if (registry != null) {
			return registry.getDir().toFile();
		}
		File owner = getWorkspace();
		if (owner != null) {
			return new File(owner, REGISTRY_DIR);
		}
		return new File(System.getProperty("java.io.tmpdir"), "convertigo-" + REGISTRY_DIR);
	}

	/**
	 * A launcher script (#!/usr/bin/env node) executes the real program, and a program can change its title,
	 * shortly after the start: the registry keeps the identity the system then gives.
	 */
	private void scheduleRefresh(ManagedProcess managed) {
		ScheduledExecutorService executor;
		synchronized (lock) {
			if (scheduler == null) {
				ScheduledThreadPoolExecutor created = new ScheduledThreadPoolExecutor(1, runnable -> {
					Thread thread = new Thread(runnable, "Managed processes registry");
					thread.setDaemon(true);
					return thread;
				});
				created.setRemoveOnCancelPolicy(true);
				created.setExecuteExistingDelayedTasksAfterShutdownPolicy(false);
				scheduler = created;
			}
			executor = scheduler;
		}
		Runnable refresh = () -> {
			if (managed.isAlive() && !managed.isStopRequested() && managed.registry != null) {
				managed.entry = managed.registry.refresh(managed.entry);
			}
		};
		executor.schedule(refresh, 500, TimeUnit.MILLISECONDS);
		executor.schedule(refresh, 3, TimeUnit.SECONDS);
	}

	/**
	 * The process is stopped or exited: it is no longer referenced, its entry is removed from the registry.
	 */
	void forget(ManagedProcess managed) {
		synchronized (lock) {
			running.remove(managed.pid(), managed);
		}
		if (managed.registry != null) {
			managed.registry.remove(managed.entry);
		}
	}

	/**
	 * @return the processes running, in the order they started
	 */
	public List<ManagedProcess> list() {
		synchronized (lock) {
			return new ArrayList<>(running.values());
		}
	}

	/**
	 * @return the processes running whose function is function
	 */
	public List<ManagedProcess> list(String function) {
		return list(managed -> managed.getFunction().equals(function));
	}

	public List<ManagedProcess> list(Predicate<ManagedProcess> filter) {
		List<ManagedProcess> list = list();
		list.removeIf(filter.negate());
		return list;
	}

	/**
	 * Stops the processes of function, with their trees, at the same time.
	 *
	 * @return the processes still alive, normally none
	 */
	public List<ProcessHandle> stop(String function) {
		return stop(list(function));
	}

	/**
	 * Stops the processes filter accepts, with their trees, at the same time.
	 *
	 * @return the processes still alive, normally none
	 */
	public List<ProcessHandle> stop(Predicate<ManagedProcess> filter) {
		return stop(list(filter));
	}

	private List<ProcessHandle> stop(List<ManagedProcess> processes) {
		if (processes.isEmpty()) {
			return List.of();
		}
		if (processes.size() == 1) {
			return processes.get(0).stop();
		}
		List<ProcessHandle> survivors = java.util.Collections.synchronizedList(new ArrayList<>());
		List<Thread> threads = new ArrayList<>();
		for (ManagedProcess managed : processes) {
			threads.add(Thread.ofVirtual().name("Stop " + managed.getFunction()).start(() -> survivors.addAll(managed.stop())));
		}
		for (Thread thread : threads) {
			try {
				thread.join();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		}
		return new ArrayList<>(survivors);
	}

	/**
	 * Stops all the processes, with their trees, at the same time; no process starts until {@link #open(File)}.
	 * Called first by Engine.stop.
	 *
	 * @return the processes still alive, normally none
	 */
	public List<ProcessHandle> stopAll() {
		List<ManagedProcess> all;
		synchronized (lock) {
			accepting = false;
			all = new ArrayList<>(running.values());
		}
		if (!all.isEmpty()) {
			logger.info("Stopping " + all.size() + " process(es) launched by the engine");
		}
		List<ProcessHandle> survivors = stop(all);
		afterStopAll();
		return survivors;
	}

	private void afterStopAll() {
		stopReaper();
	}

	/**
	 * The JVM exits: all the processes stop, and none starts anymore.
	 */
	void shutdown() {
		synchronized (lock) {
			shutdown = true;
		}
		stopAll();
	}

	private void installHook() {
		synchronized (lock) {
			if (hook != null || shutdown) {
				return;
			}
			hook = new Thread(this::shutdown, "Managed processes shutdown");
			try {
				Runtime.getRuntime().addShutdownHook(hook);
			} catch (IllegalStateException e) {
				// the JVM already exits
				shutdown = true;
				accepting = false;
			}
		}
	}
}
