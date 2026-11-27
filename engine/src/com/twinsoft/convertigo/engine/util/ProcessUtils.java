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

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ProcessBuilder.Redirect;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.fileupload.ProgressListener;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.log4j.Level;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.process.ManagedProcessSpec;
import com.twinsoft.convertigo.engine.process.ManagedProcesses;
import com.twinsoft.convertigo.engine.process.ProcessTrees;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.ProxyMode;
import com.twinsoft.convertigo.engine.LocalWorkDirectory;
import com.twinsoft.convertigo.engine.proxy.ntlm.NtlmConnectProxyBridge;

public class ProcessUtils {
	private static final String defaultNodeVersion = readDefaultNodeVersion();
	private static final Object nodeInstallLock = new Object();
	private static volatile File defaultNodeDir;

	private static String readDefaultNodeVersion() {
		try (var input = ProcessUtils.class.getResourceAsStream("/com/twinsoft/convertigo/engine/node-runtime.properties")) {
			var properties = new Properties();
			if (input != null) {
				properties.load(input);
				var version = properties.getProperty("version", "");
				if (version.matches("v\\d+\\.\\d+\\.\\d+")) {
					return version;
				}
			}
		} catch (IOException e) {
			throw new IllegalStateException("Cannot read the default Node.js version", e);
		}
		throw new IllegalStateException("Missing Node.js version: run generateNodeRuntimeProperties before building the engine");
	}

	public static String getDefaultNodeVersion() {
		return defaultNodeVersion;
	}

	public static File getDefaultNodeDir() {
		return getLocalNodeDir(defaultNodeVersion);
	}

	public static String getNodeVersion(Project project) {
		File ionicDir = new File(project.getDirPath(), "_private/ionic");
		File versionFile = new File(ionicDir, "version.json");
		try {
			if (versionFile.exists()) {
				String versionContent = FileUtils.readFileToString(versionFile, "utf-8");
				JSONObject json = new JSONObject(versionContent);
				if (json.has("nodeJsVersion")) {
					String version = json.getString("nodeJsVersion");
					if (version.matches("v\\d+\\.\\d+\\.\\d+.*")) {
						return version;
					}
				} else { // for old or custom tpl with no nodeJsVersion
					if (json.has("version")) {
						String tplVersion = json.getString("version");
						if (VersionUtils.compare(tplVersion, "7.8.0.2") < 0) {
							return "v8.9.1";
						}
					}
				}
			}
		} catch (Exception e) {
			Engine.logConvertigo.warn("Failed to retreive the nodeVersion from '" + versionFile + "': [" + e.getClass() + "] " + e.getMessage());
		}
		return defaultNodeVersion;
	}

	public static String getNodeVersion(File nodeDir) {
		File nodeFile = new File(nodeDir, Engine.isWindows() ? "node.exe" : "node");
		String version = defaultNodeVersion; 
		try {
			if (nodeFile.exists()) {
				Process p = new ProcessBuilder(nodeFile.getAbsolutePath(), "--version").start();
				if (p.waitFor(5, TimeUnit.SECONDS)) {
					try (InputStream is = p.getInputStream()) {
						version = IOUtils.toString(is, Charset.defaultCharset()).trim();
					}
				} else {
					ProcessTrees.stop(p.toHandle(), Duration.ofSeconds(2));
				}
			} else {
				Engine.logConvertigo.info("Node doesn't exist here: " + nodeFile.getAbsolutePath());
			}
		} catch (Exception e) {
			Engine.logConvertigo.warn("Failed to retreive the nodeVersion from '" + nodeFile + "': [" + e.getClass() + "] " + e.getMessage());
		}
		return version;
	}
	public static String getNpmVersion(File nodeDir) {
		String version = "n/a";
		File npmFile = new File(nodeDir, Engine.isWindows() ? "npm.cmd" : "npm");
		try {
			if (npmFile.exists()) {
				// npm is a script run by the node of its folder, which is not necessarily in the PATH of the engine
				Process p = getProcessBuilder(nodeDir.getAbsolutePath(), npmFile.getAbsolutePath(), "-version")
						.redirectError(Redirect.DISCARD).start();
				if (p.waitFor(10, TimeUnit.SECONDS)) {
					if (p.exitValue() == 0) {
						try (InputStream is = p.getInputStream()) {
							version = IOUtils.toString(is, Charset.defaultCharset()).trim();
						}
					}
				} else {
					// npm.cmd runs node through cmd.exe on Windows: the whole tree
					ProcessTrees.stop(p.toHandle(), Duration.ofSeconds(2));
				}
			} else {
				Engine.logConvertigo.info("npm doesn't exist here: " + npmFile.getAbsolutePath());
			}
		} catch (Exception e) {
			Engine.logConvertigo.warn("Failed to retreive the npmVersion from '" + npmFile + "': [" + e.getClass() + "] " + e.getMessage());
		}
		return version;
	}

