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

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.XmlService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;

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
        String sStartStopDate = DateFormat.getDateInstance(DateFormat.LONG, Locale.getDefault()).format(startStopDate) + " - " + DateFormat.getTimeInstance(DateFormat.MEDIUM, Locale.getDefault()).format(startStopDate);
        long runningElapse = currentTimeMillis - Engine.startStopDate;
        String sRunningElapse =
        	(runningElapse / (24*60*60*1000)) + " day(s), " +
        	((runningElapse / (60*60*1000)) % 24) + " hour(s), " +
        	((runningElapse / (60*1000)) % 60) + " minute(s), " +
        	((runningElapse / 1000) % 60) + " second(s)";

        Element versionElement = document.createElement("version");
        versionElement.setAttribute("product", com.twinsoft.convertigo.engine.Version.fullProductVersion);
        versionElement.setAttribute("beans", com.twinsoft.convertigo.beans.Version.version);
        versionElement.setAttribute("engine", com.twinsoft.convertigo.engine.Version.version);
        versionElement.setAttribute("build", com.twinsoft.convertigo.engine.Version.revision);
        rootElement.appendChild(versionElement);
        
        Element EngineState = document.createElement("engineState");        
        Text textStart = null;
        if(Engine.isStarted)
        	textStart = document.createTextNode("started");
        else
        	textStart = document.createTextNode("stopped");
        EngineState.appendChild(textStart);
        rootElement.appendChild(EngineState);
        
        Element startStopDateElement = document.createElement("startStopDate");
        Text textNode = document.createTextNode(sStartStopDate);
        startStopDateElement.appendChild(textNode);
        rootElement.appendChild(startStopDateElement);
        
        Element runningElapseElement = document.createElement("runningElapse");
        textNode = document.createTextNode(sRunningElapse);
        runningElapseElement.appendChild(textNode);
        rootElement.appendChild(runningElapseElement);
        
        Element dateTime=document.createElement("time");
        dateTime.setTextContent(""+System.currentTimeMillis());       
        rootElement.appendChild(dateTime);
	}
	
	

}


