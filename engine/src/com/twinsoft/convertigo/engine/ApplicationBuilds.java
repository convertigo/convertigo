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
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.util.ArrayDeque;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.admin.services.studio.ngxbuilder.WsBuilder;
import com.twinsoft.convertigo.engine.sessions.RedisInstanceDiscovery;
import com.twinsoft.convertigo.engine.util.FileUtils;
import com.twinsoft.convertigo.engine.util.InstanceIdentity;
import com.twinsoft.convertigo.engine.util.Log4jHelper;

/**
 * The production builds of the applications that a server loads without their build (DisplayObjects/mobile), when it
 * builds (allow_server_build): one at a time, in the background, never in the Studio. An application already built
 * is kept as it is.
 * <p>
 * The instances sharing a workspace build an application once: the one that creates the lock file of the project
 * builds, and renews it while building; a lock no longer renewed, or held by an instance that is no longer alive
 * (when the instances are known), is taken over. Until the application is built, its index.html is replaced by a page
 * telling the state of the build (see {@link #statusPage}). A failed build is told in the logs, the page delivered with
 * the project is restored, and it is not tried again before the project is loaded again.
 */
public final class ApplicationBuilds {
	/** The state of the build of the application of a project on this instance. */
	public enum State {
		/** waiting for its turn */
		pending,
		/** being built by this instance */
		building,
		/** being built by another instance */
		elsewhere,
		/** built by this instance */
		built,
		/** the build failed, see the logs */
		failed,
		/** not built, as this server does not build */
		notAllowed
	}

	static final String LOCK = ".mobile.server-build.lock";
	/** The output lines told when a build fails. */
	private static final int FAILURE_LINES = 40;

	static long lockRenewal = TimeUnit.MINUTES.toMillis(1);
	static long lockExpiry = TimeUnit.MINUTES.toMillis(5);
	/** The identity of this instance in the lock files (overridden by the tests). */
	static java.util.function.Supplier<String> instanceId = InstanceIdentity::getLocalInstanceId;

	private static final Map<String, State> states = new ConcurrentHashMap<>();
	private static final Set<String> queued = ConcurrentHashMap.newKeySet();
	private static ExecutorService executor;
	private static ScheduledExecutorService renewer;

	private ApplicationBuilds() {
	}

	/** A project is loaded: its application is built if it has none, on a server that builds. */
	public static void projectLoaded(Project project) {
		if (Engine.isStudioMode() || Engine.isCliMode() || !mayNeedBuild(project)) {
			return;
		}
		var name = project.getName();
		if (queued.add(name)) {
			states.put(name, State.pending);
			executor().execute(() -> {
				queued.remove(name);
				Log4jHelper.mdcClear();
				try {
					build(name);
				} catch (Throwable t) {
					states.put(name, State.failed);
					warn("Application of the project " + name + ": unable to build it", t);
				}
			});
		}
	}

	/** The builds are now allowed: the applications not built as they were not are built. */
	public static void buildsAllowed() {
		if (!Engine.isServerBuildAllowed()) {
			return;
		}
		for (var entry : states.entrySet()) {
			if (entry.getValue() == State.notAllowed) {
				try {
					var project = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(entry.getKey(), false);
					if (project != null) {
						projectLoaded(project);
					}
				} catch (Exception e) {
					warn("Application of the project " + entry.getKey() + ": unable to build it", e);
				}
			}
		}
	}

	/** @return the state of the build of the application of a project on this instance, null if it has none */
	public static State getState(String project) {
		return states.get(project);
	}

	/**
	 * The page served instead of DisplayObjects/mobile/index.html while the application of a project has no build on
	 * this instance: waiting, being built (here or by another instance, reloading until it is ready), failed or not
	 * built as this server does not build.
	 *
	 * @return the page, or null when the application is served as it is
	 */
	public static String statusPage(String project, File projectDir) {
		var state = states.get(project);
		if (state == null || state == State.built) {
			return null;
		}
		if (state == State.elsewhere && !new File(projectDir, "DisplayObjects/" + LOCK).exists()) {
			// the other instance is done
			states.remove(project, state);
			return null;
		}
		String title;
		String message;
		var progress = state == State.pending || state == State.building || state == State.elsewhere;
		switch (state) {
		case pending -> {
			title = "Waiting to be built";
			message = "This server builds the application shortly. This page reloads when it is ready.";
		}
		case building -> {
			title = "Being built";
			message = "This server is building the application. This page reloads when it is ready.";
		}
		case elsewhere -> {
			title = "Being built";
			message = "Another instance of this server is building the application. This page reloads when it is ready.";
		}
		case failed -> {
			title = "Build failed";
			message = "This server could not build the application. Its administrator finds the cause in the logs of"
					+ " the engine. The build is tried again when the project is deployed again.";
		}
		default -> {
			title = "Not built";
			message = "The application was deployed without its build, and this server does not build applications."
					+ " Deploy an archive that includes the build, or allow the builds in the configuration of the server"
					+ " (allow_server_build).";
		}
		}
		return page().replace("%REFRESH%", progress ? "<meta http-equiv=\"refresh\" content=\"15\">\n" : "")
				.replace("%STATE%", state.name()).replace("%TITLE%", title).replace("%MESSAGE%", message)
				.replace("%PROJECT%", escape(project));
	}

