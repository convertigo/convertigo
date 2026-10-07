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

package com.twinsoft.convertigo.engine.admin.services.studio.ngxbuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ProcessBuilder.Redirect;
import java.net.Inet4Address;
import java.net.NetworkInterface;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import jakarta.websocket.Session;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.LocalWorkDirectory;
import com.twinsoft.convertigo.engine.admin.services.WebSocketService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.enums.MobileBuilderBuildMode;
import com.twinsoft.convertigo.engine.enums.NgxBuilderBuildMode;
import com.twinsoft.convertigo.engine.mobile.MobileBuilder;
import com.twinsoft.convertigo.engine.mobile.MobileEventListener;
import com.twinsoft.convertigo.engine.util.NetworkUtils;
import com.twinsoft.convertigo.engine.util.ProcessUtils;

/**
 * Builds the NGX application of a project, as the application editor of the Eclipse Studio: installs its
 * packages when asked or needed, then serves it in development mode or builds it locally in the
 * DisplayObjects/mobile folder of the project.
 * <ul>
 * <li>client messages: {project, action: attach | build_dev | build_local | kill | auto_build, params: {endpoint,
 * install: update | reinstall, mode: prod | fast | watch, value: the auto build, target: dev | local, what
 * kill stops, both when empty}}</li>
 * <li>server messages: {type: log | output | error | progress | load | network | state | built | compiled,
 * value}; error is a line of the output that tells an error, compiled the end of a compilation of the
 * development server, success or failed, phase what the build does, installing, building or empty, which
 * the preview shows, restart a development server served again with new packages; a state is
 * dev:serving, dev:idle, local:building:&lt;mode&gt;, local:idle or auto:true|false|none (an engine without
 * Studio writes the sources at once, without auto build); network gives the URLs of
 * the development server on the network, as a JSON array</li>
 * </ul>
 * The development server and a local build of a project run side by side, as in the Eclipse Studio.
 */
@ServiceDefinition(name = "WsBuilder", roles = { Role.WEB_ADMIN }, parameters = {}, returnValue = "")
public class WsBuilder extends WebSocketService {
	static class Build implements Runnable {
		private static final Set<Integer> usedPort = new HashSet<>();
		private static final Pattern pRemoveEscape = Pattern.compile("\\x1b\\[[0-9;?]*[A-Za-z]");
		private static final Pattern pServedWebpack = Pattern.compile(".*?open your browser on (http\\S*).*");
		private static final Pattern pServedStandalone = Pattern.compile(".*?Local:\\s+(http\\S*).*");
		private static final Pattern pPercent = Pattern.compile("(\\d+)% (.*)");

		String projectName;
		/** the Convertigo endpoint the application calls */
		String endpoint;
		String projectEndpoint;
		/** dev, or the NgxBuilderBuildMode of a local build */
		String mode;
		/** empty, update or reinstall */
		String install;
		Project project;
		Thread thread;
		int portNode;
		String baseUrl;
		/** the URLs of the development server on the network, for a mobile device */
		String networkUrls;
		/** the result of the last compilation of the development server, success or failed */
		String compiled;
		/** what the build does, installing the packages, building the application, or nothing */
		String phase = "";
		String state = "idle";
		volatile Process process;
		/** whether the build was stopped, which does not fail */
		volatile boolean stopped;

		Build(String projectName, String endpoint, String mode, String install) {
			this.projectName = projectName;
			this.mode = mode;
			this.install = install;
			this.endpoint = endpoint;
			projectEndpoint = endpoint + "/projects/" + projectName + "/";
		}

		public void start() {
			if (thread != null) {
				return;
			}
			thread = new Thread(this);
			thread.setName("Build of " + projectName);
			thread.setDaemon(true);
			thread.start();
		}

		boolean isDev() {
			return "dev".equals(mode);
		}

