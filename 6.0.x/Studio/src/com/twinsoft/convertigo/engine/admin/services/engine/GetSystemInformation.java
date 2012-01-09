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

import java.net.InetAddress;
import java.text.DateFormat;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.admin.services.XmlService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition.Role;

@ServiceDefinition(
		name = "GetSystemInformation",
		roles = { Role.TEST_PLATFORM },
		parameters = {},
		returnValue = "the system information (JVM, memory, CPU, network...)"
	)
public class GetSystemInformation extends XmlService{

	protected void getServiceResult(HttpServletRequest request, Document document) throws Exception {
		Element rootElement = document.getDocumentElement();

		Date currentDate = new Date(System.currentTimeMillis());
        
        String hostName = "n/a";
		String addresses = "n/a";

		InetAddress address = InetAddress.getLocalHost();
		hostName = address.getHostName();

		InetAddress[] inetAddresses = InetAddress.getAllByName(hostName);
		addresses = "";
		addresses += inetAddresses[0].getHostAddress();
		for (int i = 1 ; i < inetAddresses.length ; i++) {
			addresses += ", " + inetAddresses[i].getHostAddress();
		}
        
        Runtime runtime = Runtime.getRuntime();
		
        Element dateElement = document.createElement("currentDate");
        Text textNode = document.createTextNode(DateFormat.getDateInstance(DateFormat.FULL).format(currentDate)+"-"+DateFormat.getTimeInstance(DateFormat.MEDIUM).format(currentDate));
        dateElement.appendChild(textNode);
        rootElement.appendChild(dateElement);
        
        Element javaElement = document.createElement("java");
        javaElement.setAttribute("version", System.getProperty("java.version"));
        javaElement.setAttribute("classVersion", System.getProperty("java.class.version"));
        javaElement.setAttribute("vendor", System.getProperty("java.vendor"));
        rootElement.appendChild(javaElement);
        
        Element hostElement = document.createElement("host");
        hostElement.setAttribute("name", hostName);
        hostElement.setAttribute("addresses", addresses);
        rootElement.appendChild(hostElement);
        
        Element osElement = document.createElement("os");
        osElement.setAttribute("name", System.getProperty("os.name"));
        osElement.setAttribute("version", System.getProperty("os.version"));
        osElement.setAttribute("architecture", System.getProperty("os.arch"));
        osElement.setAttribute("availableProcessors", Integer.toString(runtime.availableProcessors()));
        rootElement.appendChild(osElement);
        
        Element memoryElement = document.createElement("memory");
        memoryElement.setAttribute("total", Long.toString(runtime.totalMemory()));
        memoryElement.setAttribute("available", Long.toString(runtime.freeMemory()));
        memoryElement.setAttribute("maximal", Long.toString(runtime.maxMemory()));
        rootElement.appendChild(memoryElement);
        
        Element browserElement = document.createElement("browser");
        Text browserTextNode = document.createTextNode(request.getHeader("User-Agent"));
        browserElement.appendChild(browserTextNode);
        rootElement.appendChild(browserElement);
        
        Element contextsInUseElement = document.createElement("contextsInUse");
        Text connectedUsersTextNode = document.createTextNode(Integer.toString(Engine.theApp.contextManager.getNumberOfContexts()));
        contextsInUseElement.appendChild(connectedUsersTextNode);
        rootElement.appendChild(contextsInUseElement);
        
        Element threadsElement = document.createElement("threads");
        Text threadsTextNode = document.createTextNode(com.twinsoft.convertigo.beans.core.RequestableObject.nbCurrentWorkerThreads+" / "+Integer.parseInt(EnginePropertiesManager.getProperty(PropertyName.DOCUMENT_THREADING_MAX_WORKER_THREADS)));
        threadsElement.appendChild(threadsTextNode);
        rootElement.appendChild(threadsElement);
        
        Element cloudElement = document.createElement("cloud_instance");
        cloudElement.appendChild(document.createTextNode(Boolean.toString(Engine.isCloudMode())));
        rootElement.appendChild(cloudElement);
	}

}
