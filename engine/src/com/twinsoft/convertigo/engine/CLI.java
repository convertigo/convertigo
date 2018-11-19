/*
 * Copyright (c) 2001-2018 Convertigo SA.
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

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.enums.MobileBuilderBuildMode;
import com.twinsoft.convertigo.engine.mobile.MobileBuilder;
import com.twinsoft.convertigo.engine.util.CarUtils;
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
	
	public CLI() {
		
	}
	
	private void checkInit() throws EngineException {
//		System.setProperty(Engine.JVM_PROPERTY_USER_WORKSPACE, path.getAbsolutePath());
		Engine.bCliMode = true;
		//Engine.initPaths(new File(path, "no").getAbsolutePath());
		EnginePropertiesManager.initProperties();
		Engine.logConvertigo = Logger.getLogger("cems");
		Engine.logEngine = Logger.getLogger("cems.Engine");
		Engine.logAdmin = Logger.getLogger("cems.Admin");
		Engine.logBeans = Logger.getLogger("cems.Beans");
		Engine.logBillers = Logger.getLogger("cems.Billers");
		Engine.logEmulators = Logger.getLogger("cems.Emulators");
		Engine.logContext = Logger.getLogger("cems.Context");
		Engine.logUser = Logger.getLogger("cems.Context.User");
		Engine.logUsageMonitor = Logger.getLogger("cems.UsageMonitor");
		Engine.logStatistics = Logger.getLogger("cems.Statistics");
		Engine.logScheduler = Logger.getLogger("cems.Scheduler");
		Engine.logSiteClipper = Logger.getLogger("cems.SiteClipper");
		Engine.logSecurityFilter = Logger.getLogger("cems.SecurityFilter");
		Engine.logStudio = Logger.getLogger("cems.Studio");
		Engine.logAudit = Logger.getLogger("cems.Context.Audit");
		
		// Managers
		Engine.logContextManager = Logger.getLogger("cems.ContextManager");
		Engine.logCacheManager = Logger.getLogger("cems.CacheManager");
		Engine.logTracePlayerManager = Logger.getLogger("cems.TracePlayerManager");
		Engine.logJobManager = Logger.getLogger("cems.JobManager");
		Engine.logCertificateManager = Logger.getLogger("cems.CertificateManager");
		Engine.logDatabaseObjectManager = Logger.getLogger("cems.DatabaseObjectManager");
		Engine.logProxyManager = Logger.getLogger("cems.ProxyManager");
		Engine.logDevices = Logger.getLogger("cems.Devices");
		Engine.logCouchDbManager = Logger.getLogger("cems.CouchDbManager");
		Engine.logSecurityTokenManager = Logger.getLogger("cems.SecurityTokenManager");
	}
	
	public void buildMB(Project project) throws Exception {
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
	}
	
	public Project loadProject(File projectDir) throws EngineException {
		checkInit();
		
		File projectFile = new File(projectDir, "c8oProject.yaml");
		if (!projectFile.exists()) {
			projectFile = new File(projectDir, projectDir.getName() + ".xml");
		}
		if (!projectFile.exists()) {
			throw new EngineException("No Convertigo project here: " + projectDir);
		}
		//Engine.theApp.databaseObjectsManager
		DatabaseObjectsManager dbom = new DatabaseObjectsManager();
		Engine.theApp = new Engine();
		Engine.theApp.databaseObjectsManager = dbom;
		dbom.init();
		return dbom.importProject(projectFile);
	}
	
	public File exportToCar(Project project, File dest) throws Exception {
		dest.mkdirs();
		return CarUtils.makeArchive(dest.getAbsolutePath(), project, project.getName());
	}
	
	public static void main(String[] args) throws Exception {
		Options opts = new Options()
			.addOption(Option.builder("p").longOpt("project").optionalArg(false).argName("dir").hasArg().desc("[dir] set the directory to load as project (default current folder)").build())
			.addOption(new Option("c", "car", false, "export as [projectName].car file"))
			.addOption(Option.builder("l").longOpt("log").optionalArg(true).argName("level").hasArg().desc("optional [level] (default debug): error, info, warn, debug, trace").build())
			.addOption(new Option("h", "help", false, "show this help"));
		
		CommandLine cmd = new DefaultParser().parse(opts, args, true);
		if (cmd.getOptions().length == 0 || cmd.hasOption("help")) {
			HelpFormatter help = new HelpFormatter();
			help.printHelp("cli", opts);
		}
		
//		File wp = null;
		try {
			Level level = Level.OFF;
			if (cmd.hasOption("log")) {
				level = Level.toLevel(cmd.getOptionValue("log", "debug"));
			}
			Logger.getRootLogger().setLevel(level);
			
			File projectDir = new File(cmd.hasOption("project") ? cmd.getOptionValue("project") : ".").getCanonicalFile();
			
			CLI cli = new CLI();
			
			if (cmd.hasOption("car")) {
//				wp = File.createTempFile("Convertigo", "cli");
//				wp.delete();
//				wp.mkdirs();
				File out = new File(projectDir, "build");
				Project project = cli.loadProject(projectDir);
				System.out.println("Building  : " + projectDir);
				File file = cli.exportToCar(project, out);
				System.out.println("Builded to: " + file);	
			}
			
		} finally {
//			if (wp != null) {
//				FileUtils.deleteQuietly(wp);
//			}
		}
	}

}