		/**
		 * @return the projects a project uses, all levels, the deepest first, which have shared components of
		 *         an application
		 */
		private java.util.List<com.twinsoft.convertigo.beans.core.Project> libraries(com.twinsoft.convertigo.beans.core.Project project) {
			var projects = new java.util.ArrayList<com.twinsoft.convertigo.beans.core.Project>();
			projects.add(project);
			for (var i = 0; i < projects.size(); i++) {
				for (var reference : projects.get(i).getReferenceList()) {
					if (reference instanceof com.twinsoft.convertigo.beans.references.ProjectSchemaReference schema) {
						// the name of the project, its reference being "name=url"
						var name = schema.getParser().getProjectName();
						try {
							var used = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(name);
							if (used != null && !projects.contains(used)) {
								projects.add(used);
							}
						} catch (Exception e) {
							Engine.logStudio.debug("(WsBuilder) the project " + name + " used by " + project.getName() + " does not load", e);
						}
					}
				}
			}
			projects.remove(project);
			java.util.Collections.reverse(projects);
			projects.removeIf((used) -> {
				var application = used.getMobileApplication();
				return application == null || !(application.getApplicationComponent() instanceof com.twinsoft.convertigo.beans.ngx.components.ApplicationComponent app)
						|| app.getSharedComponentList().isEmpty();
			});
			return projects;
		}

		@Override
		public void run() {
			var mutex = new Object();
			MobileBuilder mb = null;
			try {
				Engine.checkServerBuildAllowed("The build of " + projectName);
				project = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(projectName);
				mb = project.getMobileBuilder();
				// the libraries it uses generate their shared components first, which the application takes, as
				// the CLI does
				for (var library : libraries(project)) {
					var builder = library.getMobileBuilder();
					if (builder != null && !builder.isInitialized()) {
						appendOutput("Generating the components of " + library.getName());
						MobileBuilder.initBuilder(library, true);
					}
				}
				if (mb != null && !mb.isInitialized()) {
					// an engine without the Eclipse Studio initializes the builder of a project only for the CLI
					MobileBuilder.initBuilder(project, true);
				}
				if (mb == null || !mb.isInitialized()) {
					throw new Exception("The project " + projectName + " has no application to build");
				}
				setState(isDev() ? "serving" : "building:" + mode);

				var ionicDir = new File(project.getDirPath(), "_private/ionic");
				var nodeVersion = ProcessUtils.getNodeVersion(project);
				var nodeDir = ProcessUtils.getDefaultNodeDir();
				try {
					nodeDir = ProcessUtils.getNodeDir(nodeVersion, (r, t, x) -> {
						appendOutput("Downloading nodejs " + nodeVersion + ": " + Math.round((r * 100f) / t) + "%");
					});
				} catch (Exception e) {
					log("Failed to get nodejs " + nodeVersion + ": " + e.getMessage());
				}
				var versions = "Will use nodejs " + ProcessUtils.getNodeVersion(nodeDir) + " and npm "
						+ ProcessUtils.getNpmVersion(nodeDir);
				appendOutput(versions);
				Engine.logStudio.info(versions);
				var path = nodeDir.getAbsolutePath();

				// the development server replaces all the node processes of the project, a local build only a
				// watching one
				clearNode(!isDev());

				var nodeModules = new File(ionicDir, "node_modules");
				if (StringUtils.isNotBlank(install) || !nodeModules.exists() || mb.getNeedPkgUpdate()) {
					installPackages(path, ionicDir, "reinstall".equals(install));
				}
				mb.setNeedPkgUpdate(false);

				if (isDev()) {
					mb.setBuildMutex(mutex);
					serve(path, ionicDir, mb, mutex);
				} else {
					buildLocally(path, ionicDir, mb, NgxBuilderBuildMode.get(mode));
				}
			} catch (Exception e) {
				log("Exception: " + e.getMessage());
				Engine.logStudio.warn("(WsBuilder) build of " + projectName + " failed", e);
			} finally {
				synchronized (mutex) {
					mutex.notify();
				}
				if (mb != null) {
					if (isDev()) {
						mb.setBuildMutex(null);
					}
					mb.buildFinished();
				}
				baseUrl = null;
				networkUrls = null;
				setPhase("");
				setState("idle");
				synchronized (builds) {
					builds.remove(key(projectName, isDev()), this);
				}
			}
		}