	public static void setDefaultNodeDir(File nodeDir) {
		defaultNodeDir = nodeDir;
	}

	private static String searchFullPath(String paths, String command) throws IOException {
		String shellFullpath = null;
		// Checks if the command is already full path 
		if (!(new File(command).exists())) {
			// Else search where the "exec" is and build the absolute path for this "exec"
			for (String path: paths.split(Pattern.quote(File.pathSeparator))) {
				File candidate = new File(path, command);
				if (candidate.exists()) {
					shellFullpath = candidate.getAbsolutePath();
					break;
				}
			}
		}

		// If the "exec" is not found then it search it elsewhere
		if (shellFullpath == null) {
			shellFullpath = command;
		}
		return shellFullpath;
	}

	private static String getAllPaths(String paths) {
		paths = (StringUtils.isNotBlank(paths) ? paths + File.pathSeparator : "") + System.getenv("PATH");

		String defaultPaths = null;
		if (Engine.isLinux() || Engine.isMac()) {
			defaultPaths = "/usr/local/bin";
		} else if (Engine.isWindows()) {
			String programFiles = System.getenv("ProgramW6432");
			if (programFiles != null && programFiles.length() > 0) {
				defaultPaths = programFiles + File.separator + "nodejs";
			}

			programFiles = System.getenv("ProgramFiles");
			if (programFiles != null && programFiles.length() > 0) {
				defaultPaths = (defaultPaths == null ? "" : defaultPaths + File.pathSeparator) + programFiles + File.separator + "nodejs";
			}

			String appData = System.getenv("APPDATA");
			if (appData != null && appData.length() > 0) {
				defaultPaths = (defaultPaths == null ? "" : defaultPaths + File.pathSeparator) + appData + File.separator + "npm";
			}
		}
		paths += File.pathSeparator + defaultPaths;

		return paths;
	}

	public static ProcessBuilder getProcessBuilder(String paths, List<String> command) throws IOException {
		if (command == null || command.size() == 0) {
			throw new IOException("No command paramater");
		}

		paths = getAllPaths(paths);
		command.set(0, searchFullPath(paths, command.get(0)));
		ProcessBuilder pb = new ProcessBuilder(command);
		Map<String, String> pbEnv = pb.environment();
		//		// must set "Path" for Windows 8.1 64
		pbEnv.put(pbEnv.get("PATH") == null ? "Path" : "PATH", paths);
		pbEnv.put("JAVA_HOME", System.getProperty("java.home"));
		configureProxyEnvironment(pbEnv);
		return pb; 
	}

	private static ProcessBuilder getProcessBuilder(String paths, String... command) throws IOException {
		return getProcessBuilder(paths, new LinkedList<String>(Arrays.asList(command)));
	}

	public static ProcessBuilder getNpmProcessBuilder(String paths, List<String> command) throws IOException {
		return getNpmProcessBuilder(paths, null, command);
	}

	/** The working directory must be known before applying the npm script policy. */
	public static ProcessBuilder getNpmProcessBuilder(String paths, File directory, List<String> command) throws IOException {
		if (command == null || command.size() == 0 || (!command.get(0).equals("npm") && !command.get(0).equals("yarn") && !command.get(0).equals("pnpm") && !command.get(0).equals("npx"))) {
			throw new IOException("not a npm or yarn or pnpm command");
		}

		if (Engine.isWindows()) {
			if (command.get(0).equals("npm")) {
				command.set(0, "npm.cmd");
			} else if (command.get(0).equals("yarn")) {
				command.set(0, "yarn.cmd");
			} else if (command.get(0).equals("pnpm")) {
				command.set(0, "pnpm.cmd");
			} else if (command.get(0).equals("npx")) {
				command.set(0, "npx.cmd");
			}
		}

		var pb = getProcessBuilder(paths, command).directory(directory);
		configureNpmEnvironment(pb.environment(), directory == null ? Path.of("").toAbsolutePath() : directory.toPath());
		return pb;
	}

