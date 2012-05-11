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

package com.twinsoft.convertigo.engine.admin.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.CertificateManager;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.util.XMLUtils;

public class ServiceUtils {

	public static String getParameter(ServletRequest request, String parameterName, String defaultValue) {
		String res = request.getParameter(parameterName);
		if (res == null) {
			res = defaultValue;
		}
		return res;
	}
	
	public static String getRequiredParameter(ServletRequest request, String parameterName) {
		String rep = request.getParameter(parameterName);
		if (rep == null) {
			throw new IllegalArgumentException("The parameter '" + parameterName + "' is required!");
		}
		return rep;
	}

	public static void addMessage(Document document, Element root, String message, String tagName) {
		addMessage(document, root, message, tagName, true);
	}

	public static void addMessage(Document document, Element root, String message, String tagName, Boolean usingCDATA) {
		Element line = document.createElement(tagName);
		if (usingCDATA) {
			CDATASection cdata = document.createCDATASection(message);
			line.appendChild(cdata);
		} else {
			line.setTextContent(message);
		}
		root.appendChild(line);
	}

	public static FileAndProperties startCertificate() {
		File file = new File(Engine.CERTIFICATES_PATH + CertificateManager.STORES_PROPERTIES_FILE_NAME);
		Properties storesProperties = new Properties();
		try {
			FileInputStream fis = new FileInputStream(file);
			storesProperties.load(fis);
			fis.close();
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// Creation of the vector containing the certificates and the one
		// containing the links
		List<String> certificates = new LinkedList<String>();
		Set<String> links = new TreeSet<String>();
		for (Object key : storesProperties.keySet()) {
			String tmp = (String) key;
			if (tmp.indexOf("projects.") != 0 && tmp.indexOf("tas.") != 0) {
				if (!tmp.endsWith(".type") && !tmp.endsWith(".group")) {
					certificates.add(tmp);
				}
			} else {
				links.add(tmp);
			}
		}

		return new FileAndProperties(file, storesProperties);
	}
	
	public static void handleError(Document document, HttpServletResponse response) throws ServiceException {
		try {
	        response.setStatus(500);
	        // Bugfix IE #1622: do not forget to set the content type!
	        response.setContentType("text/xml");
			response.setCharacterEncoding("UTF-8");
			XMLUtils.prettyPrintDOMWithEncoding(document, "UTF-8", response.getWriter());
		} catch (Throwable t) {
			throw new ServiceException("Unable to handle error", t);
		}
	}
	
	public static void handleError(Throwable throwable, HttpServletResponse response) throws ServiceException {
		try {
	        handleError(DOMUtils.handleError(throwable), response);
		} catch (ParserConfigurationException e) {
			throw new ServiceException("Unable to create error document from throwable", e);
		}
	}
	
	public static void handleError(String sMessage, HttpServletResponse response) throws ServiceException {
		try {
	        handleError(DOMUtils.handleError(sMessage), response);
		} catch (ParserConfigurationException e) {
			throw new ServiceException("Unable to create error document string", e);
		}
	}
	
	public static String getAdminInstance(HttpServletRequest request) {
		String logmanager_id = request.getHeader("Admin-Instance");
        return (logmanager_id == null) ? "" : logmanager_id;
	}
	
	public static void addRoleNodes(Element parent, Role[] roles) {
		Document document = parent.getOwnerDocument();
		Element e_roles = (Element) parent.appendChild(document.createElement("roles"));
		for (Role role : roles) {
			((Element) e_roles.appendChild(document.createElement("role"))).setAttribute("name", role.name());
		}
	}
	
	//method callable from both the Certificates.mapping.Delete and the Certificates.Delete services
	public static void deleteMapping(Properties storesProperties, String linkToDelete, Document document, Element root) throws ServiceException {
		if(storesProperties.remove(linkToDelete)!=null){
			//message+="Mapping "+link+" has successfully been deleted.\n";		
			ServiceUtils.addMessage(document,root,"Mapping "+linkToDelete+" has successfully been deleted.","message");
		}else{
			throw new ServiceException("Mapping "+linkToDelete+" didn't exist ");
		}
	}
	
	
}