		/**
		 * Runs npm install in the ionic folder, after removing its node_modules for a re-install.
		 */
		private void installPackages(String path, File ionicDir, boolean clean) throws Exception {
			var nodeModules = new File(ionicDir, "node_modules");
			if (clean && nodeModules.exists()) {
				appendOutput("Removing existing node_modules... This can take several seconds...");
				com.twinsoft.convertigo.engine.util.FileUtils.deleteQuietly(nodeModules);
			}
			setPhase("installing");
			appendOutput("Installing node_modules... This can take several minutes depending on your network connection speed...");
			if (!nodeModules.exists()) {
				var packageLockTpl = new File(ionicDir, "package-lock-tpl.json");
				if (packageLockTpl.exists()) {
					com.twinsoft.convertigo.engine.util.FileUtils.copyFile(packageLockTpl, new File(ionicDir, "package-lock.json"));
				}
			}
			LocalWorkDirectory.beginWork(nodeModules);
			var pb = ProcessUtils.getNpmProcessBuilder(path + File.pathSeparator + ionicDir, "npm", "install",
					"--legacy-peer-deps", "--loglevel", "info", "--ssl-key=" + marker());
			pb.redirectErrorStream(true);
			pb.directory(ionicDir);
			var p = start(pb);
			try (var br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
				String line;
				while ((line = br.readLine()) != null) {
					line = clean(line);
					if (StringUtils.isNotBlank(line)) {
						Engine.logStudio.info(line);
						appendOutput(line);
					}
				}
			}
			var code = p.waitFor();
			if (code == 0) {
				LocalWorkDirectory.endWork(nodeModules);
			}
			appendOutput(code == 0 ? "Packages installed." : "npm install ended with the code " + code + ".");
		}

		/**
		 * Serves the application in development mode, through the gateway of the engine.
		 */
		private void serve(String path, File ionicDir, MobileBuilder mb, Object mutex) throws Exception {
			mb.startBuild();
			setPhase("building");
			// a component that needs new packages, as the Eclipse Studio does, installs them and serves again
			var restarting = new boolean[] { false };
			MobileEventListener packagesNeeded = () -> {
				synchronized (restarting) {
					if (restarting[0]) {
						return;
					}
					restarting[0] = true;
				}
				log("The application needs new packages: they are installed and the application is served again.");
				// the Studio shows the application served again, as the one it asked
				send("restart", "packages");
				var next = new Build(projectName, endpoint, mode, "update");
				var thread = new Thread(() -> {
					try {
						launch(next);
					} catch (Exception e) {
						Engine.logStudio.warn("(WsBuilder) failed to serve " + projectName + " again", e);
					}
				});
				thread.setDaemon(true);
				thread.start();
			};
			mb.addMobileEventListener(packagesNeeded);
			try {
				servePackaged(path, ionicDir, mb, mutex);
			} finally {
				mb.removeMobileEventListener(packagesNeeded);
			}
		}

