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

package com.twinsoft.convertigo.engine.admin.services.engine;

import java.io.InputStream;
import java.util.Date;
import java.util.Properties;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.XmlService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;

@ServiceDefinition(
		name = "GetStatus",
		roles = { Role.TEST_PLATFORM },
		parameters = {},
		returnValue = "the engine status"
	)
public class GetStatus extends XmlService {

	protected void getServiceResult(HttpServletRequest request, Document document) throws Exception {
		Element rootElement = document.getDocumentElement();
        
        long currentTimeMillis = System.currentTimeMillis();
        Date startStopDate = new Date(Engine.startStopDate);
        long lStartStopDate = startStopDate.getTime();
        long lRunningElapseDays = (currentTimeMillis - Engine.startStopDate / (24*60*60*1000));
        long lRunningElapseHours = ((currentTimeMillis - Engine.startStopDate / (60*60*1000)) % 24);
        long lRunningElapseMin = ((currentTimeMillis - Engine.startStopDate / (60*1000)) % 60);
        long lRunningElapseSec = ((currentTimeMillis - Engine.startStopDate / 1000) % 60);

        Element versionElement = document.createElement("version");
        versionElement.setAttribute("product", com.twinsoft.convertigo.engine.Version.fullProductVersion);
        versionElement.setAttribute("id", com.twinsoft.convertigo.engine.Version.fullProductVersionID);
        versionElement.setAttribute("beans", com.twinsoft.convertigo.beans.Version.version);
        versionElement.setAttribute("engine", com.twinsoft.convertigo.engine.Version.version);
        versionElement.setAttribute("build", com.twinsoft.convertigo.engine.Version.revision);
        rootElement.appendChild(versionElement);
        
        try {
			Element buildElement = document.createElement("build");
			Properties properties = new Properties();
			ServletContext servletContext = request.getSession().getServletContext();
			InputStream buildInfoFile = servletContext.getResourceAsStream("/WEB-INF/build.txt");
			if (buildInfoFile != null) {
				properties.load(buildInfoFile);
				buildElement.setAttribute("date", properties.getProperty("build.date"));
				buildElement.setAttribute("filename", properties.getProperty("build.filename"));
				buildElement.setAttribute("version", properties.getProperty("build.version"));
				rootElement.appendChild(buildElement);
			}
		} catch (Exception e) {
			// Ignore
			Engine.logAdmin.error("Unable to get build info", e);
		}
        
        Element EngineState = document.createElement("engineState");        
        Text textStart = null;
        if(Engine.isStarted)
        	textStart = document.createTextNode("started");
        else
        	textStart = document.createTextNode("stopped");
        EngineState.appendChild(textStart);
        rootElement.appendChild(EngineState);
        
        Element startStopDateElement = document.createElement("startStopDate");
        Text textNode = document.createTextNode(String.valueOf(lStartStopDate));
        startStopDateElement.appendChild(textNode);
        rootElement.appendChild(startStopDateElement);
        
        Element runningElapseElement = document.createElement("runningElapse");
        runningElapseElement.setAttribute("days", String.valueOf(lRunningElapseDays));
        runningElapseElement.setAttribute("hours", String.valueOf(lRunningElapseHours));
        runningElapseElement.setAttribute("minutes", String.valueOf(lRunningElapseMin));
        runningElapseElement.setAttribute("seconds", String.valueOf(lRunningElapseSec));
        textNode = document.createTextNode("");
        runningElapseElement.appendChild(textNode);
        rootElement.appendChild(runningElapseElement);
        
        Element dateTime=document.createElement("time");
        dateTime.setTextContent(""+System.currentTimeMillis());       
        rootElement.appendChild(dateTime);
	}
	
	

}


