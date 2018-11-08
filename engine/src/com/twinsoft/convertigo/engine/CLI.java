package com.twinsoft.convertigo.engine;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.enums.MobileBuilderBuildMode;
import com.twinsoft.convertigo.engine.mobile.MobileBuilder;
import com.twinsoft.convertigo.engine.util.FileUtils;
import com.twinsoft.convertigo.engine.util.NetworkUtils;
import com.twinsoft.convertigo.engine.util.ProcessUtils;

public class CLI {
	private static Pattern pRemoveEchap = Pattern.compile("\\x1b\\[\\d+m");
	private static Pattern pIsServerRunning = Pattern.compile(".*?server running: (http\\S*).*");
	
	private static void appendOutput(String... txt) {
		for (String t: txt) {
			System.out.println(t);
		}
	}
	
	public static void main(String[] args) throws Exception {
		System.setProperty(Engine.JVM_PROPERTY_USER_WORKSPACE, "c:/TMP/c8oCLI");
		Engine.bCliMode = true;
		Engine.initPaths("c:/TMP/c8oCLI/no");
		Engine.start();
		Project project = Engine.theApp.databaseObjectsManager.deployProject("C:\\Users\\Nicolas\\Downloads\\sncfCmsApp.car", true);
		System.out.println("project: " + project.getQName());
		
		File ionicDir = new File(project.getDirPath() + "/_private/ionic");
		File nodeModules = new File(ionicDir, "node_modules");
		
		
		MobileBuilder mb = project.getMobileBuilder();
		boolean forceInstall = true;
		boolean forceClean = false;
		MobileBuilderBuildMode buildMode = MobileBuilderBuildMode.fast;
		if (forceInstall || !nodeModules.exists() || mb.getNeedPkgUpdate()) {
			boolean[] running = {true};
			try {
				new File(ionicDir, "package-lock.json").delete();
				
				if (forceClean) {
					appendOutput("...", "...", "Removing existing node_modules... This can take several seconds...");
					Engine.logStudio.info("Removing existing node_modules... This can take several seconds...");
					com.twinsoft.convertigo.engine.util.FileUtils.deleteQuietly(nodeModules);
				}
				appendOutput("Installing node_modules... This can take several minutes depending on your network connection speed...");
				Engine.logStudio.info("Installing node_modules... This can take several minutes depending on your network connection speed...");
				
				long start = System.currentTimeMillis();
				ProcessBuilder pb = ProcessUtils.getNpmProcessBuilder("", "npm", "install", ionicDir.toString(), "--no-shrinkwrap", "--no-package-lock");
				pb.redirectErrorStream(true);
				pb.directory(ionicDir);
				Process p = pb.start();
				Engine.execute(() -> {
					try {
						File staging = new File(nodeModules, ".staging");
						while (running[0] && !staging.exists()) {
							appendOutput("Resolving dependencies … (" + Math.round(System.currentTimeMillis() - start) / 1000 + " sec)");
							Thread.sleep(1000);
						}
						while (running[0] && staging.exists()) {
							appendOutput("Collecting node_modules: " + FileUtils.byteCountToDisplaySize(FileUtils.sizeOfAsBigInteger(nodeModules)) + " (" + Math.round(System.currentTimeMillis() - start) / 1000 + " sec)");
							Engine.logStudio.info("Installing, node_module size is now : " + FileUtils.byteCountToDisplaySize(FileUtils.sizeOfAsBigInteger(nodeModules)));
							Thread.sleep(1000);
						} 
					} catch (Exception e) {
						appendOutput("Something wrong during the install: " + e);
					}
				});
				//processes.add(p);
				BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
				String line;
				while ((line = br.readLine()) != null) {
					line = pRemoveEchap.matcher(line).replaceAll("");
					if (StringUtils.isNotBlank(line)) {
						Engine.logStudio.info(line);
						appendOutput(line);
					}
				}
				Engine.logStudio.info(line);
				appendOutput("\\o/");
			} catch (Exception e) {
				appendOutput(":( " + e);
			}
			running[0] = false;
		}
		
		mb.setNeedPkgUpdate(false);
		
		Object mutex = new Object();
		mb.setBuildMutex(mutex);
		
		try {
			File displayObjectsMobile = new File(project.getDirPath() + "/DisplayObjects/mobile");
			displayObjectsMobile.mkdirs();				
			
			appendOutput("removing previous build directory");
			for (File f: displayObjectsMobile.listFiles()) {
				if (!f.getName().equals("assets")) {
					com.twinsoft.convertigo.engine.util.FileUtils.deleteQuietly(f);
				}
			}
			appendOutput("previous build directory removed");
			project.getMobileApplication().getApplicationComponent().checkFolder();
			
			ProcessBuilder pb = ProcessUtils.getNpmProcessBuilder("", "npm", "run", buildMode.command(), "--nobrowser");
			if (!MobileBuilderBuildMode.production.equals(buildMode)) {
				List<String> cmd = pb.command();
				cmd.add("--port");
				cmd.add("" + NetworkUtils.nextAvailable(8100));
				cmd.add("--livereload-port");
				cmd.add("" + NetworkUtils.nextAvailable(35729));
				cmd.add("--dev-logger-port");
				cmd.add("" + NetworkUtils.nextAvailable(53703));
			}
			pb.redirectErrorStream(true);
			pb.directory(ionicDir);
			Process p = pb.start();
//			processes.add(p);
			BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line;
			
			while ((line = br.readLine()) != null) {
				line = pRemoveEchap.matcher(line).replaceAll("");
				if (StringUtils.isNotBlank(line)) {
					Engine.logStudio.info(line);
					appendOutput(line);
					if (line.contains("build finished")) {
						synchronized (mutex) {
							mutex.notify();
						}
					}
					Matcher m = pIsServerRunning.matcher(line);
					if (m.matches()) {
						JSONObject envJSON = new JSONObject();
						envJSON.put("remoteBase", EnginePropertiesManager.getProperty(PropertyName.APPLICATION_SERVER_CONVERTIGO_URL) + "/projects/" + project.getName() + "/_private");
						FileUtils.write(new File(displayObjectsMobile, "env.json"), envJSON.toString(4), "UTF-8");
//						baseUrl = m.group(1);
//						doLoad();
					}
				}
			}
		} finally {
			
		}
		Engine.stop();
	}

}