		private void servePackaged(String path, File ionicDir, MobileBuilder mb, Object mutex) throws Exception {
			new File(project.getDirPath(), "DisplayObjects/mobile").mkdirs();

			var pb = ProcessUtils.getNpmProcessBuilder(path, "npm", "run", "ionic:serve");
			List<String> cmd = pb.command();
			synchronized (usedPort) {
				int port = (Math.abs(ionicDir.getAbsolutePath().hashCode()) % 10000) + 40000;
				cmd.add("--");
				usedPort.clear();
				portNode = NetworkUtils.nextAvailable(port, usedPort);
				cmd.add("--port=" + portNode);
				cmd.add("--host=0.0.0.0");
				cmd.add("--disable-host-check=true");
			}
			// #183 add useless option to help terminateNode method to find the current path
			cmd.add("--ssl-key=" + marker());

			pb.redirectErrorStream(true);
			pb.directory(ionicDir);
			var angularJson = new File(ionicDir, "angular.json");
			var angular = FileUtils.readFileToString(angularJson, "UTF-8");
			// a path, as the application is reached through the gateway of the Studio or on the network by
			// a mobile device
			var basePath = projectEndpoint.replaceFirst("^https?://[^/]+", "");
			angular = angular.replaceFirst("(\"serve\":\\s*\\{).*",
					"$1 \"baseHref\":\"" + basePath + "DisplayObjects/dev" + portNode + "/\",");
			FileUtils.write(angularJson, angular, "UTF-8");

			var standalone = mb.isStandalone();
			var p = start(pb);
			try (var br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
				String line;
				StringBuilder errors = null;
				while ((line = br.readLine()) != null) {
					line = clean(line);
					if (StringUtils.isBlank(line)) {
						continue;
					}
					Engine.logStudio.info(line);
					var lower = normalize(line).toLowerCase(Locale.ROOT);
					var ended = false;
					if (lower.contains("changes detected") || lower.contains("compiling...")) {
						setPhase("building");
					}
					if (standalone) {
						progress(standaloneProgress(lower));
						if (isError(lower)) {
							appendError(line);
						} else {
							appendOutput(line);
						}
						// the end of a compilation tells how it went, before its errors
						ended = lower.contains("application bundle generation complete")
								|| lower.contains("bundle generation failed");
					} else {
						if (line.startsWith("Error: ")) {
							errors = new StringBuilder();
						}
						if (errors != null) {
							errors.append(line).append('\n');
							if (line.contains("Failed to compile.")) {
								log("error: " + errors);
								errors = null;
							}
						}
						var matcher = pPercent.matcher(line);
						if (matcher.find()) {
							progress(Integer.parseInt(matcher.group(1)));
							appendOutput(matcher.group(2));
						} else {
							appendOutput(line);
						}
						ended = line.matches(".*Compiled .*successfully.*") || line.contains("Failed to compile.");
					}
					if (ended) {
						progress(100);
						// the Studio tells the compilations that fail, as the application editor of the Eclipse Studio
						compiled = lower.contains("bundle generation failed") || line.contains("Failed to compile.")
								? "failed" : "success";
						send("compiled", compiled);
						setPhase("");
						synchronized (mutex) {
							mutex.notify();
						}
						mb.buildFinished();
					}
					var served = (standalone ? pServedStandalone : pServedWebpack).matcher(line);
					if (served.matches() && baseUrl == null) {
						baseUrl = served.group(1).replaceFirst(".*?://.*?/", "/").replaceFirst("([^/])$", "$1/");
						send("load", baseUrl);
						networkUrls = networkUrls(served.group(1));
						send("network", networkUrls);
					}
				}
			}
		}

