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

package com.twinsoft.convertigo.engine;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.fileupload.ProgressListener;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.twinsoft.convertigo.beans.core.MobileApplication;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.engine.mobile.MobileBuilder;
import com.twinsoft.convertigo.engine.util.CarUtils;
import com.twinsoft.convertigo.engine.util.HttpUtils;
import com.twinsoft.convertigo.engine.util.ProcessUtils;

public class CLI {
	public static final CLI instance = new CLI();
	
	private static Pattern pRemoveEchap = Pattern.compile("\\x1b\\[\\d+m");
	
	private CLI() {
	}
	
	private synchronized void checkInit() throws EngineException {
		if (Engine.bCliMode) {
			return;
		}
		Engine.bCliMode = true;
		
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
		Engine.logConvertigo = Logger.getLogger("cems.Studio");
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

		Engine.theApp = new Engine();
		Engine.theApp.referencedProjectManager = new ReferencedProjectManager();
		Engine.theApp.databaseObjectsManager = new DatabaseObjectsManager();
		Engine.theApp.databaseObjectsManager.init();
		
		Engine.theApp.httpClient4 = HttpUtils.makeHttpClient(true);
		
		Engine.isStarted = true;
	}
	
	public Project loadProject(File projectDir, String version, String mobileApplicationEndpoint) throws EngineException {
		File projectFile = new File(projectDir, "c8oProject.yaml");
		if (!projectFile.exists()) {
			throw new EngineException("No Convertigo project here: " + projectDir);
		}
		
		checkInit();
		
		Project project;
		Engine.PROJECTS_PATH = projectFile.getParentFile().getParent();
		try {
			project = Engine.theApp.databaseObjectsManager.importProject(projectFile);
		} catch (Exception e) {
			Engine.logConvertigo.warn("Failed to import the project from '" + projectFile + "' (" + e.getMessage() + ") trying again...");
			project = Engine.theApp.databaseObjectsManager.importProject(projectFile);
		}
		
		if (version != null) {
			project.setVersion(version);
		}
		
		if (mobileApplicationEndpoint != null) {
			MobileApplication ma = project.getMobileApplication();
			if (ma != null) {
				ma.setEndpoint(mobileApplicationEndpoint);
			}
		}
		return project;
	}
	
	public void export(Project project) throws EngineException {
		try {
			Engine.theApp.databaseObjectsManager.exportProject(project);
		} catch (Exception e) {
			Engine.logConvertigo.warn("Failed to export the project from '" + project.getDirFile() + "' (" + e.getMessage() + ") trying again...");
			Engine.theApp.databaseObjectsManager.exportProject(project);
		}
	}
	
	public File exportToCar(Project project, File dest) throws Exception {
		dest.mkdirs();
		return CarUtils.makeArchive(dest.getAbsolutePath(), project);
	}
	
	public void generateMobileBuilder(Project project) throws Exception {
		MobileBuilder.initBuilder(project, true);
		MobileBuilder.releaseBuilder(project, true);
	}