	/** Name of the npm 11 setting that runs the install scripts of all the dependencies, as npm 10 did. */
	static final String NPM_ALLOW_ALL_SCRIPTS_ENV = "npm_config_dangerously_allow_all_scripts";

	private static final ObjectMapper NPM_PACKAGE_JSON = new ObjectMapper();
	private static final Pattern NPM_SCRIPT_POLICY = Pattern.compile(
			"^\\uFEFF?\\s*(?:allow-scripts(?:\\[\\])?|strict-allow-scripts|dangerously-allow-all-scripts)\\s*=");

	/** Keep npm 10 compatibility for projects that have no explicit script policy. */
	static void configureNpmEnvironment(Map<String, String> environment, Path directory) {
		if (environment.keySet().stream().anyMatch(NPM_ALLOW_ALL_SCRIPTS_ENV::equalsIgnoreCase)) {
			logNpmScriptPolicy("keeping the existing environment setting");
			return;
		}
		boolean enabled;
		try {
			enabled = EnginePropertiesManager.getPropertyAsBoolean(EnginePropertiesManager.PropertyName.NPM_ALLOW_ALL_SCRIPTS);
		} catch (IllegalStateException uninitialized) {
			// Process discovery also runs before the engine properties are initialized.
			enabled = true;
		}
		if (!enabled) {
			logNpmScriptPolicy("compatibility is disabled by npm.allow_all_scripts");
		} else if (environment.entrySet().stream().anyMatch(e ->
				(e.getKey().equalsIgnoreCase("npm_config_allow_scripts") && !e.getValue().isBlank())
				|| (e.getKey().equalsIgnoreCase("npm_config_strict_allow_scripts") && "true".equals(e.getValue())))) {
			logNpmScriptPolicy("using the script policy from the environment");
		} else {
			try {
				if (hasNpmScriptPolicy(directory)) {
					logNpmScriptPolicy("using the project script policy");
					return;
				}
			} catch (IOException e) {
				logNpmScriptPolicy("could not read the project policy, leaving npm to handle it");
				return;
			}
			environment.put(NPM_ALLOW_ALL_SCRIPTS_ENV, "true");
			logNpmScriptPolicy("allowing dependency install scripts for compatibility");
		}
	}

	private static boolean hasNpmScriptPolicy(Path directory) throws IOException {
		for (Path current = directory.toAbsolutePath().normalize(); current != null; current = current.getParent()) {
			try (var config = Files.newBufferedReader(current.resolve(".npmrc"))) {
				String line;
				while ((line = config.readLine()) != null) {
					if (NPM_SCRIPT_POLICY.matcher(line).find()) {
						return true;
					}
				}
			} catch (NoSuchFileException e) {
				// No project npm configuration here.
			}
			try (var manifest = Files.newInputStream(current.resolve("package.json"))) {
				var json = NPM_PACKAGE_JSON.readTree(manifest);
				return json != null && json.has("allowScripts");
			} catch (NoSuchFileException e) {
				// npm also looks for the project root above the working directory.
				if (Files.isDirectory(current.resolve("node_modules"))) {
					return false;
				}
			}
		}
		return false;
	}

	private static void logNpmScriptPolicy(String decision) {
		if (Engine.logEngine != null) {
			Engine.logEngine.debug("npm install scripts: " + decision);
		}
	}

	/** Apply the engine proxy to any child process, without exporting upstream credentials. */
	public static void configureProxyEnvironment(Map<String, String> environment) throws IOException {
		String mode;
		try {
			mode = EnginePropertiesManager.getProperty(EnginePropertiesManager.PropertyName.PROXY_SETTINGS_MODE);
		} catch (IllegalStateException uninitialized) {
			// Process discovery also runs before the engine properties are initialized.
			return;
		}
		if (ProxyMode.off.getValue().equals(mode)) return;
		String localProxy = NtlmConnectProxyBridge.getLocalProxyUrl();
		String excluded = EnginePropertiesManager.getProperty(EnginePropertiesManager.PropertyName.PROXY_SETTINGS_BY_PASS_DOMAINS);
		var domains = new java.util.LinkedHashSet<String>();
		if (excluded != null) for (String domain : excluded.split(",")) {
			if (!domain.isBlank()) domains.add(domain.trim());
		}
		domains.add("localhost");
		domains.add("127.0.0.1");
		domains.add("::1");
		for (String key : List.of("HTTP_PROXY", "HTTPS_PROXY", "http_proxy", "https_proxy", "http-proxy", "https-proxy", "npm_config_proxy", "npm_config_https_proxy", "PIP_PROXY")) {
			environment.put(key, localProxy);
		}
		for (String key : List.of("NO_PROXY", "no_proxy", "no-proxy")) environment.put(key, String.join(",", domains));
		// Node versions with built-in environment proxy support also cover native fetch.
		environment.put("NODE_USE_ENV_PROXY", "1");
	}

