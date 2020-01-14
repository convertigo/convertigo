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
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.ProxyMethod;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.ProxyMode;

public class ProcessUtils {
	
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
		paths = (paths != null && paths.length() > 0 ? paths + File.pathSeparator : "") + System.getenv("PATH");
		
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
		if (command == null || command.size() == 0 || !command.get(0).equals("npm")) {
			throw new IOException("not a npm command");
		}
		
		if (Engine.isWindows()) {
			command.set(0, "npm.cmd");
		}
		
		if (paths.isEmpty()) {
			paths = npmPath;
		} else {
			paths = npmPath + File.pathSeparator + paths;
		}
		
		ProcessBuilder pb = getProcessBuilder(paths, command);
		
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
}