		/**
		 * Builds the application in the DisplayObjects/mobile folder of the project, as the "Build locally"
		 * menu of the Eclipse Studio.
		 */
		private void buildLocally(String path, File ionicDir, MobileBuilder mb, NgxBuilderBuildMode buildMode) throws Exception {
			mb.setAppBuildMode(buildMode == NgxBuilderBuildMode.prod ? MobileBuilderBuildMode.production : MobileBuilderBuildMode.fast);
			var baseHref = "/convertigo";
			try {
				baseHref = new URL(projectEndpoint).getPath().replaceFirst("/projects/.*", "");
			} catch (Exception e) {
			}
			baseHref += "/projects/" + projectName + "/DisplayObjects/mobile/";

			setPhase("building");
			appendOutput("Building the application in " + buildMode.label() + " mode...");
			var displayObjectsMobile = new File(project.getDirPath(), "DisplayObjects/mobile");
			displayObjectsMobile.mkdirs();
			for (var file : displayObjectsMobile.listFiles()) {
				if (!file.getName().equals("assets")) {
					com.twinsoft.convertigo.engine.util.FileUtils.deleteQuietly(file);
				}
			}

			var standalone = mb.isStandalone();
			var pb = ProcessUtils.getNpmProcessBuilder(path, "npm", "run", buildMode.command());
			List<String> cmd = pb.command();
			cmd.add("--");
			// #183 add useless option to help terminateNode method to find the current path
			cmd.add((standalone ? "--external-dependencies=" : "--output-path=") + displayObjectsMobile.getAbsolutePath());
			// #393 add base href for project's web app
			cmd.add("--base-href=" + baseHref);
			pb.redirectErrorStream(true);
			pb.directory(ionicDir);

			var p = start(pb);
			var failed = false;
			try (var br = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
				String line;
				while ((line = br.readLine()) != null) {
					line = clean(line);
					if (StringUtils.isBlank(line)) {
						continue;
					}
					Engine.logStudio.debug(line);
					var lower = normalize(line).toLowerCase(Locale.ROOT);
					var matcher = pPercent.matcher(line);
					if (!standalone && matcher.find()) {
						progress(Integer.parseInt(matcher.group(1)));
						appendOutput(matcher.group(2));
					} else {
						if (standalone) {
							progress(standaloneProgress(lower));
						}
						if (isError(lower)) {
							appendError(line);
						} else {
							appendOutput(line);
						}
					}
					if (lower.contains("bundle generation failed") || lower.contains("[error]")
							|| line.contains("Failed to compile.")) {
						failed = true;
					}
					if (lower.contains("application bundle generation complete") || line.contains("- Hash:")) {
						progress(100);
						if (buildMode == NgxBuilderBuildMode.watch) {
							send("built", failed ? "failed" : "success");
							failed = false;
							setPhase("");
						}
					}
				}
			}
			var code = p.waitFor();
			if (stopped) {
				send("built", "stopped");
				appendOutput("The build is stopped.");
			} else if (buildMode != NgxBuilderBuildMode.watch || code != 0) {
				progress(100);
				send("built", code == 0 && !failed ? "success" : "failed");
				appendOutput(code == 0 && !failed ? "The application is built in DisplayObjects/mobile."
						: "The build failed.");
			}
		}

		/**
		 * @return the URL of the development server on each network address of the host, as the QR codes of
		 * the Eclipse Studio show it to a mobile device
		 */
		private static String networkUrls(String url) {
			var urls = new JSONArray();
			try {
				var local = new URL(url);
				for (var netint : Collections.list(NetworkInterface.getNetworkInterfaces())) {
					if (netint.isLoopback() || !netint.isUp()) {
						continue;
					}
					for (var address : Collections.list(netint.getInetAddresses())) {
						if (address instanceof Inet4Address) {
							urls.put(new JSONObject().put("name", netint.getDisplayName()).put("url",
									local.getProtocol() + "://" + address.getHostAddress() + ":" + local.getPort()
											+ local.getFile().replaceFirst("([^/])$", "$1/")));
						}
					}
				}
			} catch (Exception e) {
				Engine.logStudio.debug("(WsBuilder) no network address", e);
			}
			return urls.toString();
		}

		/**
		 * @return the progress a line of the standalone (esbuild) builder shows, 0 when it tells nothing
		 */
		private static int standaloneProgress(String lower) {
			if (lower.contains("application bundle generation complete") || lower.contains("bundle generation failed")) {
				return 99;
			} else if (lower.contains("lazy chunk files")) {
				return 80;
			} else if (lower.contains("initial chunk files")) {
				return 60;
			} else if (lower.contains("✔")) {
				return 35;
			} else if (lower.contains("❯") || lower.contains("changes detected. rebuilding")) {
				return 1;
			}
			return 0;
		}

		/**
		 * Stops the processes this build started, with their children.
		 */
		void stopProcess() {
			stopped = true;
			var current = process;
			if (current != null && current.isAlive()) {
				current.toHandle().descendants().forEach(ProcessHandle::destroy);
				current.destroy();
			}
		}

