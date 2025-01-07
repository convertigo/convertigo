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

package com.twinsoft.convertigo.engine.admin.services.certificates;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.CertificateManager;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.XmlService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceParameterDefinition;
import com.twinsoft.convertigo.engine.admin.util.ServiceUtils;
import com.twinsoft.convertigo.engine.util.Crypto2;
import com.twinsoft.convertigo.engine.util.PropertiesUtils;

@ServiceDefinition(
		name = "Configure",
		roles = { Role.WEB_ADMIN, Role.CERTIFICATE_CONFIG },
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

		synchronized (Engine.CERTIFICATES_PATH) {
			Properties storesProperties = PropertiesUtils.load(file);

			// Creation of the vector containing the certificates and the one containing the links
			java.util.List<String> linksVector = new ArrayList<String>();
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
			Collections.sort(linksVector);

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
					storesProperties.setProperty(certifName, Crypto2.encodeToHexString(certifPwd));
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

			storesKeysEnum = Collections.enumeration(linksVector);
			while (storesKeysEnum.hasMoreElements()) {
				certifName = (String) storesKeysEnum.nextElement();
				storesProperties.setProperty(certifName, storesProperties.getProperty(certifName));
			}
			PropertiesUtils.store(storesProperties, file);
		}
		ServiceUtils.addMessage(document,rootElement,"The certificates have successfully been updated.","message");
	}
}	