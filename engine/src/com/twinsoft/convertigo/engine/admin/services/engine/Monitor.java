/*
 * Copyright (c) 2001-2024 Convertigo SA.
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

package com.twinsoft.convertigo.engine.admin.services.engine;

import javax.servlet.http.HttpServletRequest;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineStatistics;
import com.twinsoft.convertigo.engine.admin.services.XmlService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;

@ServiceDefinition(
		name = "Monitor",
		roles = { Role.WEB_ADMIN, Role.MONITOR_AGENT, Role.HOME_VIEW, Role.HOME_CONFIG },
		parameters = {},
		returnValue = "the monitoring data"
	)
public class Monitor extends XmlService {

	protected void getServiceResult(HttpServletRequest request, Document document) throws Exception {
		Element rootElement = document.getDocumentElement();
		Element objectElement;
		
		Runtime runtime = Runtime.getRuntime();
		objectElement = document.createElement("memoryMaximal");
		rootElement.appendChild(objectElement);
		objectElement.setTextContent("" + (runtime.maxMemory() / (1024 * 1024)));
		
		objectElement = document.createElement("memoryTotal");
		rootElement.appendChild(objectElement);
		objectElement.setTextContent("" + (runtime.totalMemory() / (1024 * 1024)));
		
		objectElement = document.createElement("memoryUsed");
		rootElement.appendChild(objectElement);
		objectElement.setTextContent("" + ((runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)));

		objectElement = document.createElement("threads");
		rootElement.appendChild(objectElement);
		objectElement.setTextContent("" + com.twinsoft.convertigo.beans.core.RequestableObject.nbCurrentWorkerThreads);

		objectElement = document.createElement("contexts");
		rootElement.appendChild(objectElement);
		try {
			objectElement.setTextContent(Engine.isStarted ? "" + Engine.theApp.contextManager.getNumberOfContexts() : "0");
		} catch (Exception e) {
			objectElement.setTextContent("0");
		}

		objectElement = document.createElement("requests");
		rootElement.appendChild(objectElement);
		objectElement.setTextContent("" + Math.max(EngineStatistics.getAverage(EngineStatistics.REQUEST), 0));
	}

}
