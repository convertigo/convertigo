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

package com.twinsoft.convertigo.engine.admin.services.trace;

import javax.servlet.http.HttpServletRequest;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.TracePlayerManager;
import com.twinsoft.convertigo.engine.admin.services.XmlService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition.Role;

@ServiceDefinition(
		name = "List",
		roles = { Role.WEB_ADMIN },
		parameters = {},
		returnValue = ""
	)
public class List extends XmlService{

	protected void getServiceResult(HttpServletRequest request, Document document) throws Exception {
		Element rootElement = document.getDocumentElement();
        
        TracePlayerManager tpm = Engine.theApp.tracePlayerManager;
        
        Element tracesListElement = document.createElement("traces");
        rootElement.appendChild(tracesListElement);
        
        for(TracePlayerManager.TraceConfig trace : tpm.getTraces()){
        	Element connectionElement = document.createElement("trace");
            connectionElement.setAttribute("enabled", Boolean.toString(trace.getEnable()));
            connectionElement.setAttribute("port", Integer.toString(trace.getPort()));
            connectionElement.setAttribute("file", trace.getFile());
            tracesListElement.appendChild(connectionElement);
        }
        
        
        Element tracesListEtrs = document.createElement("etrs");
        rootElement.appendChild(tracesListEtrs);        
        
        for(String candidate : tpm.getEtr()){
        	Element connectionElement = document.createElement("etr");
            connectionElement.setTextContent(candidate);           
            tracesListEtrs.appendChild(connectionElement);
        }
        
        
	}

}