	/** @return whether the page tells a build going on, which a client can wait for */
	public static boolean isInProgress(String project) {
		var state = states.get(project);
		return state == State.pending || state == State.building || state == State.elsewhere;
	}

	private static String page;

	private static synchronized String page() {
		if (page == null) {
			try (var input = ApplicationBuilds.class.getResourceAsStream("ApplicationBuilds-page.html")) {
				page = input == null ? "<!DOCTYPE html><title>%TITLE%</title><main class=\"%STATE%\"><h1>%TITLE%</h1><p>%MESSAGE%</p></main>"
						: new String(input.readAllBytes(), StandardCharsets.UTF_8);
			} catch (IOException e) {
				page = "<!DOCTYPE html><title>%TITLE%</title><h1>%TITLE%</h1><p>%MESSAGE%</p>";
			}
		}
		return page;
	}

	private static String escape(String text) {
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}

	/** At engine stop: the running build is stopped. */
	public static synchronized void stop() {
		if (executor != null) {
			executor.shutdownNow();
			executor = null;
		}
		if (renewer != null) {
			renewer.shutdownNow();
			renewer = null;
		}
		for (var name : states.keySet()) {
			if (states.get(name) == State.building) {
				WsBuilder.stopBuildForServer(name);
			}
		}
		queued.clear();
	}

	/** An application to build in DisplayObjects/mobile: it has a builder and no build. */
	private interface Application {
		/** @return whether it is built */
		boolean build(java.util.function.Consumer<String> listener) throws Exception;
	}

	/** @return whether the project may have an application to build, cheaply, while it is loaded */
	private static boolean mayNeedBuild(Project project) {
		try {
			if (project.getFlowEngine() != null) {
				// its builders tell it, in the background
				return true;
			}
		} catch (Exception e) {
			// no Flow
		}
		return application(project) != null;
	}

	/** @return the application of the project when it has no build, else null */
	private static Application application(Project project) {
		var flow = flowApplication(project);
		if (flow != null) {
			return flow;
		}
		try {
			var mobileApplication = project.getMobileApplication();
			if (mobileApplication != null && mobileApplication
					.getApplicationComponent() instanceof com.twinsoft.convertigo.beans.ngx.components.ApplicationComponent app) {
				// a production build writes env.json, which the page of an unbuilt application has not
				if (app.getBuiltGenerationTime() != -1) {
					return null;
				}
				var projectName = project.getName();
				return listener -> WsBuilder.buildForServer(projectName,
						EnginePropertiesManager.getProperty(PropertyName.APPLICATION_SERVER_CONVERTIGO_URL), listener);
			}
		} catch (Exception e) {
			// no application
		}
		return null;
	}

	/**
	 * @return the application of a Flow builder of the project that tells it has no build (state.built), with a build
	 *         command, else null
	 */
	private static Application flowApplication(Project project) {
		try {
			var engine = project.getFlowEngine();
			if (engine == null) {
				return null;
			}
			var builders = com.twinsoft.convertigo.engine.flow.FlowStudioSupport.contextMenu(engine).optJSONArray("builders");
			for (int i = 0; builders != null && i < builders.length(); i++) {
				var builder = builders.getJSONObject(i);
				var state = builder.optJSONObject("state");
				var commands = builder.optJSONObject("commands");
				var command = commands == null ? null : commands.optJSONObject("build");
				// a builder that does not tell whether it is built is never built by the server
				if (builder.optBoolean("available", false) && state != null && state.has("built")
						&& !state.optBoolean("built", true) && command != null && command.optBoolean("enabled", true)) {
					return new Application() {
						@Override
						public boolean build(java.util.function.Consumer<String> listener) throws Exception {
							var result = com.twinsoft.convertigo.engine.flow.FlowStudioSupport.contextAction(engine, command);
							listener.accept(result.optString("message", ""));
							var error = result.optJSONObject("error");
							if (error != null) {
								listener.accept(error.toString());
							}
							var details = result.optJSONObject("details");
							var steps = details == null ? null : details.optJSONArray("steps");
							for (int j = 0; steps != null && j < steps.length(); j++) {
								listener.accept(steps.get(j).toString());
							}
							return result.optBoolean("ok", false);
						}
					};
				}
			}
		} catch (Exception e) {
			warn("Application of the project " + project.getName() + ": unable to read its Flow builders", e);
		}
		return null;
	}

