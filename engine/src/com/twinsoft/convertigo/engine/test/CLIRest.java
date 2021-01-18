/*
 * Copyright (c) 2001-2019 Convertigo SA.
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

package com.twinsoft.convertigo.engine.test;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.codehaus.jettison.json.JSONObject;
import org.w3c.dom.Document;

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.Reference;
import com.twinsoft.convertigo.beans.references.ProjectSchemaReference;
import com.twinsoft.convertigo.engine.DatabaseObjectsManager;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.FilePropertyManager;
import com.twinsoft.convertigo.engine.SchemaManager;
import com.twinsoft.convertigo.engine.enums.JsonOutput;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class CLIRest {
	public static final CLIRest instance = new CLIRest();
	
	List<String> list = new ArrayList<String>();

	private CLIRest() {	
	}
	
	private synchronized void init(String userWskPath, String projectsPath) throws EngineException {
//		if (Engine.bCliMode) {
//			return;
//		}
//		Engine.bCliMode = true;
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

		Engine.theApp = new Engine();
		Engine.theApp.filePropertyManager = new FilePropertyManager();
		Engine.theApp.filePropertyManager.init();
		Engine.theApp.databaseObjectsManager = new DatabaseObjectsManager();
		Engine.theApp.databaseObjectsManager.init();
		Engine.theApp.schemaManager = new SchemaManager();
		Engine.theApp.schemaManager.init();
		
		Engine.USER_WORKSPACE_PATH = userWskPath;
		Engine.PROJECTS_PATH = projectsPath;
	}
	
	public Project loadProject(File projectDir) throws EngineException {
		return loadProject(projectDir, null);
	}
	
	public Project loadProject(File projectDir, String version) throws EngineException {
		File projectFile = new File(projectDir, "c8oProject.yaml");
		if (!projectFile.exists()) {
			projectFile = new File(projectDir, projectDir.getName() + ".xml");
		}
		if (!projectFile.exists()) {
			throw new EngineException("No Convertigo project here: " + projectDir);
		}
		
		Project project;
		try {
			project = Engine.theApp.databaseObjectsManager.importProject(projectFile);
		} catch (Exception e) {
			e.printStackTrace();
			Engine.logConvertigo.warn("Failed to import the project from '" + projectFile + "' (" + e.getMessage() + ") trying again...");
			project = Engine.theApp.databaseObjectsManager.importProject(projectFile);
		}
		if (project != null) {
			String projectName = project.getName();
			synchronized (list) {
				if (!list.contains(projectName)) {
					list.add(projectName);
					System.out.println("Project "+projectName+" loaded");
				}
				for (Reference ref: project.getReferenceList()) {
					if (ref instanceof ProjectSchemaReference) {
						ProjectSchemaReference psr = (ProjectSchemaReference)ref;
						String pn = psr.getProjectName();
						if (!list.contains(pn)) {
							File f = new File(projectDir, "../"+pn);
							if (f.exists()) {
								loadProject(f, null);
							}
						}
					}
				}
			}
		}
		return project;
	}
	
	private JSONObject testXmlRestCompliancy(File file, boolean debug) {
		try {
			Document xml = XMLUtils.parseDOM(file);
			Document doc = Engine.theApp.schemaManager.makeXmlRestCompliant(xml, debug);
			JsonOutput.JsonRoot jsonRoot = JsonOutput.JsonRoot.docAttrAndChildNodes;
			String content = XMLUtils.XmlToJson(doc.getDocumentElement(), true, true, jsonRoot);
			return new JSONObject(content);
		} catch (Exception e) {
			e.printStackTrace();
			return new JSONObject();
		}
	}
	
	public static void main(String[] args) {
		CLIRest cli = null;
		try {
			Options opts = new Options()
				.addOption(Option.builder("p").longOpt("properties").optionalArg(false).argName("filepath").hasArg().desc("[filepath] set the filepath of properties file to load").build())
				.addOption(Option.builder("l").longOpt("log").optionalArg(true).argName("level").hasArg().desc("optional [level] (default debug): error, info, warn, debug, trace").build())
				.addOption(new Option("h", "help", false, "show this help"));
			
			CommandLine cmd = new DefaultParser().parse(opts, args, true);
			if (cmd.getOptions().length == 0 || cmd.hasOption("help")) {
				HelpFormatter help = new HelpFormatter();
				help.printHelp("cli", opts);
			}
			
			Level level = Level.OFF;
			if (cmd.hasOption("log")) {
				level = Level.toLevel(cmd.getOptionValue("log", "debug"));
			}
			Logger.getRootLogger().setLevel(level);
			
			String filepath = cmd.hasOption("properties") ? cmd.getOptionValue("properties") : "test.properties";
			if (new File(filepath).exists()) {
				Properties props = new Properties();
				props.load(new FileInputStream(filepath));
				
				String userWskPath = props.getProperty("engine.user.workspace.dir");
				String projectsPath = props.getProperty("engine.projects.dir");
				cli = instance;
				cli.init(userWskPath, projectsPath);
				
				String[] projects = props.getProperty("load.projects").split(",");
				
				File projectsDir = new File(projectsPath).getCanonicalFile();
				for (String pname: projects) {
					Project project = cli.loadProject(new File(projectsDir, pname));
					Engine.theApp.schemaManager.getSchemasForProject(project.getName(), com.twinsoft.convertigo.engine.SchemaManager.Option.noCache);
				}
				
				JSONObject jsonObject;
				File testDir = new File(props.getProperty("test.dir")).getCanonicalFile();
				for (String pname: projects) {
					boolean debug = false;
					//if ("PobiFicp".equals(pname)) debug = true;
					for (String xml:  props.getProperty("test."+ pname).split(",")) {
						jsonObject = cli.testXmlRestCompliancy(new File(testDir, xml), debug);
						System.out.println(jsonObject.toString(1));
					}
				}
			}
		} catch (Throwable t) {
			t.printStackTrace();
		} finally {
			if (cli != null)
				cli.list.clear();
		}
	}

}