	public static ProcessBuilder getNpmProcessBuilder(String paths, String... command) throws IOException {
		return getNpmProcessBuilder(paths, null, command);
	}

	public static ProcessBuilder getNpmProcessBuilder(String paths, File directory, String... command) throws IOException {
		return getNpmProcessBuilder(paths, directory, new LinkedList<String>(Arrays.asList(command)));
	}

	/** at most for the extraction of an archive of Node.js or of a JDK */
	private static final Duration TAR_TIMEOUT = Duration.ofMinutes(10);
	/** at most for a command of the Android sdkmanager, which downloads */
	private static final Duration SDK_MANAGER_TIMEOUT = Duration.ofMinutes(30);

	/**
	 * Waits for process at most timeout, then stops its whole tree.
	 *
	 * @return its exit code, -1 when it was stopped
	 */
	private static int waitFor(Process process, Duration timeout, String what) throws InterruptedException {
		if (process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
			return process.exitValue();
		}
		Engine.logEngine.warn(what + " did not end within " + timeout.toMinutes() + " min: it is stopped");
		ProcessTrees.stop(process.toHandle(), Duration.ofSeconds(5));
		return -1;
	}

	/**
	 * Starts the Android sdkmanager (a script that runs java) as a managed process, stopped with its tree when the
	 * engine stops or dies, and after SDK_MANAGER_TIMEOUT: its output then ends, and the reader goes on.
	 */
	private static Process startSdkManager(ProcessBuilder builder) throws IOException {
		var managed = ManagedProcesses.get().start(ManagedProcessSpec.of("sdkmanager").builder(builder));
		CompletableFuture.delayedExecutor(SDK_MANAGER_TIMEOUT.toMillis(), TimeUnit.MILLISECONDS).execute(() -> {
			if (managed.isAlive()) {
				Engine.logEngine.warn("The Android sdkmanager did not end within " + SDK_MANAGER_TIMEOUT.toMinutes() + " min: it is stopped");
				managed.stop();
			}
		});
		return managed.getProcess();
	}

	private static String getNodeOs() {
		// an arm64 Linux (Docker on Apple Silicon, Graviton...) cannot run the x64 build: its own folder keeps them apart
		boolean arm64 = "aarch64".equals(System.getProperty("os.arch"));
		return Engine.isWindows() ? "win-x64" : Engine.isLinux() ? (arm64 ? "linux-arm64" : "linux-x64") : arm64 ? "darwin-arm64" : "darwin-x64";
	}

	private static String getNodeOs(String version) {
		var platform = getNodeOs();
		// Official Apple Silicon archives start at Node.js 16; older templates retain the Intel distribution.
		return platform.equals("darwin-arm64") && VersionUtils.compare(version, "v16.0.0") < 0
				? "darwin-x64" : platform;
	}

	/** The Node.js distributions, rebuildable: in the local working directory when one is configured. */
	private static File getNodesDir() {
		var local = LocalWorkDirectory.getDirectory("nodes");
		return local != null ? local : new File(Engine.USER_WORKSPACE_PATH, "nodes");
	}

	private static File getLocalNodeDir(String version) {
		if (version.equals(defaultNodeVersion)) {
			var override = System.getProperty("convertigo.node.path", "").trim();
			if (!override.isEmpty()) {
				var bin = nodeBinDirectory(new File(override));
				if (bin == null) {
					throw new IllegalArgumentException("convertigo.node.path must contain an executable Node.js runtime");
				}
				return bin;
			}
			if (Engine.WEBAPP_PATH != null && !Engine.WEBAPP_PATH.isEmpty()) {
				var bundled = new File(Engine.WEBAPP_PATH, "WEB-INF/node");
				try {
					var marker = new File(bundled, ".convertigo-node-version");
					if (marker.isFile() && version.equals(Files.readString(marker.toPath()).trim())) {
						var bin = nodeBinDirectory(bundled);
						if (bin != null) {
							return bin;
						}
					}
				} catch (IOException e) {
					Engine.logEngine.warn("Cannot read the bundled Node.js version", e);
				}
			}
			var bin = nodeBinDirectory(defaultNodeDir);
			if (bin != null) {
				return bin;
			}
		}
		return new File(getNodesDir(), "node-" + version + "-" + getNodeOs(version));
	}

