/*
 * Copyright (c) 2001-2016 Convertigo SA.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 *
 * $URL$
 * $Author$
 * $Revision$
 * $Date$
 */

package com.twinsoft.convertigo.engine.mobile;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.ProxyMethod;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.ProxyMode;

public class BuilderCommand {

	private String cmdOutput = "";
	private String errorLines = "";
	
	private boolean processCanceled = false;
	private Process process = null;
    
	private OS osLocal = null;

	String defaultPaths = null;
	
	private enum OS {
		generic,
		linux,
		mac,
		win32,
		solaris;
	}
	
	public BuilderCommand() {
		// Get defaultPaths
		if (is(OS.mac) || is(OS.linux)) {
			defaultPaths = "/usr/local/bin";
		} else if (is(OS.win32)) {
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
	}

    protected String getAdditionalPath() {
    	return "";
    }
	
    protected static String getFullPath(String paths, String command) throws IOException {
		for (String path: paths.split(Pattern.quote(File.pathSeparator))) {
			File candidate = new File(path, command);
			if (candidate.exists()) {
				return candidate.getCanonicalPath();
			}
		}
		return null;
	}
    
	protected boolean is(OS os) {
		return getOsLocal() == os;
	}
    
	protected OS getOsLocal() {
		if (osLocal == null) {
			String osname = System.getProperty("os.name", "generic").toLowerCase();
			if (osname.startsWith("windows")) {
				osLocal = OS.win32;
			} else if (osname.startsWith("linux")) {
				osLocal = OS.linux;
			} else if (osname.startsWith("sunos")) {
				osLocal = OS.solaris;
			} else if (osname.startsWith("mac") || osname.startsWith("darwin")) {
				osLocal = OS.mac;
			} else {
				osLocal = OS.generic;
			}
		}
		return osLocal;
	}
	
	public String run(File launchDir, String command, List<String> parameters, boolean mergeError) throws Throwable {
		if (is(OS.win32)) {
			// Works for cordova and npm
			command += ".cmd";
		 }
		
		String shellFullpath = command;
		String paths = getAdditionalPath();
		paths = (paths.length() > 0 ? paths + File.pathSeparator : "") + System.getenv("PATH");
		paths += File.pathSeparator + defaultPaths;
		
		// Checks if the command is already full path 
		if (!(new File(shellFullpath).exists())) {
			// Else search where the "exec" is and build the absolute path for this "exec"
			shellFullpath = getFullPath(paths, command);
			
			// If the "exec" is not found then it search it elsewhere
			if (shellFullpath == null) {
				shellFullpath = command;
			}
		}
		
		// Prepares the command
		parameters.add(0, shellFullpath);
		ProcessBuilder pb = new ProcessBuilder(parameters);
		// Set the directory from where the command will be executed
		pb.directory(launchDir.getCanonicalFile());		
		
		Map<String, String> pbEnv = pb.environment();		
		// must set "Path" for Windows 8.1 64
		pbEnv.put(pbEnv.get("PATH") == null ? "Path" : "PATH", paths);
		
		// Specific to npm command
		if (shellFullpath.endsWith("npm") || shellFullpath.endsWith("npm.cmd")) {
			
			// Set the proxy for npm
			String proxyMode = EnginePropertiesManager.getProperty(EnginePropertiesManager.PropertyName.PROXY_SETTINGS_MODE);
			if (proxyMode.equals(ProxyMode.manual.getValue())) {
				String proxyAuthMethod = EnginePropertiesManager.getProperty(EnginePropertiesManager.PropertyName.PROXY_SETTINGS_METHOD);

				if (proxyAuthMethod.equals(ProxyMethod.anonymous.getValue()) || proxyAuthMethod.equals(ProxyMethod.basic.getValue())) {
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
		}
		
		pb.redirectErrorStream(mergeError);
		
		Engine.logEngine.info("Executing command : " + parameters);
		
		process = pb.start();
		cmdOutput = "";
		errorLines = "";
		processCanceled = false;
		
		// Logs the output
		Thread outThread = new Thread(new Runnable() {
			@Override
	        public void run() {
				try {
					String line;
					BufferedReader bis = new BufferedReader(new InputStreamReader(process.getInputStream()));
					while ((line = bis.readLine()) != null) {
						Engine.logEngine.debug(line);
						BuilderCommand.this.cmdOutput += line;
					}
				} catch (Exception e) {
					Engine.logEngine.error("Error while executing command", e);
				}
			}
		});
		outThread.start();
		
		Thread errThread = null;
		if (!mergeError) {
			// Logs the error output
			errThread = new Thread(new Runnable() {
				@Override
		        public void run() {
					try {
						String line;
						BufferedReader bis = new BufferedReader(new InputStreamReader(process.getErrorStream()));
						while ((line = bis.readLine()) != null) {
							Engine.logEngine.error(line);
							BuilderCommand.this.errorLines += line;
						}
					} catch (Exception e) {
						Engine.logEngine.error("Error while executing command", e);
					}
				}
			});
			errThread.start();
		}
		
		int exitCode = process.waitFor();
		Engine.logEngine.debug("Command ended : "+ command + " " + parameters+ ". Exit code :"+ exitCode);
		
		if (exitCode != 0 && !processCanceled) {
			throw new Exception("Exit code " + exitCode + " when running the command '" + command + 
					"' with parameters : '" + parameters + "'. The output of the command is : '" 
					 + cmdOutput + "'");
		}
		
		return cmdOutput;
	}
	
	public boolean isCanceled() {
		return processCanceled;
	}

	public void cancel() {
		if (process != null) {
			processCanceled = true;
			process.destroy();
		}
	}
	
	public String getErrorLines() {
		return errorLines;
	}

	public String getOutputLines() {
		return cmdOutput;
	}
	
}
