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
 * $URL: http://sourceus.twinsoft.fr/svn/convertigo/CEMS_opensource/branches/7.1.x/Studio/src/com/twinsoft/convertigo/engine/admin/services/engine/GetJavaSystemProperties.java $
 * $Author: fabienb $
 * $Revision: 30435 $
 * $Date: 2012-05-11 15:21:46 +0200 (ven., 11 mai 2012) $
 */

package com.twinsoft.convertigo.engine.admin.services.engine;

import java.util.Iterator;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.admin.services.XmlService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;

@ServiceDefinition(
		name = "GetEnvironmentVariablesList",
		roles = { Role.TEST_PLATFORM },
		parameters = {},
		returnValue = "the Environment variables list"
	)
public class GetEnvironmentVariablesList extends XmlService {

	protected void getServiceResult(HttpServletRequest request, Document document) throws Exception {
		Element rootElement = document.getDocumentElement();
		
		// Get environment variables
		Iterator<Entry<String, String>> environmentVariables = System.getenv().entrySet().iterator();
		
		Element environmentVariablesListElement = document.createElement("environmentVariables");
		rootElement.appendChild(environmentVariablesListElement);
		
		while (environmentVariables.hasNext()) {
			Entry<String, String> environmentVariable = environmentVariables.next();
			
			Element environmentVariableElement = document.createElement("environmentVariable");
			environmentVariableElement.setAttribute("name", environmentVariable.getKey());
			environmentVariableElement.setAttribute("value", environmentVariable.getValue());
			environmentVariablesListElement.appendChild(environmentVariableElement);
		}
		
	}

}
