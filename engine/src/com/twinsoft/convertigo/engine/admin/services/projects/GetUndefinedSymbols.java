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

package com.twinsoft.convertigo.engine.admin.services.projects;

import java.util.Set;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.XmlService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceParameterDefinition;

@ServiceDefinition(
		name = "GetUndefinedSymbols",
		roles = { Role.TEST_PLATFORM, Role.PROJECTS_CONFIG, Role.PROJECTS_VIEW },
		parameters = {
				@ServiceParameterDefinition(
						name = "projectName",
						description = "the name of the project to retrieve undefined global symbols"
					)
			},
		returnValue = "all project's undefined global symbols"
	)
public class GetUndefinedSymbols extends XmlService {

	protected void getServiceResult(HttpServletRequest request, Document document) throws Exception {
		Element root = document.getDocumentElement();
		String projectName = request.getParameter("projectName");
		final Set<String> allUndefinedSymbols = Engine.theApp.databaseObjectsManager.symbolsGetUndefined(projectName);
		
		Element undefined_symbols = document.createElement("undefined_symbols");
		if (allUndefinedSymbols != null) {
			for (String undefinedSymbol : new TreeSet<String>(allUndefinedSymbols)) {
				Element symbol = document.createElement("symbol");
				symbol.setTextContent(undefinedSymbol);
				undefined_symbols.appendChild(symbol);
			}
		}
		root.appendChild(undefined_symbols);
	}
}