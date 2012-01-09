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
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;

import javax.servlet.http.HttpServletRequest;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.engine.CertificateManager;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.XmlService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceParameterDefinition;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition.Role;
import com.twinsoft.convertigo.engine.admin.util.ServiceUtils;
import com.twinsoft.convertigo.engine.util.GenericUtils;
import com.twinsoft.util.DESKey;
import com.twinsoft.util.QuickSort;

@ServiceDefinition(
		name = "Configure",
		roles = { Role.WEB_ADMIN },
		parameters = {		
			@ServiceParameterDefinition(
					name = "group_{i}",
					description = "the group of the certificate i (0 first)"
				),
			@ServiceParameterDefinition(
					name = "name_{i}",
					description = "the name of the certificate i"
				),
			@ServiceParameterDefinition(
					name = "pwd_{i}",
					description = "the password of the certificate i"
				),
			@ServiceParameterDefinition(
					name = "type_{i}",
					description = "the type of the certificate i: client | server"
				)
		},
		returnValue = ""
	)
public class Configure extends XmlService {
	
	protected void getServiceResult(HttpServletRequest request, Document document) throws Exception {
				
		Element rootElement = document.getDocumentElement();
	
			
		File file = new File(Engine.CERTIFICATES_PATH + CertificateManager.STORES_PROPERTIES_FILE_NAME);
		Properties storesProperties = new Properties();
		FileInputStream fis = new FileInputStream(file);
		storesProperties.load(fis);
		fis.close();

		// Creation of the vector containing the certificates and the one containing the links
		//Vector<String> certifVector = new Vector<String>();
		Vector<String> linksVector = new Vector<String>();
		String tmp = "";
		Enumeration<?> storesKeysEnum = storesProperties.propertyNames();
		while(storesKeysEnum.hasMoreElements()) {
			tmp = (String)storesKeysEnum.nextElement();
			if ( tmp.indexOf("projects.")!=0 && tmp.indexOf("tas.")!=0 ) {
//				if ( !tmp.endsWith(".type") && !tmp.endsWith(".group") ){
//					certifVector.add(tmp);
//				}
			} else
				linksVector.add(tmp);
		}
		QuickSort quickSort = new QuickSort(linksVector);
		linksVector = GenericUtils.cast(quickSort.perform(true));
		
		File certifDirectory = new File(Engine.CERTIFICATES_PATH);
    	File certifList[] = certifDirectory.listFiles();
    	ArrayList<String> possibleCertificates=new ArrayList<String>();
    	for(int i=0;i<certifList.length;i++){
    		possibleCertificates.add(certifList[i].getName());
    	}
    	
		String certifName = "";
		String certifPwd = "";
		String group;
		//Properties newStoresProperties = new Properties();
		int i = 0;
		while ( ((certifName=(String)request.getParameter("name_"+i)) != null)
			  &&((certifPwd =(String)request.getParameter("pwd_"+i))  != null) ) {
			if (possibleCertificates.contains(certifName)) {
				storesProperties.setProperty(certifName, DESKey.encodeToHexString(certifPwd));
				storesProperties.setProperty(certifName+".type", (String) request.getParameter("type_"+i));

				group = (String) request.getParameter("group_"+i);
				if (group != null) {
					storesProperties.setProperty(certifName+".group", group);
				}
			}else{
				throw new ServiceException("You tried to configure an uninstalled certificate!");
			}
			i++;
		}
		
		storesKeysEnum = linksVector.elements();
		while (storesKeysEnum.hasMoreElements()) {
			certifName = (String) storesKeysEnum.nextElement();
			storesProperties.setProperty(certifName, storesProperties.getProperty(certifName));
		}
		
		FileOutputStream fos = new FileOutputStream(file);
		storesProperties.store(fos , "");
		fos.flush();
		fos.close();
		ServiceUtils.addMessage(document,rootElement,"The certificates have successfully been updated.","message");
	}
}	