/*
 * Copyright (c) 2001-2025 Convertigo SA.
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

package com.twinsoft.convertigo.engine.admin.services.projects;

import java.io.File;
import java.text.DateFormat;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.codehaus.jettison.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.beans.core.Reference;
import com.twinsoft.convertigo.beans.references.ProjectSchemaReference;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.admin.services.XmlService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;

@ServiceDefinition(
		name = "GetProjects",
		roles = {
			Role.TEST_PLATFORM,
			Role.PROJECTS_CONFIG, Role.PROJECTS_VIEW,
			Role.CERTIFICATE_CONFIG, Role.CERTIFICATE_VIEW
		},
		parameters = {},
		returnValue = "the projects list"
		)
public class List extends XmlService{

	protected void getServiceResult(HttpServletRequest request, Document document) throws Exception {
		Element root = document.getDocumentElement();

		Element projectsListElement = document.createElement("projects");
		root.appendChild(projectsListElement);
		boolean isStudio = Engine.isStudioMode();
		for (String projectName : Engine.theApp.databaseObjectsManager.getAllProjectNamesList()) {
			try {
				if (isStudio && projectName.startsWith("mobilebuilder_tpl_")) {
					continue;
				}
				Project project = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(projectName);
				if (project == null) {
					continue;
				}
				String deployDate = "n/a";
				File file = new File(Engine.projectDir(projectName) + ".car");
				if (file.exists())
					deployDate = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, request.getLocale()).format(new Date(file.lastModified()));

				String comment = project.getComment();
				try {
					var json = new JSONObject(comment);
					var locale = request.getLocale().getLanguage();
					if (json.has(locale)) {
						json = json.getJSONObject(locale);
					} else {
						json = json.getJSONObject(json.keys().next().toString());
					}
					comment = project.getName();
					if (json.has("displayName")) {
						comment = json.getString("displayName");
					}
					if (json.has("comment")) {
						comment += ": " + json.getString("comment");
					}
				} catch (Exception e) {
				}
				comment = comment.replaceAll("<.*?>", "");
				if (comment.length() > 100) comment = comment.substring(0, 100) + "...";

				String version = project.getVersion();

				DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, request.getLocale());
				String exported = project.getInfoForProperty("exported", df, request.getLocale());

				Element projectElement = document.createElement("project");
				projectElement.setAttribute("name", projectName);
				projectElement.setAttribute("comment", comment);
				projectElement.setAttribute("version", version);
				projectElement.setAttribute("exported", exported);
				projectElement.setAttribute("exportedTs", "" + project.getExportTime());
				projectElement.setAttribute("deployDate", deployDate);
				projectElement.setAttribute("deployDateTs", "" + file.lastModified());

				if (Engine.theApp.databaseObjectsManager.symbolsProjectCheckUndefined(projectName)) {
					projectElement.setAttribute("undefined_symbols", "true");
				}

				for (Reference ref: project.getReferenceList()) {
					if (ref instanceof ProjectSchemaReference) {
						ProjectSchemaReference prjRef = (ProjectSchemaReference) ref;
						if (prjRef.getParser().isValid() && Engine.theApp.databaseObjectsManager.getOriginalProjectByName(prjRef.getParser().getProjectName(), true) == null) {
							projectElement.setAttribute("missingDependencies", "true");
							break;
						}
					}
				}

				projectsListElement.appendChild(projectElement);
			}
			catch (EngineException e) {
				String message="Unable to get project information ('" + projectName + "')";
				Engine.logAdmin.error(message, e);
			}
		}	
	}
}
