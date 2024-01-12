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

package com.twinsoft.convertigo.engine.admin.services.global_symbols;

import java.util.Set;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.core.Project;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.XmlService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;

@ServiceDefinition(name = "List", roles = { Role.WEB_ADMIN, Role.SYMBOLS_CONFIG,
		Role.SYMBOLS_VIEW }, parameters = {}, returnValue = "the global symbols list")
public class List extends XmlService {

	protected void getServiceResult(HttpServletRequest request, Document document) throws Exception {
		Element root = document.getDocumentElement();

		Element symbolsListElement = document.createElement("symbols");
		root.appendChild(symbolsListElement);

		Engine.theApp.databaseObjectsManager.symbolsCheck();

		Set<String> symbolNames = Engine.theApp.databaseObjectsManager.symbolsGetNames();

		for (String symbolName : symbolNames) {
			Element symbolElement = document.createElement("symbol");
			symbolElement.setAttribute("name", symbolName);
			symbolElement.setAttribute("value",
					Engine.theApp.databaseObjectsManager.symbolsGetValueService(symbolName));
			symbolsListElement.appendChild(symbolElement);
		}

		Set<Triple<String, String, String>> defaultSymbols = new TreeSet<>();
		for (String projectName : Engine.theApp.databaseObjectsManager.getAllProjectNamesList()) {
			Project project = Engine.theApp.databaseObjectsManager.getOriginalProjectByName(projectName);
			if (project != null && project.defaultSymbols != null) {
				for (Pair<String, String> pair : project.defaultSymbols) {
					if (!symbolNames.contains(pair.getLeft())) {
						defaultSymbols.add(Triple.of(projectName, pair.getLeft(), pair.getRight()));
					}
				}
			}
		}

		Element defaultSymbolsListElement = document.createElement("defaultSymbols");
		root.appendChild(defaultSymbolsListElement);

		for (Triple<String, String, String> triple : defaultSymbols) {
			Element symbolElement = document.createElement("defaultSymbol");
			symbolElement.setAttribute("project", triple.getLeft());
			symbolElement.setAttribute("name", triple.getMiddle());
			symbolElement.setAttribute("value", triple.getRight());
			defaultSymbolsListElement.appendChild(symbolElement);
		}
	}
}
