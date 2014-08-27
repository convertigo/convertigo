/*
 * Copyright (c) 2001-2011 Convertigo SA.
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

package com.twinsoft.convertigo.engine.servlets;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.enums.Parameter;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.convertigo.engine.util.HttpUtils;
import com.twinsoft.convertigo.engine.util.URLUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class WidgetsServlet extends HttpServlet {
	
	private static final long serialVersionUID = -6386425197912471410L;
	
	private static final Pattern reg_project = Pattern.compile("/widgets/([^/]*)$");
	
	enum WidgetType {
		gadget
	}
        
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, java.io.IOException {
        doRequest(request, response);
    }
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, java.io.IOException {
        doRequest(request, response);
    }
    	
    protected void doRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, java.io.IOException {
    	try {
    		String requestUri = HttpUtils.originalRequestURI(request);
	    	Matcher mat_project = reg_project.matcher(requestUri);
	    	
	    	if (!mat_project.find()) {
	    		response.sendError(500, "cannot find project name");
	    		return;
	    	}
    		String project_name = mat_project.group(1);
    		
    		Project project = Engine.theApp.databaseObjectsManager.getProjectByName(project_name);
    		Map<String, String[]> parameters = new HashMap<String, String[]>(GenericUtils.<Map<String, String[]>>cast(request.getParameterMap()));
    		Map<String, String> unique_parameters = GenericUtils.uniqueMap(parameters);
    		
    		
    		WidgetType widget_type = null;
    		try {
    			widget_type = WidgetType.valueOf(unique_parameters.remove(Parameter.WidgetType.getName()));
    		} catch (Exception e) {
				throw new EngineException("wrong '"+Parameter.WidgetType.getName()+"' parameter");
			}
    		
    		String widget_name = unique_parameters.remove(Parameter.WidgetName.getName());
    		if (widget_name == null) {
    			widget_name = project_name;
    		}
    		
    		String url = requestUri.replaceFirst("widgets", "projects") + "/index.html#";
    		
    		switch (widget_type) {
    		case gadget :
        	   	Document document = XMLUtils.getDefaultDocumentBuilder().newDocument();
            	Element e_module = document.createElement("Module");
            	
            	Element e_module_prefs = document.createElement("ModulePrefs");
            	e_module_prefs.setAttribute("title", widget_name);
            	e_module_prefs.setAttribute("description", "From C-EMS '" + project_name + "' project (" + project.getComment() + ")");
            	
            	
            	Element e_content = document.createElement("Content");
            	e_content.setAttribute("type", "url");
            	
            	unique_parameters.put(Parameter.WidgetContainer.getName(), "gatein");
            	url += URLUtils.mapToQuery(parameters);
            	e_content.setAttribute("href", url);

            	e_module.appendChild(e_module_prefs);
            	e_module.appendChild(e_content);
            	
            	document.appendChild(e_module);
            	response.setCharacterEncoding("UTF-8");
            	response.setContentType("text/xml");
            	XMLUtils.prettyPrintDOMWithEncoding(document, "UTF-8", response.getWriter());
    		}

	    	
    	} catch (EngineException e) {
    		response.sendError(500, "unhandled error : " + e.getMessage());
    	}
    }
}