	private static void build(String name) throws Exception {
		waitForEngine();
		var project = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(name, false);
		var application = project == null ? null : application(project);
		if (application == null) {
			states.remove(name);
			return;
		}
		if (!Engine.isServerBuildAllowed()) {
			states.put(name, State.notAllowed);
			info("Application of the project " + name + ": not built, as this server does not build (allow_server_build)");
			return;
		}
		var displayObjects = new File(project.getDirPath(), "DisplayObjects");
		var lock = new File(displayObjects, LOCK);
		var holder = acquire(lock);
		if (holder != null) {
			states.put(name, State.elsewhere);
			info("Application of the project " + name + ": built by the instance " + holder);
			return;
		}
		states.put(name, State.building);
		var renewal = renewer().scheduleWithFixedDelay(() -> renew(lock), lockRenewal, lockRenewal, TimeUnit.MILLISECONDS);
		var mobile = new File(displayObjects, "mobile");
		var delivered = readIndex(mobile);
		var lines = new ArrayDeque<String>();
		try {
			info("Application of the project " + name + ": production build, as the project has no build");
			var start = System.currentTimeMillis();
			var built = application.build(line -> {
				synchronized (lines) {
					if (lines.size() == FAILURE_LINES) {
						lines.removeFirst();
					}
					lines.addLast(line);
				}
			});
			if (built) {
				states.put(name, State.built);
				info("Application of the project " + name + ": built in " + (System.currentTimeMillis() - start) / 1000
						+ " s");
			} else {
				states.put(name, State.failed);
				restoreIndex(mobile, delivered);
				synchronized (lines) {
					error("Application of the project " + name + ": the production build failed, it is not tried again"
							+ " before the project is loaded again; last output:\n" + String.join("\n", lines));
				}
			}
		} finally {
			renewal.cancel(false);
			release(lock);
		}
	}

	/** Builds wait for the engine to be started: projects are loaded during its start. */
	private static void waitForEngine() throws InterruptedException {
		for (int i = 0; i < 1200 && !Engine.isStarted; i++) {
			Thread.sleep(500);
		}
	}

	/**
	 * @return null when this instance holds the lock, else the instance that holds it
	 */
	static String acquire(File lock) throws IOException {
		Files.createDirectories(lock.getParentFile().toPath());
		var me = instanceId.get();
		for (int attempt = 0; attempt < 2; attempt++) {
			try {
				Files.writeString(lock.toPath(), me, StandardCharsets.UTF_8, java.nio.file.StandardOpenOption.CREATE_NEW,
						java.nio.file.StandardOpenOption.WRITE);
				// a shared file system may let two instances create it: the one written last holds it
				var holder = holder(lock);
				return me.equals(holder) ? null : holder;
			} catch (FileAlreadyExistsException e) {
				var holder = holder(lock);
				if (holder == null || !isStale(lock, holder)) {
					return holder == null ? "unknown" : holder;
				}
				info("Application build: the lock " + lock + " of the instance " + holder + " is no longer renewed, it is"
						+ " taken over");
				Files.deleteIfExists(lock.toPath());
			}
		}
		return holder(lock);
	}

	private static String holder(File lock) {
		try {
			return Files.readString(lock.toPath(), StandardCharsets.UTF_8).trim();
		} catch (IOException e) {
			return null;
		}
	}

	/** @return whether the lock is no longer renewed, or held by an instance known to be gone */
	static boolean isStale(File lock, String holder) {
		if (System.currentTimeMillis() - lock.lastModified() > lockExpiry) {
			return true;
		}
		try {
			var instances = RedisInstanceDiscovery.listInstances();
			return !instances.isEmpty() && instances.stream().noneMatch(instance -> holder.equals(instance.instanceId));
		} catch (Throwable t) {
			// the instances are not known
			return false;
		}
	}

	private static void renew(File lock) {
		try {
			Files.setLastModifiedTime(lock.toPath(), FileTime.fromMillis(System.currentTimeMillis()));
		} catch (IOException e) {
			// renewed at the next time
		}
	}

	static void release(File lock) {
		try {
			if (instanceId.get().equals(holder(lock))) {
				Files.deleteIfExists(lock.toPath());
			}
		} catch (IOException e) {
			// expires
		}
	}

	/** @return the page delivered with the project in place of its application, null if none */
	private static byte[] readIndex(File mobile) {
		try {
			return Files.readAllBytes(new File(mobile, "index.html").toPath());
		} catch (IOException e) {
			return null;
		}
	}

	/** After a failed build: the page delivered with the project comes back, what the build wrote goes, but the assets. */
	static void restoreIndex(File mobile, byte[] delivered) {
		var files = mobile.listFiles();
		if (files != null) {
			for (var file : files) {
				if (!file.getName().equals("assets")) {
					FileUtils.deleteQuietly(file);
				}
			}
		}
		if (delivered != null) {
			try {
				Files.createDirectories(mobile.toPath());
				Files.write(new File(mobile, "index.html").toPath(), delivered);
			} catch (IOException e) {
				warn("Application build: unable to restore " + mobile + "/index.html", e);
			}
		}
	}

	private static synchronized ExecutorService executor() {
		if (executor == null) {
			executor = Executors.newSingleThreadExecutor(runnable -> {
				var thread = new Thread(runnable, "ApplicationBuilds");
				thread.setDaemon(true);
				return thread;
			});
		}
		return executor;
	}

	private static synchronized ScheduledExecutorService renewer() {
		if (renewer == null) {
			renewer = Executors.newSingleThreadScheduledExecutor(runnable -> {
				var thread = new Thread(runnable, "ApplicationBuilds lock");
				thread.setDaemon(true);
				return thread;
			});
		}
		return renewer;
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
}
