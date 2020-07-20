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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.fileupload.ProgressListener;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.twinsoft.convertigo.beans.core.MobileApplication;
import com.twinsoft.convertigo.beans.core.MobilePlatform;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.admin.services.mobiles.GetBuildStatus;
import com.twinsoft.convertigo.engine.admin.services.mobiles.GetPackage;
import com.twinsoft.convertigo.engine.admin.services.mobiles.LaunchBuild;
import com.twinsoft.convertigo.engine.admin.services.mobiles.MobileResourceHelper;
import com.twinsoft.convertigo.engine.enums.ArchiveExportOption;
import com.twinsoft.convertigo.engine.enums.MobileBuilderBuildMode;
import com.twinsoft.convertigo.engine.mobile.MobileBuilder;
import com.twinsoft.convertigo.engine.util.CarUtils;
import com.twinsoft.convertigo.engine.util.HttpUtils;
import com.twinsoft.convertigo.engine.util.ProcessUtils;
import com.twinsoft.convertigo.engine.util.RemoteAdmin;

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
		
		Engine.startStopDate = System.currentTimeMillis();
		
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
		Engine.theApp.eventManager = new EventManager();
		Engine.theApp.eventManager.init();
		Engine.theApp.referencedProjectManager = new ReferencedProjectManager();
		Engine.theApp.databaseObjectsManager = new DatabaseObjectsManager();
		Engine.theApp.databaseObjectsManager.init();
		Engine.theApp.proxyManager = new ProxyManager();
		Engine.theApp.proxyManager.init();
		
		Engine.theApp.httpClient4 = HttpUtils.makeHttpClient(true);
		Engine.theApp.httpClient = HttpUtils.makeHttpClient3(true);
		
		Engine.isStarted = true;
	}
	
	public Project loadProject(File projectDir, String version, String mobileApplicationEndpoint, String gitContainer) throws EngineException {
		File projectFile = new File(projectDir, "c8oProject.yaml");
		if (!projectFile.exists()) {
			throw new EngineException("No Convertigo project here: " + projectDir);
		}
		
		checkInit();
		
		Project project;
		Engine.PROJECTS_PATH = projectFile.getParentFile().getParent();
		
		File testFile;
		boolean ok = false;
		if (gitContainer != null) {
			testFile = new File(gitContainer, "convertigoWriteTest");
			if (ok = testFile.mkdirs()) {
				ok = testFile.delete();
				if (ok) {
					EnginePropertiesManager.setProperty(PropertyName.GIT_CONTAINER, testFile.getParent());
					Engine.logEngine.info("Use GitContainer to: " + testFile.getParent());
				}
			}
			if (!ok) {
				Engine.logEngine.info("Cannot write to: " + testFile.getParent());
			}
		}
		
		if (!ok) {
			testFile = new File(EnginePropertiesManager.getProperty(PropertyName.GIT_CONTAINER), "convertigoWriteTest");
			if (ok = testFile.mkdirs()) {
				ok = testFile.delete();
				if (ok) {
					Engine.logEngine.info("Use GitContainer to: " + testFile.getParent());
				}
			}
			if (!ok) {
				Engine.logEngine.info("Cannot write to: " + testFile.getParent());
			}
		}
		
		if (!ok) {
			testFile = new File(Engine.PROJECTS_PATH, "convertigoWriteTest");
			if (ok = testFile.mkdirs()) {
				ok = testFile.delete();
			}
			if (ok) {
				EnginePropertiesManager.setProperty(PropertyName.GIT_CONTAINER, testFile.getParent());
				Engine.logEngine.info("Use GitContainer to: " + testFile.getParent());
			}
			if (!ok) {
				Engine.logEngine.info("Cannot write to: " + testFile.getParent());
			}
		}
		
		if (!ok) {
			File tmpFile;
			try {
				tmpFile = File.createTempFile("convertigoWriteTest", "Tmp");
				tmpFile.delete();
				testFile = new File(tmpFile, "convertigoWriteTest");
				if (ok = testFile.mkdirs()) {
					ok = testFile.delete();
				}
				if (ok) {
					EnginePropertiesManager.setProperty(PropertyName.GIT_CONTAINER, testFile.getParent());
					Engine.logEngine.info("Use GitContainer to: " + testFile.getParent());
				} else {
					Engine.logEngine.info("Cannot write to: " + testFile.getParent());
				}
			} catch (IOException e) {
			}
		}
		
		if (!ok) {
			File _private = new File(projectDir, "_private/gitContainer");
			testFile = new File(_private, "convertigoWriteTest");
			if (ok = testFile.mkdirs()) {
				ok = testFile.delete();
			}
			if (ok) {
				EnginePropertiesManager.setProperty(PropertyName.GIT_CONTAINER, testFile.getParent());
				Engine.logEngine.info("Use GitContainer to: " + testFile.getParent());
			} else {
				Engine.logEngine.info("Cannot write to: " + testFile.getParent());
			}
		}
		
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
	
	public File exportToCar(Project project, File dest, boolean includeTestCases, boolean includeStubs,
			boolean includeMobileApp, boolean includeMobileAppAssets, boolean includeMobileDataset,
			boolean includeMobilePlatformsAssets) throws Exception {
		dest.mkdirs();
		Set<ArchiveExportOption> options = new HashSet<>(ArchiveExportOption.all);
		if (!includeTestCases) {
			options.remove(ArchiveExportOption.includeTestCase);
		}
		if (!includeStubs) {
			options.remove(ArchiveExportOption.includeStubs);
		}
		if (!includeMobileApp) {
			options.remove(ArchiveExportOption.includeMobileApp);
		}
		if (!includeMobileAppAssets) {
			options.remove(ArchiveExportOption.includeMobileAppAssets);
		}
		if (!includeMobileDataset) {
			options.remove(ArchiveExportOption.includeMobileDataset);
		}
		if (!includeMobilePlatformsAssets) {
			options.remove(ArchiveExportOption.includeMobilePlatformsAssets);
		}
		return CarUtils.makeArchive(dest.getAbsolutePath(), project);
	}
	
	public void generateMobileBuilder(Project project, String mode) throws Exception {
		MobileBuilder mb = project.getMobileBuilder();
		MobileBuilderBuildMode bm = MobileBuilderBuildMode.production;
		try {
			bm = MobileBuilderBuildMode.valueOf(mode);
		} catch (Exception e) { }
		mb.setAppBuildMode(bm);
		MobileBuilder.initBuilder(project, true);
		MobileBuilder.releaseBuilder(project, true);
	}

	public void compileMobileBuilder(Project project, String mode) throws Exception {
		File ionicDir = new File(project.getDirPath() + "/_private/ionic");
		if (!ionicDir.exists()) {
			Engine.logConvertigo.warn("Failed to perform NodeJS build, no folder: " + ionicDir);
			return;
		}
		boolean b_ngx = project.getMobileApplication().getApplicationComponent() instanceof com.twinsoft.convertigo.beans.ngx.components.ApplicationComponent;
		boolean ngx = false;
		
		String nodeVersion = ProcessUtils.getNodeVersion(project);
		Engine.logConvertigo.info("Requested nodeVersion: " + nodeVersion);
		File nodeDir = ProcessUtils.getNodeDir(nodeVersion, new ProgressListener() {
			
			@Override
			public void update(long pBytesRead, long pContentLength, int pItems) {
				Engine.logConvertigo.info("download NodeJS " + nodeVersion + ": " + Math.round(100f * pBytesRead / pContentLength) + "% [" + pBytesRead + "/" + pContentLength + "]");
			}
		});
		String nodePath = nodeDir.getAbsolutePath();
		
		ProcessBuilder pb;
		
		if (ngx) {
			File yarnFile = new File(ionicDir, "node_modules/.bin/yarn");
			if (!yarnFile.exists()) {
				Engine.logConvertigo.info("Installing Yarn...");
				pb = ProcessUtils.getNpmProcessBuilder(nodePath, "npm", "install", "yarn");
				pb.redirectErrorStream(true);
				pb.directory(ionicDir);
				pb.start().waitFor();
			}
			pb = ProcessUtils.getNpmProcessBuilder(yarnFile.getParent(), "yarn");
		} else {
			pb = ProcessUtils.getNpmProcessBuilder(nodePath, "npm", "install", ionicDir.toString(), "--no-shrinkwrap", "--no-package-lock");	
		}
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
		int code = p.waitFor();
		Engine.logConvertigo.info((ngx ? "yarn" : "npm install") + " finished with exit: " + code);
		
		ngx = b_ngx;
		
		if (ngx) {
			pb = ProcessUtils.getNpmProcessBuilder(nodePath, "npm", "run", "ionic:build:prod", "--nobrowser");
		} else {
			if ("debug".equals(mode)) {
				pb = ProcessUtils.getNpmProcessBuilder(nodePath, "npm", "run", "build", "--nobrowser");
			} else {
				pb = ProcessUtils.getNpmProcessBuilder(nodePath, "npm", "run", "build", "--aot", "--minifyjs", "--minifycss", "--release", "--nobrowser");
//				pb = ProcessUtils.getNpmProcessBuilder(nodeDir.getAbsolutePath(), "npm", "run", MobileBuilderBuildMode.production.command(), "--nobrowser");
			}
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
		code = p.waitFor();
		if (code != 0) {
			throw new EngineException("npm build return a '" + code + "' failure code, see --info logs for details");
		}
		Engine.logConvertigo.info("npm run finished with exit: " + code);
	}
	
	public void deploy(File file, String server, String user, String password, boolean trustAllCertificates, boolean assembleXsl) throws EngineException {
		boolean isHttps = server.startsWith("https://");
		String convertigoServer = server.substring(isHttps ? 8 : 7);
		RemoteAdmin remoteAdmin = new RemoteAdmin(convertigoServer, isHttps, trustAllCertificates);
		Engine.logEngine.info("Trying to connect the user '" + user + "' to the Convertigo remote server: " + server);
		remoteAdmin.login(user, password);
		Engine.logEngine.info("Deployement of '" + file + "' to the Convertigo remote server: " + server);
		remoteAdmin.deployArchive(file, assembleXsl);
		Engine.logEngine.info("File '" + file + "' deployed to the Convertigo remote server: " + server);
	}
	
	public void launchBuild(Project project, List<String> platforms) {
		String buildFolder = "mobile/www";
		MobileApplication mobileApplication = project.getMobileApplication();
		for (MobilePlatform mobilePlatform : mobileApplication.getMobilePlatformList()) {
			String platformName = mobilePlatform.getName();
			try {
				if (platforms.isEmpty() || platforms.contains(platformName)) {
					MobileResourceHelper mobileResourceHelper = new MobileResourceHelper(null, buildFolder, project.getName(), platformName);
					String sResult = LaunchBuild.perform(mobileResourceHelper, null);
					Engine.logEngine.info("build: " + sResult);
				}
			} catch (Exception e) {
				e.printStackTrace();
				Engine.logEngine.error("failed to launch build for " + platformName, e);
			}
		}
	}
	
	public List<File> downloadBuild(Project project, List<String> platforms, File destinationDir) {
		MobileApplication mobileApplication = project.getMobileApplication();
		List<MobilePlatform> platformList = new ArrayList<>(mobileApplication.getMobilePlatformList());
		List<File> files = new ArrayList<File>(platformList.size());
		boolean hasMore = true;
		while (hasMore) {
			hasMore = false;
			for (Iterator<MobilePlatform> it = platformList.iterator(); it.hasNext();) {
				MobilePlatform platform = it.next();
				HttpMethod method = null;
				String platformName = platform.getName();
				
				if (!platforms.isEmpty() && !platforms.contains(platformName)) {
					it.remove();
					continue;
				}
				
				try {
					String sResult = GetBuildStatus.perform(mobileApplication, platformName, null);
					Engine.logEngine.info("build status: " + sResult);

					if (sResult.contains("status\": \"pending\"")) {
						hasMore = true;
					} else {
						it.remove();
					}

					if (sResult.contains("status\": \"complete\"")) {
						method = GetPackage.perform(mobileApplication, platformName, null);
						String filename = mobileApplication.getComputedApplicationName() + "_" + platformName + "." + platform.getPackageType();
//						try {
//							Engine.logEngine.info("ContentDisposition : " + method.getResponseHeader(HeaderName.ContentDisposition.value()));
//							filename = method.getResponseHeader(HeaderName.ContentDisposition.value()).getValue().replaceFirst(".*filename=\"(.*)\".*", "$1");
//						} catch (Exception e) {}
						File file = new File(destinationDir, filename);
						try (FileOutputStream fos = new FileOutputStream(file)) {
							IOUtils.copy(method.getResponseBodyAsStream(), fos);
							files.add(file);
						}
					}
				} catch (Exception e) {
					it.remove();
					Engine.logEngine.error("failed to retrieve " + platformName, e);
				} finally {
					if (method != null) {
						method.releaseConnection();
					}
				}
			}
			if (hasMore) {
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e) {}
			}
		}
		return files;
	}
	
	public static void main(String[] args) throws Exception {
		Options opts = new Options()
			.addOption(Option.builder("p").longOpt("project").optionalArg(false).argName("dir").hasArg().desc("<dir> set the directory to load as project (default current folder).").build())
			.addOption(Option.builder("gc").longOpt("gitContainer").optionalArg(true).argName("path").hasArg().desc("git dependencies can be extrated to the <gitContainer> folder instead of defaults.").build())
			.addOption(Option.builder("g").longOpt("generate").optionalArg(true).argName("mode").hasArg().desc("generate mobilebuilder code into _private/ionic: <mode> can be production (default) or debugplus, debug, fast. If omitted, build mode is used.").build())
			.addOption(Option.builder("b").longOpt("build").optionalArg(true).argName("mode").hasArg().desc("build generated mobilebuilder code with NPM into DisplayObject/mobile: <mode> can be production (default) or debug. If omitted, generate mode is used.").build())
			.addOption(Option.builder("c").longOpt("car").desc("export as <projectName>.car file").build())
			.addOption(Option.builder("nb").longOpt("nativeBuild").optionalArg(true).argName("platforms").hasArg().desc("perform and download a remote cordova build of the application. Launch build and download for all mobile platforms or add the optional <platforms> parameter with list of plaform separated by coma: Android,IOs.").build())
			.addOption(Option.builder("lnb").longOpt("launchNativeBuild").optionalArg(true).argName("platforms").hasArg().desc("perform a remote cordova build of the application. Launch build for all mobile platforms or add the optional <platforms> parameter with list of plaform separated by coma: Android,IOs.").build())
			.addOption(Option.builder("dnb").longOpt("downloadNativeBuild").optionalArg(true).argName("platforms").hasArg().desc("download a remote cordova build of the application. Download from previous launch, all mobile platforms or add the optional <platforms> parameter with list of plaform separated by coma: Android,IOs.").build())
			.addOption(Option.builder("noTC").longOpt("excludeTestCases").desc("when export or deploy, do not include TestCases.").build())
			.addOption(Option.builder("noS").longOpt("excludeStubs").desc("when export or deploy, do not include Stubs.").build())
			.addOption(Option.builder("noMA").longOpt("excludeMobileApp").desc("when export or deploy, do not include built MobileApp.").build())
			.addOption(Option.builder("noMAA").longOpt("excludeMobileAppAssets").desc("when export or deploy, do not include built MobileApp assets.").build())
			.addOption(Option.builder("noDS").longOpt("excludeDataset").desc("when export or deploy, do not include mobile dataset.").build())
			.addOption(Option.builder("noPA").longOpt("excludePlatformAssets").desc("when export or deploy, do not include mobile platform assets.").build())
			.addOption(Option.builder("d").longOpt("deploy").optionalArg(false).argName("server").hasArg().desc("deploy the current project to <server> using user/password credentials.").build())
			.addOption(Option.builder("u").longOpt("user").optionalArg(false).argName("user").hasArg().desc("<user> used by the deploy action, default is 'admin'.").build())
			.addOption(Option.builder("w").longOpt("password").optionalArg(false).argName("password").hasArg().desc("<password> used by the deploy action, default is 'admin'.").build())
			.addOption(Option.builder("trust").longOpt("trustAllCertificates").desc("deploy over an https <server> without checking certificates.").build())
			.addOption(Option.builder("xsl").longOpt("assembleXsl").desc("assemble XSL files on deploy.").build())
			.addOption(Option.builder("v").longOpt("version").optionalArg(false).argName("version").hasArg().desc("change the 'version' property of the loaded <project>.").build())
			.addOption(Option.builder("l").longOpt("log").optionalArg(true).argName("level").hasArg().desc("optional <level> (default debug): error, info, warn, debug, trace.").build())
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
			Logger.getLogger("org").setLevel(Level.WARN);
			Logger.getLogger("httpclient").setLevel(Level.WARN);
			
			File projectDir = new File(cmd.hasOption("project") ? cmd.getOptionValue("project") : ".").getCanonicalFile();
			
			CLI cli = new CLI();
			
			String version = cmd.getOptionValue("version", null);
			String mobileApplicationEndpoint = cmd.getOptionValue("mobileApplicationEndpoint", null);
			Project project = cli.loadProject(projectDir, version, mobileApplicationEndpoint, cmd.getOptionValue("gitContainer"));
			
			String gMode = cmd.getOptionValue("generate", null);
			String bMode = cmd.getOptionValue("build", null);
			if (cmd.hasOption("generate") || cmd.hasOption("build")) {
				if (gMode == null && bMode != null) {
					gMode = bMode;
				}
				cli.generateMobileBuilder(project, gMode);
			}
			
			if (cmd.hasOption("build")) {
				if (bMode == null) {
					bMode = (gMode == null || gMode.equals("production")) ? "production" : "debug";
				}
				cli.compileMobileBuilder(project, bMode);
			}
			
			File file = null;
			if (cmd.hasOption("car") || cmd.hasOption("deploy")) {
				cli.export(project);
				File out = new File(projectDir, "build");
				Logger.getRootLogger().info("Building  : " + projectDir);
				
				file = cli.exportToCar(project, out, !cmd.hasOption("excludeTestCases"),
						!cmd.hasOption("excludeStubs"), !cmd.hasOption("excludeMobileApp"),
						!cmd.hasOption("excludeMobileAppAssets"), !cmd.hasOption("excludeDataset"),
						!cmd.hasOption("excludePlatformAssets"));
				Logger.getRootLogger().info("Built to: " + file);	
			}
			
			if (cmd.hasOption("deploy")) {
				String server = cmd.getOptionValue("deploy");
				String user = cmd.getOptionValue("user", "admin");
				String password = cmd.getOptionValue("password", "admin");
				boolean trustAllCertificates = cmd.hasOption("trust");
				boolean assembleXsl = cmd.hasOption("xsl");
				cli.deploy(file, server, user, password, trustAllCertificates, assembleXsl);
			}
			
			if (cmd.hasOption("launchNativeBuild") || cmd.hasOption("nativeBuild")) {
				String opt = cmd.getOptionValue("nativeBuild");
				if (opt == null) {
					opt = cmd.getOptionValue("launchNativeBuild");
				}
				List<String> platforms = opt == null ? Collections.emptyList() : Arrays.asList(opt.split(","));
				cli.launchBuild(project, platforms);
			}
			
			if (cmd.hasOption("downloadNativeBuild") || cmd.hasOption("nativeBuild")) {
				String opt = cmd.getOptionValue("nativeBuild");
				if (opt == null) {
					opt = cmd.getOptionValue("downloadNativeBuild");
				}
				List<String> platforms = opt == null ? Collections.emptyList() : Arrays.asList(opt.split(","));
				List<File> files = cli.downloadBuild(project, platforms, new File(project.getDirFile(), "build"));
				Logger.getRootLogger().info("Downloaded application: " + files);
			}
			Logger.getRootLogger().info("Operations terminated!");
		} finally {
		}
	}

}
