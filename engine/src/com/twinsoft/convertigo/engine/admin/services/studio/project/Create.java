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

package com.twinsoft.convertigo.engine.admin.services.studio.project;

import java.io.File;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.http.HttpServletRequest;

import org.apache.commons.io.FileUtils;
import org.codehaus.jettison.json.JSONObject;

import com.twinsoft.api.Session;
import com.twinsoft.convertigo.beans.connectors.CicsConnector;
import com.twinsoft.convertigo.beans.connectors.HttpConnector;
import com.twinsoft.convertigo.beans.connectors.JavelinConnector;
import com.twinsoft.convertigo.beans.connectors.SapJcoConnector;
import com.twinsoft.convertigo.beans.connectors.SiteClipperConnector;
import com.twinsoft.convertigo.beans.connectors.SqlConnector;
import com.twinsoft.convertigo.beans.core.Connector;
import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.JSonService;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.util.ProjectUrlParser;
import com.twinsoft.convertigo.engine.util.ProjectUtils;
import com.twinsoft.convertigo.engine.util.StringUtils;

/**
 * Creates a project from a template, as the new project wizard of the Eclipse Studio: the template
 * project is imported under the new name, with no version, its default connector renamed and set up
 * with the settings given for its type (HTTP, SQL, SAP or Site Clipper), then saved.
 * <ul>
 * <li>name: the name of the new project</li>
 * <li>template: the URL of the template, as {@link ProjectUrlParser} reads it
 * (templateName=https://.../archive/version.zip)</li>
 * <li>settings: a JSON object with connectorName and the settings of the connector</li>
 * </ul>
 */
@ServiceDefinition(name = "Create", roles = { Role.WEB_ADMIN, Role.PROJECTS_CONFIG }, parameters = {}, returnValue = "")
public class Create extends JSonService {

	@Override
	protected void getServiceResult(HttpServletRequest request, JSONObject response) throws Exception {
		var name = request.getParameter("name");
		if (name == null || name.isBlank()) {
			throw new ServiceException("missing name parameter");
		}
		name = name.trim();
		if (!name.equals(StringUtils.normalize(name))) {
			throw new ServiceException("The project name \"" + name + "\" can only use letters, digits and underscores.");
		}
		var template = request.getParameter("template");
		if (template == null || template.isBlank()) {
			throw new ServiceException("missing template parameter");
		}
		var settingsParameter = request.getParameter("settings");
		var settings = new JSONObject(settingsParameter == null || settingsParameter.isBlank() ? "{}" : settingsParameter);

		if (Engine.theApp.databaseObjectsManager.existsProject(name)) {
			throw new ServiceException("A project named \"" + name + "\" already exists.");
		}
		var parser = new ProjectUrlParser(template);
		if (!parser.isValid()) {
			throw new ServiceException("The template URL is not valid: " + template);
		}
		var templateName = parser.getProjectName();
		parser.setProjectName(name);
		var project = Engine.theApp.referencedProjectManager.importProject(parser, true, true);
		if (project == null) {
			throw new ServiceException("The template cannot be loaded from " + template);
		}

		try {
			project.setVersion("");
			var connector = project.getDefaultConnector();
			var oldConnectorName = connector == null ? "" : connector.getName();
			var connectorName = settings.optString("connectorName", "").trim();
			if (connector != null && !connectorName.isEmpty()) {
				connector.setName(connectorName);
			}
			configureConnector(connector, settings);
			Engine.theApp.databaseObjectsManager.exportProject(project);
			renameProjectFiles(project, templateName, oldConnectorName, connector == null ? "" : connector.getName());
		} catch (Exception e) {
			try {
				Engine.theApp.databaseObjectsManager.deleteProject(name, false, false);
			} catch (Exception ex) {
				Engine.logAdmin.debug("(studio.project.Create) cannot delete the project " + name, ex);
			}
			throw new ServiceException("Unable to create the project " + name + " from its template: " + e.getMessage(), e);
		}

		response.put("done", true);
		response.put("name", project.getName());
	}

