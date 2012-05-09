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

package com.twinsoft.convertigo.engine.admin.services.certificates;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.engine.CertificateManager;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.XmlService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition.Role;
import com.twinsoft.convertigo.engine.util.Crypto2;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.util.QuickSort;

@ServiceDefinition(
		name = "List",
		roles = { Role.WEB_ADMIN },
		parameters = {},
		returnValue = ""
	)
public class List extends XmlService {

	protected void getServiceResult(HttpServletRequest request, Document document) throws Exception {
		Element rootElement = document.getDocumentElement();

		Element certificates = document.createElement("certificates");
        rootElement.appendChild(certificates);
        
        File file = new File(Engine.CERTIFICATES_PATH + CertificateManager.STORES_PROPERTIES_FILE_NAME);
        Properties storesProperties = new Properties();
        FileInputStream fis;
		try {
			fis = new FileInputStream(file);
			storesProperties.load(fis);
	        fis.close();
		} catch (Exception e) {
			String message="Unexpected exception";
			Engine.logEngine.error(message, e);
			throw new ServiceException(message,e);
		}
        
        
        Vector<String> certifVector = new Vector<String>();
        Vector<String> linksVector = new Vector<String>();
        String tmp = "";
        Enumeration<?> storesKeysEnum = storesProperties.propertyNames();
        while(storesKeysEnum.hasMoreElements()) {
        	tmp = (String)storesKeysEnum.nextElement();
        	if ( tmp.indexOf("projects.")!=0 && tmp.indexOf("tas.")!=0 ) {
        		if ( !tmp.endsWith(".type") && !tmp.endsWith(".group") )
        			certifVector.add(tmp);
        	} else
        		linksVector.add(tmp);
        }
        QuickSort quickSort = new QuickSort(linksVector);
		linksVector = GenericUtils.cast(quickSort.perform(true));
		
        storesKeysEnum = certifVector.elements();
    	
    	String certificateName, certificateType, certificatePwd, certificateGroup;
    	ArrayList<String> installedCertificates=new ArrayList<String>() ;
    	while (storesKeysEnum.hasMoreElements()) {
    		certificateName  = (String) storesKeysEnum.nextElement();
    		certificateType  = (String) storesProperties.getProperty(certificateName + ".type");
    		certificatePwd   = Crypto2.decodeFromHexString((String) storesProperties.getProperty(certificateName));
    		certificateGroup = (String) storesProperties.getProperty(certificateName + ".group");

    		
    		Element certificateElement = document.createElement("certificate");
    		certificateElement.setAttribute("name", certificateName);
    		certificateElement.setAttribute("type", certificateType);
    		certificateElement.setAttribute("password", certificatePwd);
    		certificateElement.setAttribute("validPass", Boolean.toString(CertificateManager.checkCertificatePassword(certificateType, Engine.CERTIFICATES_PATH + "/" + certificateName, certificatePwd)));
    		certificateElement.setAttribute("group", certificateGroup);
    		certificates.appendChild(certificateElement);
    		installedCertificates.add(certificateName);
    		
    	}
    	
    
    	Element candidates = document.createElement("candidates");
    	rootElement.appendChild(candidates);    	
    	File certifDirectory = new File(Engine.CERTIFICATES_PATH);
    	File certifList[] = certifDirectory.listFiles();
    	for (int k=0 ; k<certifList.length ; k++) {
    		String certifName=certifList[k].getName();
    		String certificateExtensionName = certifName.replaceFirst(".*\\.", ".");
    		if(CertificateManager.isCertificateExtension(certificateExtensionName)) {
    			if(!installedCertificates.contains(certifName)){
    				Element candidateElement = document.createElement("candidate");
	    			candidateElement.setAttribute("name", certifName);
	    			candidates.appendChild(candidateElement);
    			}
    		}
    	}
    											
    											
    	
    	Element bindings = document.createElement("bindings");
    	rootElement.appendChild(bindings);
    	
    	Element anonymous = document.createElement("anonymous");
    	bindings.appendChild(anonymous);
    	
    	boolean cariocaLinksExist = false;
    	String link = "";
    	storesKeysEnum = linksVector.elements();
    	while (storesKeysEnum.hasMoreElements()) {
    		// Entire link
    		link = (String) storesKeysEnum.nextElement();

    		// Name of the certificate linked to this Convertigo link
    		certificateName = (String)storesProperties.getProperty(link);

    		// CUT OF THE ENTIRE LINK
    		// Targetted object of this link : 'tas' (carioca/vic) or 'projects' (Convertigo)
    		StringTokenizer st = new StringTokenizer(link.substring(0, link.length()-13), ".");
    		String targettedObject = st.nextToken();
    		if (targettedObject.equals("tas")) {
    			cariocaLinksExist = true;
    			// There isn't more Convertigo links
    			break; 
    		}

    		// Convertigo project
    		String convProject = "";
    		if (st.hasMoreTokens()) 
    			convProject = st.nextToken();
    		
    		Element bindingElement = document.createElement("binding");
    		bindingElement.setAttribute("projectName", convProject);
    		bindingElement.setAttribute("certificateName", certificateName);
    		anonymous.appendChild(bindingElement);
    		
    	}
    	
    	Element carioca = document.createElement("carioca");
    	bindings.appendChild(carioca);
    	
    	while ( cariocaLinksExist || storesKeysEnum.hasMoreElements() ) {
    		// Entire link
    		if (cariocaLinksExist)
    			// In the first loop, the entire link is already got (in the loop concerning the Convertigo links)
    			cariocaLinksExist = false;
    		else
    			link = (String) storesKeysEnum.nextElement();

    		// Name of the certificate linked to this Carioca link
    		certificateName = (String)storesProperties.getProperty(link);
    		
    		// CUT OF THE ENTIRE LINK
    		// the Targetted object of this link is 'tas' (carioca)
    		StringTokenizer st = new StringTokenizer(link.substring(0, link.length()-13), ".");
    		
    		// Project
    		String virtualServer = "";
    		String group ="";
    		String user ="";
    		String project ="";
    		
    		// tas
    		if (st.hasMoreTokens())
    			st.nextToken();
    		
    		if (link.indexOf("projects")==-1) {
    			// Virtual Server
    			if (st.hasMoreTokens())
    				virtualServer = st.nextToken();
    			
    			// Group of the user
    			if (st.hasMoreTokens())
    				group = st.nextToken();
    	
    			// User
    			if (st.hasMoreTokens())
    				user = st.nextToken();
    		}
    		else {
    			while (st.hasMoreTokens()) {
    				tmp = st.nextToken();
    				
    				if (tmp.equals("projects")) {
    					tmp = st.nextToken();
    					project = tmp;
    				}
    				else {
    					if (virtualServer.equals(""))
    						virtualServer = tmp;
    					else {
    						if (group.equals(""))
    							group = tmp;
    						else {
    							if (user.equals(""))
    								user = tmp;
    						}
    					}
    				}
    			}
    		}
    		
    		
    		Element bindingElement = document.createElement("binding");
		    bindingElement.setAttribute("projectName", project);
		    bindingElement.setAttribute("virtualServerName", virtualServer);
		    bindingElement.setAttribute("imputationGroup", group);
		    bindingElement.setAttribute("userName", user);
		    bindingElement.setAttribute("certificateName", certificateName);
			carioca.appendChild(bindingElement);
    	}
	}

}
