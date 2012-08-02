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
 * $URL: http://sourceus/svn/convertigo/CEMS_opensource/trunk/Studio/src/com/twinsoft/convertigo/engine/admin/services/global_symbols/List.java $
 * $Author: fabienb $
 * $Revision: 30435 $
 * $Date: 2012-05-11 15:21:46 +0200 (Fri, 11 May 2012) $
 */

package com.twinsoft.convertigo.engine.admin.services.global_symbols;

import java.io.FileInputStream;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.XmlService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;

@ServiceDefinition(
		name = "GetGlobalSymbols",
		roles = { Role.WEB_ADMIN },
		parameters = {},
		returnValue = "the global symbols list"
	)
public class List extends XmlService{

	protected void getServiceResult(HttpServletRequest request, Document document) throws Exception {
		Element root = document.getDocumentElement();
        
        Element symbolsListElement = document.createElement("symbols");
        root.appendChild(symbolsListElement);
        
        Properties prop = new Properties();
        try {
        	FileInputStream in = new FileInputStream(Engine.CONFIGURATION_PATH + "/global_symbols.properties");
	        prop.load(in);
	        in.close();
        }
        catch (Exception e) {}
        
        int nbList = prop.size();
        String listKeys = prop.keySet().toString();
        String listValues = prop.values().toString();
        
    	for (int i = 0; i<nbList; i++) {
    		String symbol = listKeys.split(",")[0];
    		String symbolValue = listValues.split(",")[0];
    		int nbChar = symbol.length() + 2;
    		if (listKeys.length() > nbChar) {
	    		listKeys = listKeys.substring(nbChar);
				nbChar = symbolValue.length() + 2;
				listValues =listValues.substring(nbChar);
    		}
			if (i == 0) {
				symbol = symbol.substring(1);
				symbolValue = symbolValue.substring(1);
			}
			if (i == nbList-1) {
				symbol = symbol.split("]")[0];
				symbolValue = symbolValue.split("]")[0];
			}
 
			Element symbolElement = document.createElement("symbol");
			symbolElement.setAttribute("name", symbol);
			symbolElement.setAttribute("value", symbolValue);
			symbolsListElement.appendChild(symbolElement);
    	}	
	}
}