		private Process start(ProcessBuilder pb) throws IOException {
			var p = pb.start();
			process = p;
			return p;
		}

		private static String clean(String line) {
			line = pRemoveEscape.matcher(line).replaceAll("");
			return line.replaceAll("[\\p{Cntrl}&&[^\\t]]", "");
		}

		private static String normalize(String line) {
			return line.replace(' ', ' ').replaceAll("\\s+", " ").trim();
		}

		/**
		 * @return the useless option value that marks the node processes of the project
		 */
		private String marker() {
			return new File(project.getDirFile(), "DisplayObjects/mobile").getAbsolutePath();
		}

		private void progress(int progress) {
			if (progress > 0) {
				send("progress", "" + progress);
			}
		}

		private void appendOutput(String string) {
			send("output", string);
		}

		/** a line of the output that tells an error of the build, which the Studio shows in red */
		private void appendError(String string) {
			send("error", string);
		}

		/**
		 * @param lower a line of the output, normalized in lower case
		 * @return whether it tells an error of the compilation, as esbuild and the Angular compiler write them
		 */
		private static boolean isError(String lower) {
			return lower.startsWith("✘ [error]") || lower.startsWith("[error]") || lower.startsWith("error:")
					|| lower.matches("^error (ts|ng)\\d+.*") || lower.contains("bundle generation failed");
		}

		private void log(String log) {
			send("log", log);
		}

		private void setState(String state) {
			this.state = state;
			send("state", stateMessage());
		}

		String stateMessage() {
			return (isDev() ? "dev:" : "local:") + state;
		}

		private void setPhase(String phase) {
			if (!phase.equals(this.phase)) {
				this.phase = phase;
				send("phase", (isDev() ? "dev:" : "local:") + phase);
			}
		}

		private void send(String type, String value) {
			WsBuilder.broadcast(projectName, type, value);
		}

		/**
		 * Stops the node processes of the project: its development server, its builds and its installs.
		 */
		void terminateNode(boolean prodOnly) {
			stopProcess();
			clearNode(prodOnly);
		}

		/**
		 * Kills the node processes of the project left by previous builds, all of them or only a watching one,
		 * as this build starts: unlike a stop, this build goes on and reports its own result.
		 */
		private void clearNode(boolean prodOnly) {
			killNode(prodOnly ? " && /--watch|:watch/" : "", prodOnly ? " -and $_.CommandLine -like '*--watch*'" : "");
			if (!prodOnly) {
				baseUrl = null;
				// the gateway no longer reaches its port
				portNode = 0;
			}
		}

		/**
		 * Stops the development server alone, a local build of the project going on, as the Stop of the
		 * application editor of the Eclipse Studio stops the one it is asked.
		 */
		void terminateServe() {
			baseUrl = null;
			stopProcess();
			killNode(" && /serve/", " -and $_.CommandLine -like '*serve*'");
			portNode = 0;
		}

		/**
		 * @param filter what the command line of a node process of the project also matches to be killed, for
		 * awk
		 * @param windowsFilter the same, for PowerShell
		 */
		private void killNode(String filter, String windowsFilter) {
			if (project == null) {
				return;
			}
			// #183 the node processes of this project carry "=<project dir>/DisplayObjects/mobile" in their
			// command line: match the full path to spare the same project opened in another workspace
			var marker = "=" + new File(project.getDirFile(), "DisplayObjects").getAbsolutePath() + File.separator;
			int retry = 10;
			try {
				while (retry-- > 0) {
					ProcessBuilder pb;
					if (Engine.isWindows()) {
						pb = new ProcessBuilder("powershell", "-Command",
								"Get-WmiObject Win32_Process | Where-Object { $_.Name -eq 'node.exe' -and $_.CommandLine -and $_.CommandLine.IndexOf($env:C8O_NODE_MARKER, [StringComparison]::OrdinalIgnoreCase) -ge 0" + windowsFilter + " } | ForEach-Object { $_.Terminate() }");
					} else {
						pb = new ProcessBuilder("/bin/bash", "-c",
								"ps -A -ww -o pid= -o args= | awk '{ i = index($0, ENVIRON[\"C8O_NODE_MARKER\"]) } i && substr($0, 1, i) ~ /node|npm|ng/" + filter + " { print $1 }' | xargs kill");
					}
					pb.environment().put("C8O_NODE_MARKER", marker);
					int code = pb.redirectError(Redirect.DISCARD).redirectOutput(Redirect.DISCARD).start().waitFor();
					if (code == 0) {
						retry = 0;
					}
				}
				synchronized (usedPort) {
					usedPort.remove(portNode);
				}
			} catch (Exception e) {
				Engine.logStudio.warn("Failed to terminate the node server", e);
			}
		}
	}

