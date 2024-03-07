/*
 * Copyright (c) 2001-2024 Convertigo SA.
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
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
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

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.ProxyMethod;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.ProxyMode;

public class ProcessUtils {
	private static String defaultNodeVersion = "v16.18.0";
	private static File defaultNodeDir;

	public static String getDefaultNodeVersion() {
		return defaultNodeVersion;
	}

	public static File getDefaultNodeDir() {
		if (defaultNodeDir == null) {
			defaultNodeDir = getLocalNodeDir(defaultNodeVersion);
		}
		return defaultNodeDir;
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
				Process p = new ProcessBuilder(npmFile.getAbsolutePath(), "-version").start();
				if (p.waitFor(5, TimeUnit.SECONDS)) {
					try (InputStream is = p.getInputStream()) {
						version = IOUtils.toString(is, Charset.defaultCharset()).trim();
					}
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
		String version = getNodeVersion(nodeDir);
		if (version != null && (!version.equals(defaultNodeVersion) || defaultNodeDir == null)) {
			defaultNodeVersion = version;
			defaultNodeDir = nodeDir;
		}
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
		return pb; 
	}

	private static ProcessBuilder getProcessBuilder(String paths, String... command) throws IOException {
		return getProcessBuilder(paths, new LinkedList<String>(Arrays.asList(command)));
	}

	public static ProcessBuilder getNpmProcessBuilder(String paths, List<String> command) throws IOException {
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

		ProcessBuilder pb = getProcessBuilder(paths, command);

		if (Engine.isCliMode()) {
			return pb;
		}
		String proxyMode = EnginePropertiesManager.getProperty(EnginePropertiesManager.PropertyName.PROXY_SETTINGS_MODE);
		if (proxyMode.equals(ProxyMode.manual.getValue())) {
			String proxyAuthMethod = EnginePropertiesManager.getProperty(EnginePropertiesManager.PropertyName.PROXY_SETTINGS_METHOD);

			if (proxyAuthMethod.equals(ProxyMethod.anonymous.getValue()) || proxyAuthMethod.equals(ProxyMethod.basic.getValue())) {
				Map<String, String> pbEnv = pb.environment();
				String proxyHost = EnginePropertiesManager.getProperty(EnginePropertiesManager.PropertyName.PROXY_SETTINGS_HOST);
				String proxyPort = EnginePropertiesManager.getProperty(EnginePropertiesManager.PropertyName.PROXY_SETTINGS_PORT);

				String npmProxy = proxyHost + ":" + proxyPort;

				if (proxyAuthMethod.equals(ProxyMethod.basic.getValue())) {
					String proxyUser = EnginePropertiesManager.getProperty(EnginePropertiesManager.PropertyName.PROXY_SETTINGS_USER);
					String proxyPassword = EnginePropertiesManager.getProperty(EnginePropertiesManager.PropertyName.PROXY_SETTINGS_PASSWORD);

					npmProxy = proxyUser + ":" + proxyPassword + "@" + npmProxy;
				}

				String noProxy = EnginePropertiesManager.getProperty(EnginePropertiesManager.PropertyName.PROXY_SETTINGS_BY_PASS_DOMAINS);

				pbEnv.put("http-proxy", "http://" + npmProxy);
				pbEnv.put("https-proxy", "http://" + npmProxy);
				pbEnv.put("no-proxy", noProxy);

				pbEnv.put("HTTP_PROXY", "http://" + npmProxy);
				pbEnv.put("HTTPS_PROXY", "http://" + npmProxy);
				pbEnv.put("NO_PROXY", noProxy);
			}
		}

		return pb;
	}

	public static ProcessBuilder getNpmProcessBuilder(String paths, String... command) throws IOException {
		return getNpmProcessBuilder(paths, new LinkedList<String>(Arrays.asList(command)));
	}

	private static String getNodeOs() {
		return Engine.isWindows() ? "win-x64" : Engine.isLinux() ? "linux-x64" : "aarch64".equals(System.getProperty("os.arch")) ? "darwin-arm64" : "darwin-x64";
	}

	private static File getLocalNodeDir(String version) {
		if (version.equals(defaultNodeVersion) && defaultNodeDir != null) {
			return defaultNodeDir;
		}
		return new File(Engine.USER_WORKSPACE_PATH, "nodes/node-" + version + "-" + getNodeOs());
	}

	public static SortedSet<String> getNodeVersions() {
		Pattern pv = Pattern.compile("(?:node-)?(v?(\\d+)\\.\\d+\\.\\d+)(?:-(.*))?");

		SortedSet<String> versions = new TreeSet<>(new Comparator<String>() {

			@Override
			public int compare(String v1, String v2) {
				return VersionUtils.compare(v1, v2) * -1;
			}
		});

		File npms = new File(Engine.USER_WORKSPACE_PATH, "nodes");
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
		synchronized (defaultNodeVersion) {
			File dir = getLocalNodeDir(version);
			Engine.logEngine.info("getLocalNodeDir " + dir + (dir.exists() ? " exists" : " doesn't exist"));
			if (dir.exists()) {
				if (!Engine.isWindows() && !dir.getName().equals("bin")) {
					dir = new File(dir, "bin");
				}
				return dir;
			}
			File archive = new File(dir.getPath() + (Engine.isWindows() ? ".zip" : ".tar.gz"));
			HttpGet get = new HttpGet("https://nodejs.org/dist/" + version + "/" + archive.getName());
			boolean retry;
			do {
				retry = false;
				Engine.logEngine.info("getNodeDir archive " + dir + " downloaded from " + get.getURI().toString());

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
				if (Engine.isWindows()) {
					Level l = Engine.logEngine.getLevel();
					try {
						Engine.logEngine.info("prepare to unzip " + archive.getAbsolutePath() + " to " + dir.getAbsolutePath());
						Engine.logEngine.setLevel(Level.OFF);
						ZipUtils.expandZip(archive.getAbsolutePath(), dir.getAbsolutePath(), dir.getName());
						Engine.logEngine.setLevel(l);
						Engine.logEngine.info("unzip terminated!");
					} finally {
						Engine.logEngine.setLevel(l);
					}
				} else {
					Engine.logEngine.info("tar -zxf " + archive.getAbsolutePath() + " into " + archive.getParentFile());
					int status = ProcessUtils.getProcessBuilder(null, "tar", "-zxf", archive.getAbsolutePath()).directory(archive.getParentFile())
							.redirectError(Redirect.DISCARD).redirectOutput(Redirect.DISCARD).start().waitFor();
					if (archive.getName().contains("darwin-arm64")) {
						if (status != 0) {
							retry = true;
							get = new HttpGet("https://nodejs.org/dist/" + version + "/" + archive.getName().replace("arm64", "x64"));
						} else if (!dir.exists()) {
							File dir64 = new File(dir.getParentFile(), dir.getName().replace("arm64", "x64"));
							if (dir64.exists()) {
								dir64.renameTo(dir);
							}
						}
					}
					if (!retry) {
						dir = new File(dir, "bin");
					}
				}
				FileUtils.deleteQuietly(archive);
			} while(retry);
			return dir;
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
			ProcessUtils.getProcessBuilder(null, "tar", "--strip-components=" + (Engine.isLinux() ? 1 : 3), "-zxf", archive.getAbsolutePath()).directory(dir)
			.redirectError(Redirect.DISCARD).redirectOutput(Redirect.DISCARD).start().waitFor();
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
			p = ProcessUtils.getProcessBuilder(binDir.getAbsolutePath(), Engine.isWindows() ? "sdkmanager.bat" : "sdkmanager", "--licenses", "--sdk_root=" + dir.getAbsolutePath()).redirectErrorStream(true).start();
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
				p = ProcessUtils.getProcessBuilder(binDir.getAbsolutePath(), Engine.isWindows() ? "sdkmanager.bat" : "sdkmanager", "--list", "--sdk_root=" + dir.getAbsolutePath()).redirectErrorStream(true).start();
				output = IOUtils.toString(p.getInputStream(), "UTF-8");
				code = p.waitFor();
				Engine.logEngine.info("Android package list: " + code + "\n" + output);
	
				Matcher m = Pattern.compile(".*(build-tools;[0-9.]+?) .*").matcher(output);
				while (m.find()) {
					buildTools = m.group(1);
				}
				Engine.logEngine.info("build-tools: " + buildTools);
			}
			p = ProcessUtils.getProcessBuilder(binDir.getAbsolutePath(), Engine.isWindows() ? "sdkmanager.bat" : "sdkmanager", "--sdk_root=" + dir.getAbsolutePath(), buildTools).redirectErrorStream(true).start();
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
