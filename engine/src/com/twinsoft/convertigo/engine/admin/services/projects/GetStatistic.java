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

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.XmlService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.util.ProjectUtils;

@ServiceDefinition(
		name = "GetStatistic",
		roles = { Role.TEST_PLATFORM, Role.PROJECTS_CONFIG, Role.PROJECTS_VIEW },
		parameters = {},
		returnValue = "return the statistic for a specific project"
	)
public class GetStatistic extends XmlService{

	protected void getServiceResult(HttpServletRequest request, Document document) throws Exception {
		String projectName = request.getParameter("projectName");
		
		Element root = document.getDocumentElement();
        Element projectStatistic = document.createElement("statistics");
        projectStatistic.setAttribute("project", projectName);
        Project project = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(projectName);
        
        Map<String,String> mapStatsProject = ProjectUtils.getStatByProject(project);
        
        if(mapStatsProject != null && mapStatsProject.size()>0) {
            for(String key : mapStatsProject.keySet()) {
            	Element obj = document.createElement(key.replaceAll(" ", "_"));
            	obj.setTextContent(mapStatsProject.get(key));
            	projectStatistic.appendChild(obj);
            }
        }

        root.appendChild(projectStatistic);
	}
}