	public void compileMobileBuilder(Project project, String mode) {
		File ionicDir = new File(project.getDirPath() + "/_private/ionic");
		if (!ionicDir.exists()) {
			Engine.logConvertigo.warn("Failed to perform NodeJS build, no folder: " + ionicDir);
			return;
		}
		String nodeVersion = "v8.9.1";
		try {
			File nodeDir = ProcessUtils.getNodeDir(nodeVersion, new ProgressListener() {
				
				@Override
				public void update(long pBytesRead, long pContentLength, int pItems) {
					Engine.logConvertigo.info("download NodeJS " + nodeVersion + ": " + Math.round(100f * pBytesRead / pContentLength) + "% [" + pBytesRead + "/" + pContentLength + "]");
				}
			});
			ProcessBuilder pb = ProcessUtils.getNpmProcessBuilder(nodeDir.getAbsolutePath(), "npm", "install", ionicDir.toString(), "--no-shrinkwrap", "--no-package-lock");
			pb.redirectErrorStream(true);
			pb.directory(ionicDir);
			Process p = pb.start();
			BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line;
			while ((line = br.readLine()) != null) {
				line = pRemoveEchap.matcher(line).replaceAll("");
				if (StringUtils.isNotBlank(line)) {
					Engine.logConvertigo.info(line);
				}
			}
			Engine.logConvertigo.info(line);
			int code = p.waitFor();
			Engine.logConvertigo.info("npm install finished with exit: " + code);
			
			if ("debug".equals(mode)) {
				pb = ProcessUtils.getNpmProcessBuilder(nodeDir.getAbsolutePath(), "npm", "run", "build", "--nobrowser");
			} else {
				pb = ProcessUtils.getNpmProcessBuilder(nodeDir.getAbsolutePath(), "npm", "run", "build", "--aot", "--minifyjs", "--minifycss", "--release", "--nobrowser");
//				pb = ProcessUtils.getNpmProcessBuilder(nodeDir.getAbsolutePath(), "npm", "run", MobileBuilderBuildMode.production.command(), "--nobrowser");
			}
			pb.redirectErrorStream(true);
			pb.directory(ionicDir);
			p = pb.start();
			br = new BufferedReader(new InputStreamReader(p.getInputStream()));
			while ((line = br.readLine()) != null) {
				line = pRemoveEchap.matcher(line).replaceAll("");
				if (StringUtils.isNotBlank(line)) {
					Engine.logConvertigo.info(line);
				}
			}
			Engine.logConvertigo.info(line);
			code = p.waitFor();
			
			Engine.logConvertigo.info("npm run finished with exit: " + code);
		} catch (Exception e) {
			Engine.logConvertigo.error("buildMobileBuilder failed", e);
		}
		
		
	}
	
	public static void main(String[] args) throws Exception {
		Options opts = new Options()
			.addOption(Option.builder("p").longOpt("project").optionalArg(false).argName("dir").hasArg().desc("[dir] set the directory to load as project (default current folder)").build())
			.addOption(Option.builder("g").longOpt("generate").desc("generate mobilebuilder code inside _private").build())
			.addOption(Option.builder("b").longOpt("build").optionalArg(true).argName("mode").hasArg().desc("build generated mobilebuilder code with NPM into DisplayObject/mobile: [mode] can be production (default) or debug").build())
			.addOption(Option.builder("c").longOpt("car").desc("export as [projectName].car file").build())
			.addOption(Option.builder("v").longOpt("version").optionalArg(false).argName("version").hasArg().desc("change the 'version' property of the loaded [project]").build())
			.addOption(Option.builder("l").longOpt("log").optionalArg(true).argName("level").hasArg().desc("optional [level] (default debug): error, info, warn, debug, trace").build())
			.addOption(new Option("h", "help", false, "show this help"));
		
		CommandLine cmd = new DefaultParser().parse(opts, args, true);
		if (cmd.getOptions().length == 0 || cmd.hasOption("help")) {
			HelpFormatter help = new HelpFormatter();
			help.printHelp("cli", opts);
			return;
		}
		
		try {
			Level level = Level.OFF;
			if (cmd.hasOption("log")) {
				level = Level.toLevel(cmd.getOptionValue("log", "debug"));
			}
			Logger.getRootLogger().setLevel(level);
			Logger.getLogger("org.apache.http").setLevel(Level.WARN);
			
			File projectDir = new File(cmd.hasOption("project") ? cmd.getOptionValue("project") : ".").getCanonicalFile();
			
			CLI cli = new CLI();
			
			String version = cmd.getOptionValue("version", null);
			String mobileApplicationEndpoint = cmd.getOptionValue("mobileApplicationEndpoint", null);
			Project project = cli.loadProject(projectDir, version, mobileApplicationEndpoint);
			
			if (cmd.hasOption("generate") || cmd.hasOption("build")) {
				cli.generateMobileBuilder(project);
			}
			
			if (cmd.hasOption("build")) {
				cli.compileMobileBuilder(project, cmd.getOptionValue("build", null));
			}
			
			if (cmd.hasOption("car")) {
				cli.export(project);
				File out = new File(projectDir, "build");
				System.out.println("Building  : " + projectDir);
				File file = cli.exportToCar(project, out);
				System.out.println("Builded to: " + file);	
			}
			System.out.println("Operations terminated!");
		} finally {
		}
	}

}
