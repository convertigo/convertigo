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
 * $URL: http://sourceus/svn/convertigo/CEMS_opensource/trunk/Studio/src/com/twinsoft/convertigo/engine/admin/services/projects/Delete.java $
 * $Author: fabienb $
 * $Revision: 30435 $
 * $Date: 2012-05-11 15:21:46 +0200 (Fri, 11 May 2012) $
 */

package com.twinsoft.convertigo.engine.admin.services.global_symbols;

import java.io.FileOutputStream;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import org.w3c.dom.Document;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.XmlService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;

@ServiceDefinition(
		name = "Update",
		roles = { Role.WEB_ADMIN },
		parameters = {},
		returnValue = ""
	)
public class Update extends XmlService {

	protected void getServiceResult(HttpServletRequest request, Document document) throws Exception {
        String symbols = request.getParameter("symbols");
        int nbSymbol = Integer.parseInt(request.getParameter("nbSymbol"));
        
        Properties prop = new Properties();
        for (int i=0; i<nbSymbol; i++) {
        	String symbol = symbols.split(",")[0];
        	symbols = symbols.substring(symbol.length()+1);
        	String symbolName = symbol.split("=")[0];
        	String value = symbol.split("=")[1];
        	prop.setProperty(symbolName, value);
        } 
        prop.store(new FileOutputStream(Engine.theApp.databaseObjectsManager.getGlobalSymbolsFilePath()), "global symbols");        
	}
}
