/*
 * Copyright (c) 2001-2020 Convertigo SA.
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.fileupload.ProgressListener;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.log4j.Level;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.ProxyMethod;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.ProxyMode;

public class ProcessUtils {
	public static final String defaultNodeVersion = "v10.19.0";
	
	public static void setNpmFolder(File npmFolder) {
		if (new File(npmFolder, "npm").exists()) {
			npmPath = npmFolder.getAbsolutePath();
		}
	}
	
	private static String npmPath = null;
	
	public static String searchFullPath(String paths, String command) throws IOException {
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
	
	public static String getAllPaths(String paths) {
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
	
	public static ProcessBuilder getProcessBuilder(String paths, String... command) throws IOException {
		return getProcessBuilder(paths, new LinkedList<String>(Arrays.asList(command)));
	}
	
	public static ProcessBuilder getNpmProcessBuilder(String paths, List<String> command) throws IOException {
		if (command == null || command.size() == 0 || !(command.get(0).equals("npm") || command.get(0).equals("node"))) {
			throw new IOException("not a npm command");
		}
		
		if (Engine.isWindows()) {
			command.set(0, "npm.cmd");
		}
		
		if (StringUtils.isBlank(paths)) {
			paths = npmPath;
		} else if (npmPath != null) {
			paths = npmPath + File.pathSeparator + paths;
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
				
				pbEnv.put("http-proxy", "http://" + npmProxy);
				pbEnv.put("https-proxy", "http://" + npmProxy);
			}
		}
		
		return pb;
	}
	
	public static ProcessBuilder getNpmProcessBuilder(String paths, String... command) throws IOException {
		return getNpmProcessBuilder(paths, new LinkedList<String>(Arrays.asList(command)));
	}
	
	public static String getNodeOs() {
		return Engine.isWindows() ? "win-x64" : Engine.isLinux() ? "linux-x64" : "darwin-x64";
	}
	
	public static File getLocalNodeDir(String version) {
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
		File dir = getLocalNodeDir(version);
		Engine.logEngine.info("getLocalNodeDir " + dir + (dir.exists() ? " exists" : " doesn't exist"));
		if (dir.exists()) {
			return dir;
		}
		File archive = new File(dir.getPath() + (Engine.isWindows() ? ".zip" : ".tar.gz"));
		HttpGet get = new HttpGet("https://nodejs.org/dist/" + version + "/" + archive.getName());
		
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
				Engine.logEngine.setLevel(Level.OFF);
				Engine.logEngine.info("prepare to unzip " + archive.getAbsolutePath() + " to " + dir.getAbsolutePath());
				ZipUtils.expandZip(archive.getAbsolutePath(), dir.getAbsolutePath(), dir.getName());
				Engine.logEngine.info("unzip terminated!");
			} finally {
				Engine.logEngine.setLevel(l);
			}
		} else {
			Engine.logEngine.info("tar -zxf " + archive.getAbsolutePath() + " into " + archive.getParentFile());
			ProcessUtils.getProcessBuilder(null, "tar", "-zxf", archive.getAbsolutePath()).directory(archive.getParentFile()).start().waitFor();
			dir = new File(dir, "bin");
		}
		FileUtils.deleteQuietly(archive);
		return dir;
	}
}