	private static void configureConnector(Connector connector, JSONObject settings) throws Exception {
		if (connector instanceof HttpConnector http) {
			if (settings.has("server")) {
				http.setServer(settings.getString("server"));
			}
			if (settings.has("port")) {
				http.setPort(settings.getInt("port"));
			}
			if (settings.has("https")) {
				http.setHttps(settings.getBoolean("https"));
			}
		} else if (connector instanceof SqlConnector sql) {
			if (settings.has("jdbcDriver")) {
				sql.setJdbcDriverClassName(settings.getString("jdbcDriver"));
			}
			if (settings.has("jdbcUrl")) {
				sql.setJdbcURL(settings.getString("jdbcUrl"));
			}
			if (settings.has("user")) {
				sql.setJdbcUserName(settings.getString("user"));
			}
			if (settings.has("password")) {
				sql.setJdbcUserPassword(settings.getString("password"));
			}
		} else if (connector instanceof SapJcoConnector sap) {
			if (settings.has("asHost")) {
				sap.setAsHost(settings.getString("asHost"));
			}
			if (settings.has("systemNumber")) {
				sap.setSystemNumber(settings.getString("systemNumber"));
			}
			if (settings.has("client")) {
				sap.setClient(settings.getString("client"));
			}
			if (settings.has("user")) {
				sap.setUser(settings.getString("user"));
			}
			if (settings.has("password")) {
				sap.setPassword(settings.getString("password"));
			}
			if (settings.has("language")) {
				sap.setLanguage(settings.getString("language"));
			}
		} else if (connector instanceof JavelinConnector javelin) {
			// a screen connector: its emulator technology and its service code, "parameter,DIR|host:port" or
			// "parameter,TCP|host:port" for DKU, as the new project wizards of the Eclipse Studio build them
			var emulator = settings.optString("emulator", "");
			var technology = switch (emulator) {
			case "IBM3270" -> Session.SNA;
			case "IBM5250" -> Session.AS400;
			case "BullDKU7107" -> Session.DKU;
			case "UnixVT220" -> Session.VT;
			default -> "";
			};
			if (!technology.isEmpty()) {
				var host = settings.optString("host", "").trim();
				var port = settings.has("port") ? String.valueOf(settings.getInt("port")) : "";
				var address = port.isEmpty() || host.isEmpty() ? host : host + ":" + port;
				var type = "BullDKU7107".equals(emulator) ? "TCP" : "DIR";
				javelin.setServiceCode(settings.optString("connectionParameter", "") + "," + type + "|" + address);
				javelin.setEmulatorTechnology(technology);
				if ("IBM3270".equals(emulator)) {
					javelin.setIbmTerminalType("IBM-3279");
				} else if ("IBM5250".equals(emulator)) {
					javelin.setIbmTerminalType("IBM-3179");
				}
				var screenClass = javelin.getDefaultScreenClass();
				if (screenClass != null && !screenClass.getLocalCriterias().isEmpty()) {
					screenClass.getLocalCriterias().get(0).setName(emulator);
				}
			}
		} else if (connector instanceof CicsConnector cics) {
			if (settings.has("ctgName")) {
				cics.setMainframeName(settings.getString("ctgName"));
			}
			if (settings.has("ctgServer")) {
				cics.setServer(settings.getString("ctgServer"));
			}
			if (settings.has("ctgPort")) {
				cics.setPort(settings.getInt("ctgPort"));
			}
		} else if (connector instanceof SiteClipperConnector siteClipper) {
			if (settings.has("trustAllServerCertificates")) {
				siteClipper.setTrustAllServerCertificates(settings.getBoolean("trustAllServerCertificates"));
			}
			if (settings.has("targetUrl")) {
				siteClipper.getDefaultTransaction().setTargetURL(settings.getString("targetUrl"));
			}
		}
	}

	/**
	 * The Eclipse project file and the internal schemas of the transactions keep the names of the template:
	 * they take the names of the new project and of its connector.
	 */
	private static void renameProjectFiles(Project project, String templateName, String oldConnectorName, String connectorName) {
		try {
			var eclipseProject = new File(project.getDirPath(), ".project");
			if (eclipseProject.exists()) {
				var txt = FileUtils.readFileToString(eclipseProject, StandardCharsets.UTF_8);
				txt = txt.replaceFirst("(<name>)(.*?)(</name>)", "$1" + project.getName() + "$3");
				FileUtils.writeStringToFile(eclipseProject, txt, StandardCharsets.UTF_8);
			}
			var xsdInternalDir = new File(project.getDirPath() + "/" + Project.XSD_FOLDER_NAME + "/" + Project.XSD_INTERNAL_FOLDER_NAME).getCanonicalFile();
			if (!xsdInternalDir.exists()) {
				return;
			}
			var connectorRenamed = !oldConnectorName.isEmpty() && !oldConnectorName.equals(connectorName);
			if (connectorRenamed) {
				var srcDir = new File(xsdInternalDir, oldConnectorName);
				if (srcDir.exists()) {
					if (oldConnectorName.equalsIgnoreCase(connectorName)) {
						var tmpDir = new File(xsdInternalDir, "tmp" + oldConnectorName).getCanonicalFile();
						FileUtils.moveDirectory(srcDir, tmpDir);
						srcDir = tmpDir;
					}
					FileUtils.moveDirectory(srcDir, new File(xsdInternalDir, connectorName));
				}
			}
			var connectorDirs = xsdInternalDir.listFiles(File::isDirectory);
			if (connectorDirs == null) {
				return;
			}
			for (var connectorDir : connectorDirs) {
				var xsdFiles = connectorDir.listFiles();
				if (xsdFiles == null) {
					continue;
				}
				for (var xsdFile : xsdFiles) {
					var xsdPath = xsdFile.getCanonicalPath();
					ProjectUtils.xsdRenameProject(xsdPath, templateName, project.getName());
					if (connectorRenamed && connectorDir.getName().equals(connectorName)) {
						ProjectUtils.xsdRenameConnector(xsdPath, oldConnectorName, connectorName);
					}
				}
			}
		} catch (Exception e) {
			Engine.logAdmin.error("(studio.project.Create) An error occured while updating the transaction schemas of " + project.getName(), e);
		}
	}
}
