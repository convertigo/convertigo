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

package com.twinsoft.convertigo.engine.admin.services.engine;

import java.util.Iterator;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.admin.services.XmlService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.util.XMLUtils;

@ServiceDefinition(
		name = "GetEnvironmentVariablesList",
		roles = { Role.TEST_PLATFORM, Role.SYMBOLS_VIEW, Role.HOME_VIEW, Role.HOME_CONFIG },
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
			String name = environmentVariable.getKey();
			String value = environmentVariable.getValue();
			name = XMLUtils.stripNonValidXMLCharacters(name);
			value = XMLUtils.stripNonValidXMLCharacters(value);
			environmentVariableElement.setAttribute("name", name);
			environmentVariableElement.setAttribute("value", value);
			environmentVariablesListElement.appendChild(environmentVariableElement);
		}
	}

}
