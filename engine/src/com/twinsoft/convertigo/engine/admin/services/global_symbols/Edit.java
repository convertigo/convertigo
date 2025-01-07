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

package com.twinsoft.convertigo.engine.admin.services.global_symbols;

import javax.servlet.http.HttpServletRequest;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.XmlService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;

@ServiceDefinition(
		name = "Edit",
		roles = { Role.WEB_ADMIN, Role.SYMBOLS_CONFIG },
		parameters = {},
		returnValue = "edit an existing global symbol"
	)
public class Edit extends XmlService {

	protected void getServiceResult(HttpServletRequest request, Document document) throws Exception {
		String symbolName = request.getParameter("symbolName");
		String symbolValue = request.getParameter("symbolValue");
		String oldSymbolName = request.getParameter("oldSymbolName");
		
		Element root = document.getDocumentElement();
		Element response = document.createElement("response");

		try {
			Engine.theApp.databaseObjectsManager.symbolsEdit(oldSymbolName, symbolName, symbolValue);
			response.setAttribute("state", "success");
			response.setAttribute("message","Global symbol '" + symbolName + "' have been successfully edited!");
		} catch (Exception e) {
			Engine.logAdmin.error("Error during editing the global symbol!\n" + e.getMessage());
			
			response.setAttribute("state", "error");
			response.setAttribute("message","Error during editing the global symbol!\n" + e.getMessage());
		}
		root.appendChild(response);		
	}
}