	private static File nodeBinDirectory(File directory) {
		if (directory == null) {
			return null;
		}
		var bin = Engine.isWindows() || new File(directory, "node").isFile() ? directory : new File(directory, "bin");
		var node = new File(bin, Engine.isWindows() ? "node.exe" : "node");
		return node.isFile() && (Engine.isWindows() || node.canExecute()) ? bin : null;
	}

	public static SortedSet<String> getNodeVersions() {
		Pattern pv = Pattern.compile("(?:node-)?(v?(\\d+)\\.\\d+\\.\\d+)(?:-(.*))?");

		SortedSet<String> versions = new TreeSet<>(new Comparator<String>() {

			@Override
			public int compare(String v1, String v2) {
				return VersionUtils.compare(v1, v2) * -1;
			}
		});

		File npms = getNodesDir();
		if (npms.exists()) {
			String os = getNodeOs();
			for (File f : npms.listFiles()) {
				Matcher m = pv.matcher(f.getName());
				if (m.matches() && os.equals(m.group(3))) {
					versions.add(m.group(1));
				}
			}
		}

		HttpGet get = new HttpGet("https://nodejs.org/dist/");
		try (CloseableHttpResponse response = Engine.theApp.httpClient4.execute(get)) {
			String html = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
			Matcher m = pv.matcher(html);
			while (m.find()) {
				if (Integer.parseInt(m.group(2)) >= 8) {
					versions.add(m.group(1));
				}
			}
		} catch (Exception e) {
		}
		return versions;
	}

	public static File getNodeDir(String version) throws Exception {
		return getNodeDir(version, null);
	}

	public static File getNodeDir(String version, ProgressListener progress) throws Exception {
		return getNodeDir(version, progress, null);
	}

	/** Reports download, verification and extraction to a Studio build output as well as the engine log. */
	public static File getNodeDir(String version, ProgressListener progress, java.util.function.Consumer<String> status) throws Exception {
		synchronized (nodeInstallLock) {
			File dir = getLocalNodeDir(version);
			var installed = nodeBinDirectory(dir);
			if (installed != null) {
				return installed;
			}
			var archives = new File(Engine.USER_WORKSPACE_PATH, "nodes");
			return new NodeRuntimeInstaller(version, getNodeOs(version), dir, archives, progress, status).install();
		}
	}

	public static File getJDK8(ProgressListener progress) throws ClientProtocolException, IOException, JSONException, InterruptedException {	
		return getJDK(8, progress);
	}
	