	/** the running builds, by project for the development server and by project + " local" for a local build */
	static Map<String, Build> builds = new HashMap<>();

	/**
	 * Stops the node processes of all the builds as the engine stops: a development server, a build or an
	 * install would outlive it, as when the desktop Studio quits.
	 */
	public static void stopAll() {
		List<Build> running;
		synchronized (builds) {
			running = List.copyOf(builds.values());
			builds.clear();
		}
		for (var build : running) {
			try {
				build.terminateNode(false);
			} catch (Exception e) {
				Engine.logStudio.warn("(WsBuilder) failed to stop a build", e);
			}
		}
	}
	/**
	 * Stops the development server and the local build of a project closed, deleted or renamed, whose node
	 * processes would go on with a folder that leaves the workspace.
	 */
	public static void stop(String projectName) {
		Build dev, local;
		synchronized (builds) {
			dev = builds.remove(key(projectName, true));
			local = builds.remove(key(projectName, false));
		}
		if (dev == null && local == null) {
			return;
		}
		broadcast(projectName, "log", "Stopping the builds of " + projectName);
		for (var build : new Build[] { local, dev }) {
			if (build != null) {
				try {
					build.terminateNode(false);
				} catch (Exception e) {
					Engine.logStudio.warn("(WsBuilder) failed to stop a build of " + projectName, e);
				}
			}
		}
	}

	/** the clients following the builds of each project */
	static Map<String, Set<WsBuilder>> listeners = new HashMap<>();

	/**
	 * @return whether the development server the Studio started for a project listens on a port, the only one
	 *         the gateway of the engine reaches for it
	 */
	public static boolean serves(String projectName, int port) {
		synchronized (builds) {
			var build = builds.get(projectName);
			return port > 0 && build != null && build.portNode == port;
		}
	}

	Session session;
	String project;

	static String key(String project, boolean dev) {
		return dev ? project : project + " local";
	}

	static void broadcast(String project, String type, String value) {
		Set<WsBuilder> clients;
		synchronized (listeners) {
			clients = new HashSet<>(listeners.getOrDefault(project, Set.of()));
		}
		for (var client : clients) {
			try {
				synchronized (client) {
					send(client.session, type, value);
				}
			} catch (Exception e) {
			}
		}
	}

	@Override
	public void onOpen(Session session) {
		this.session = session;
	}

	@Override
	public void onMessage(String message, Session session) {
		try {
			var json = new JSONObject(message);
			var action = json.getString("action");
			var params = json.optJSONObject("params");
			params = params == null ? new JSONObject() : params;
			follow(json.getString("project"));
			switch (action) {
			case "attach" -> onAttach();
			case "build_dev" -> onBuild(params, "dev");
			case "build_local" -> onBuild(params, NgxBuilderBuildMode.get(params.optString("mode", "fast")).name());
			case "kill" -> onKill(params.optString("target", ""));
			case "auto_build" -> onAutoBuild(params.optBoolean("value", true));
			default -> send("log", "Unknown action " + action);
			}
		} catch (Exception e) {
			Engine.logAdmin.error("(WsBuilder) failed to handle the message " + message, e);
		}
	}

