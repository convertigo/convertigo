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

import javax.servlet.http.HttpServletRequest;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineStatistics;
import com.twinsoft.convertigo.engine.admin.services.XmlService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition.Role;

@ServiceDefinition(
		name = "Monitor",
		roles = { Role.WEB_ADMIN, Role.MONITOR_AGENT },
		parameters = {},
		returnValue = "the monitoring data"
	)
public class Monitor extends XmlService {

	protected void getServiceResult(HttpServletRequest request, Document document) throws Exception {
		Element rootElement = document.getDocumentElement();
		Element objectElement;
		Text objectText;

		objectElement = document.createElement("threads");
		rootElement.appendChild(objectElement);
		objectText = document.createTextNode("" + com.twinsoft.convertigo.beans.core.RequestableObject.nbCurrentWorkerThreads);
		objectElement.appendChild(objectText);

		objectElement = document.createElement("contexts");
		rootElement.appendChild(objectElement);
		objectText = document.createTextNode(Engine.isStarted ? "" + Engine.theApp.contextManager.getNumberOfContexts() : "0");
		objectElement.appendChild(objectText);

		objectElement = document.createElement("requests");
		rootElement.appendChild(objectElement);
		long i = EngineStatistics.getAverage(EngineStatistics.REQUEST);
		if (i == -1) i = 0;
		objectText = document.createTextNode("" + i);
		objectElement.appendChild(objectText);
	}

}