	public static File getJDK(int version, ProgressListener progress) throws ClientProtocolException, IOException, JSONException, InterruptedException {
		File dir;
		String env = System.getenv("JAVA_HOME_" + version + "_X64");
		if (env != null) {
			dir = new File(env);
			if (dir.exists() && new File(dir, "bin").exists()) {
				Engine.logEngine.info("Use the JDK 8 from env JAVA_HOME_" + version + "_X64: " + dir);
				return dir;
			}
		}
		String os = Engine.isWindows() ? "windows" : Engine.isLinux() ? "linux" : "mac";
		dir = new File(Engine.USER_WORKSPACE_PATH, "jdk/jdk-" + version + "-" + os);
		if (dir.exists()) {
			return dir;
		}
		HttpGet get = new HttpGet("https://api.adoptium.net/v3/assets/latest/" + version + "/hotspot?vendor=eclipse");
		String content;
		try (CloseableHttpResponse response = Engine.theApp.httpClient4.execute(get)) {
			content = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
		}
		JSONArray array = new JSONArray(content);
		String url = null;
		String release = null;
		for (int i = 0; url == null && i < array.length(); i++) {
			release = array.getJSONObject(i).getString("release_name");
			JSONObject obj = array.getJSONObject(i).getJSONObject("binary");
			if ("x64".equals(obj.getString("architecture")) &&
					os.equals(obj.getString("os")) &&
					"jdk".equals(obj.getString("image_type"))) {
				obj = obj.getJSONObject("package");
				url = obj.getString("link");
			}
		}
		if (url == null) {
			return dir;
		}
		File archive = new File(dir.getAbsolutePath() + url.replaceFirst(".*/(.*?)$", "$1"));
		if (!archive.exists() ) {
			get = new HttpGet(url);
			try (CloseableHttpResponse response = Engine.theApp.httpClient4.execute(get)) {
				FileUtils.deleteQuietly(archive);
				archive.getParentFile().mkdirs();
				if (progress != null) {
					long length = response.getEntity().getContentLength();
					try (FileOutputStream fos = new FileOutputStream(archive)) {
						InputStream is = response.getEntity().getContent();
						byte[] buf = new byte[1024 * 1024];
						int n;
						long t = 0, now, ts = 0;
						while ((n = is.read(buf)) > -1) {
							fos.write(buf, 0, n);
							t += n;
							now = System.currentTimeMillis();
							if (now > ts) {
								progress.update(t, length, 1);
								ts = now + 2000;
							}
						}
						progress.update(t, length, 1);
					}
				} else {
					FileUtils.copyInputStreamToFile(response.getEntity().getContent(), archive);
				}
			}
		}

		if (Engine.isWindows()) {
			Level l = Engine.logEngine.getLevel();
			try {
				Engine.logEngine.info("prepare to unzip " + archive.getAbsolutePath() + " to " + dir.getAbsolutePath());
				Engine.logEngine.setLevel(Level.OFF);
				ZipUtils.expandZip(archive.getAbsolutePath(), dir.getAbsolutePath(), release);
				Engine.logEngine.setLevel(l);
				Engine.logEngine.info("unzip terminated!");
			} finally {
				Engine.logEngine.setLevel(l);
			}
		} else {
			Engine.logEngine.info("tar -zxf " + archive.getAbsolutePath() + " into " + archive.getParentFile());
			dir.mkdirs();
			waitFor(ProcessUtils.getProcessBuilder(null, "tar", "--strip-components=" + (Engine.isLinux() ? 1 : 3), "-zxf", archive.getAbsolutePath()).directory(dir)
			.redirectError(Redirect.DISCARD).redirectOutput(Redirect.DISCARD).start(), TAR_TIMEOUT, "tar");
		}
		archive.delete();
		Engine.logEngine.info("jdk dir: " + dir);
		return dir;
	}

	public static File getAndroidSDK(ProgressListener progress) throws Exception {
		return getAndroidSDK(null, progress);
	}