	@Override
	public void onClose(Session session) {
		follow(null);
	}

	@Override
	public void onError(Throwable throwable, Session session) {
		Engine.logAdmin.debug("(WsBuilder) socket error", throwable);
		follow(null);
	}

	/**
	 * Follows the builds of a project, and no more the ones of the previous project.
	 */
	private void follow(String next) {
		synchronized (listeners) {
			if (project != null && !project.equals(next)) {
				var clients = listeners.get(project);
				if (clients != null) {
					clients.remove(this);
				}
			}
			project = next;
			if (next != null) {
				listeners.computeIfAbsent(next, k -> new HashSet<>()).add(this);
			}
		}
	}

	void send(String action, String message) throws JSONException, IOException {
		synchronized (this) {
			send(session, action, message);
		}
	}

	private void onAttach() throws Exception {
		send("state", autoState(builder(project)));
		for (var dev : new boolean[] { true, false }) {
			Build current;
			synchronized (builds) {
				current = builds.get(key(project, dev));
			}
			if (current == null) {
				send("state", dev ? "dev:idle" : "local:idle");
			} else {
				send("state", current.stateMessage());
				if (current.baseUrl != null) {
					send("load", current.baseUrl);
				}
				if (current.networkUrls != null) {
					send("network", current.networkUrls);
				}
				if (current.compiled != null) {
					send("compiled", current.compiled);
				}
				if (!current.phase.isEmpty()) {
					send("phase", (dev ? "dev:" : "local:") + current.phase);
				}
			}
		}
	}

	private void onBuild(JSONObject params, String mode) throws Exception {
		launch(new Build(project, params.getString("endpoint"), mode, params.optString("install", "")));
	}

	/**
	 * Starts a build, which replaces the running one of the same kind.
	 */
	static void launch(Build next) throws InterruptedException {
		Build previous;
		synchronized (builds) {
			previous = builds.put(key(next.projectName, next.isDev()), next);
		}
		if (previous != null) {
			// one development server and one local build at a time for a project: the new one replaces the
			// running one
			previous.stopProcess();
			if (previous.thread != null) {
				previous.thread.join(10000);
			}
		}
		next.start();
	}

	/**
	 * Suspends or resumes the writing of the sources of the application while it is edited, as the "Toggle
	 * auto build" of the Eclipse Studio.
	 */
	private void onAutoBuild(boolean value) throws Exception {
		var mb = builder(project);
		if (mb != null && Engine.isStudioMode()) {
			mb.setAutoBuild(value);
		}
		broadcast(project, "state", autoState(mb));
	}

	/**
	 * @return the auto build state, none for an engine without Studio, whose builder writes at once
	 */
	private static String autoState(MobileBuilder mb) {
		return "auto:" + (!Engine.isStudioMode() ? "none" : mb == null || mb.isAutoBuild());
	}

	private static MobileBuilder builder(String projectName) {
		try {
			var project = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(projectName, false);
			return project == null ? null : project.getMobileBuilder();
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * @param target dev to stop the development server, local to stop the local build, empty for both
	 */
	private void onKill(String target) throws Exception {
		Build dev, local;
		synchronized (builds) {
			dev = "local".equals(target) ? null : builds.get(key(project, true));
			local = "dev".equals(target) ? null : builds.get(key(project, false));
		}
		if (dev == null && local == null) {
			send("state", "dev:idle");
			send("state", "local:idle");
			return;
		}
		if (local != null) {
			broadcast(project, "log", "Stopping the local build of " + project);
			local.terminateNode(true);
		}
		if (dev != null) {
			broadcast(project, "log", "Stopping the development server of " + project);
			if (local != null || "dev".equals(target)) {
				dev.terminateServe();
			} else {
				dev.terminateNode(false);
			}
		}
	}

	static void send(Session session, String action, String message) throws JSONException, IOException {
		var json = new JSONObject();
		json.put("type", action);
		json.put("value", message);
		session.getBasicRemote().sendText(json.toString());
	}
}