	public static File getAndroidSDK(String preferedAndroidBuildTools, ProgressListener progress) throws Exception {
		File dir;
		String env = System.getenv("ANDROID_HOME");
		if (env != null) {
			dir = new File(env);
			if (new File(dir, "tools").exists() || new File(dir, "cmdline-tools").exists()) {
				Engine.logEngine.info("Use the ANDROID SDK from env ANDROID_HOME: " + dir);
				return dir;
			}
		}

		dir = new File(Engine.USER_WORKSPACE_PATH, "android-sdk");

		if (!new File(dir, "tools").exists() && !new File(dir, "cmdline-tools").exists()) {
			HttpGet get = new HttpGet("https://developer.android.com/studio");
			String content;
			try (CloseableHttpResponse response = Engine.theApp.httpClient4.execute(get)) {
				content = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
			}
			String os = Engine.isWindows() ? "win" : Engine.isLinux() ? "linux" : "mac";
			Matcher m = Pattern.compile("\"(https://dl.google.com/android/repository/commandlinetools-" + os + "-.*?(\\..*?))\"").matcher(content);
			if (!m.find()) {
				Engine.logEngine.error("Cannot find Android SDK Manager link");
			}
			Engine.logEngine.info("Will download Android SDK Manager from: " + m.group(1));
			File archive = new File(dir.getAbsoluteFile() + m.group(2));

			archive.delete();

			get = new HttpGet(m.group(1));
			try (CloseableHttpResponse response = Engine.theApp.httpClient4.execute(get)) {
				FileUtils.deleteQuietly(archive);
				archive.getParentFile().mkdirs();
				if (progress != null) {
					long length = response.getEntity().getContentLength();
					try (FileOutputStream fos = new FileOutputStream(archive)) {
						InputStream is = response.getEntity().getContent();
						byte[] buf = new byte[1024 * 1024];
						int n;
						long t = 0, now, ts = 0;
						while ((n = is.read(buf)) > -1) {
							fos.write(buf, 0, n);
							t += n;
							now = System.currentTimeMillis();
							if (now > ts) {
								progress.update(t, length, 1);
								ts = now + 2000;
							}
						}
						progress.update(t, length, 1);
					}
				} else {
					FileUtils.copyInputStreamToFile(response.getEntity().getContent(), archive);
				}
			}
			Level l = Engine.logEngine.getLevel();
			try {
				Engine.logEngine.info("prepare to unzip " + archive.getAbsolutePath() + " to " + dir.getAbsolutePath());
				Engine.logEngine.setLevel(Level.OFF);
				ZipUtils.expandZip(archive.getAbsolutePath(), dir.getAbsolutePath(), null);
				Engine.logEngine.setLevel(l);
				Engine.logEngine.info("unzip terminated!");
			} finally {
				Engine.logEngine.setLevel(l);
			}
			archive.delete();
		}

		File binDir = null;
		for (File f: dir.listFiles()) {
			binDir = new File(f, "bin");
			if (binDir.exists()) {
				break;
			}
		}
		if (binDir == null) {
			String msg = "no bin folder found in: " + Arrays.toString(dir.list());
			Engine.logEngine.error(msg);
			throw new EngineException(msg);
		}
		if (!Engine.isWindows()) {
			for (File bin: binDir.listFiles()) {
				bin.setExecutable(true);
			}
		}


		File buildToolsDir = null;
		try {
			buildToolsDir = new File(dir, "build-tools").listFiles()[0];
		} catch (Exception e) {
		}
		
		Process p;
		int code;
		String output;
		if (buildToolsDir == null) {
			Engine.logEngine.info("Android commands");
			p = startSdkManager(ProcessUtils.getProcessBuilder(binDir.getAbsolutePath(), Engine.isWindows() ? "sdkmanager.bat" : "sdkmanager", "--licenses", "--sdk_root=" + dir.getAbsolutePath()).redirectErrorStream(true));
			BufferedOutputStream bos = new BufferedOutputStream(p.getOutputStream());
			BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream(), "UTF-8"));
			Engine.execute(() -> {
				try {
					while (true) {
						bos.write("y\n".getBytes("UTF-8"));
						bos.flush();
					}
				} catch (IOException e) {
					// end of process
				} catch (Exception e) {
					Engine.logEngine.info("Boom: " + e);
				}
			});
			String line;
			output = "";
			while ((line = br.readLine()) != null) {
				Engine.logEngine.info("Android licenses: " + line);
			}
			code = p.waitFor();
			Engine.logEngine.info("Android licenses: " + code + "\n" + output);
		}
		
		if (buildToolsDir == null) {
			String buildTools = "";
			if (StringUtils.isNotBlank(preferedAndroidBuildTools)) {
				buildTools = preferedAndroidBuildTools;
				if (!buildTools.startsWith("build-tools;")) {
					buildTools = "build-tools;" + buildTools; 
				}
				Engine.logEngine.info("use prefered build-tools: " + buildTools);
			} else {
				p = startSdkManager(ProcessUtils.getProcessBuilder(binDir.getAbsolutePath(), Engine.isWindows() ? "sdkmanager.bat" : "sdkmanager", "--list", "--sdk_root=" + dir.getAbsolutePath()).redirectErrorStream(true));
				output = IOUtils.toString(p.getInputStream(), "UTF-8");
				code = p.waitFor();
				Engine.logEngine.info("Android package list: " + code + "\n" + output);
	
				Matcher m = Pattern.compile(".*(build-tools;[0-9.]+?) .*").matcher(output);
				while (m.find()) {
					buildTools = m.group(1);
				}
				Engine.logEngine.info("build-tools: " + buildTools);
			}
			p = startSdkManager(ProcessUtils.getProcessBuilder(binDir.getAbsolutePath(), Engine.isWindows() ? "sdkmanager.bat" : "sdkmanager", "--sdk_root=" + dir.getAbsolutePath(), buildTools).redirectErrorStream(true));
			output = IOUtils.toString(p.getInputStream(), "UTF-8");
			code = p.waitFor();
			Engine.logEngine.info("Android install build-tools: " + code + "\n" + output);
			try {
				buildToolsDir = new File(dir, "build-tools").listFiles()[0];
			} catch (Exception e) {
			}
		}
		
		if (buildToolsDir != null) {
			try {
				String suffix = Engine.isWindows() ? ".bat" : "";
				File dxbin = new File(buildToolsDir, "dx" + suffix);
				if (!dxbin.exists()) {
					FileUtils.copyFile(new File(buildToolsDir, "d8" + suffix), dxbin);
					dxbin.setExecutable(true);
				}
				dxbin = new File(buildToolsDir, "lib/dx.jar");
				if (!dxbin.exists()) {
					FileUtils.copyFile(new File(buildToolsDir, "lib/d8.jar"), dxbin);
				}
			} catch (Exception e) {
				Engine.logEngine.warn("Failed to path build-tools for dx: " + e.getMessage());
			}
		}

		return dir;
	}

	public static File getGradle(ProgressListener progress) throws Exception {
		File dir = new File(Engine.USER_WORKSPACE_PATH, "gradle");
		File gradle = new File(dir, "bin/gradle");

		if (!gradle.exists()) {
			gradle = new File(searchFullPath(getAllPaths(null), "gradle"));
			Engine.logEngine.info("Found system Gradle: " + gradle.getAbsolutePath());
		}

		if (!gradle.exists()) {
			File dists = new File(System.getProperty("user.home"), ".gradle/wrapper/dists");
			Engine.logEngine.info("Check gradle at: " + dists + " " + dists.exists());
			if (dists.exists()) {
				File[] gradles = dists.listFiles();
				Arrays.sort(gradles);
				Engine.logEngine.info("Gradles: " + gradles);
				for (int i = gradles.length - 1; i >= 0; i--) {
					try {
						File eGradle = new File(gradles[i].listFiles()[0].listFiles((FileFilter) DirectoryFileFilter.DIRECTORY)[0], "bin/gradle");
						if (eGradle.exists()) {
							gradle = eGradle;
							dir = gradle.getParentFile().getParentFile();
							Engine.logEngine.info("Will use existing gradle from: " + dir);
							break;
						}
					} catch (Exception e) {
						Engine.logEngine.info("Check gradle failed for " + gradles[i] + ": " + e);
					}
				}
			}
		}

		if (!gradle.exists()) {
			HttpGet get = new HttpGet("https://gradle.org/install");
			String content;
			try (CloseableHttpResponse response = Engine.theApp.httpClient4.execute(get)) {
				content = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
			}
			Matcher m = Pattern.compile("(gradle-.*?)-bin\\.zip").matcher(content);
			if (!m.find()) {
				Engine.logEngine.error("Cannot find Gradle link");
			}
			Engine.logEngine.info("Will download Gradle from: https://downloads.gradle.org/distributions/" + m.group());
			File archive = new File(Engine.USER_WORKSPACE_PATH, m.group());

			archive.delete();

			get = new HttpGet("https://downloads.gradle.org/distributions/" + m.group());
			try (CloseableHttpResponse response = Engine.theApp.httpClient4.execute(get)) {
				FileUtils.deleteQuietly(archive);
				archive.getParentFile().mkdirs();
				if (progress != null) {
					long length = response.getEntity().getContentLength();
					try (FileOutputStream fos = new FileOutputStream(archive)) {
						InputStream is = response.getEntity().getContent();
						byte[] buf = new byte[1024 * 1024];
						int n;
						long t = 0, now, ts = 0;
						while ((n = is.read(buf)) > -1) {
							fos.write(buf, 0, n);
							t += n;
							now = System.currentTimeMillis();
							if (now > ts) {
								progress.update(t, length, 1);
								ts = now + 2000;
							}
						}
						progress.update(t, length, 1);
					}
				} else {
					FileUtils.copyInputStreamToFile(response.getEntity().getContent(), archive);
				}
			}
			Level l = Engine.logEngine.getLevel();
			try {
				Engine.logEngine.info("prepare to unzip " + archive.getAbsolutePath() + " to " + dir.getAbsolutePath());
				Engine.logEngine.setLevel(Level.OFF);
				ZipUtils.expandZip(archive.getAbsolutePath(), dir.getAbsolutePath(), m.group(1));
				Engine.logEngine.setLevel(l);
				Engine.logEngine.info("unzip terminated!");
				gradle = new File(dir, "bin/gradle");
			} finally {
				Engine.logEngine.setLevel(l);
			}
			archive.delete();
		}
		gradle.setExecutable(true);

		return dir;
	}
}
