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

package com.twinsoft.convertigo.engine.admin.services.certificates.mappings;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.CertificateManager;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.XmlService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.admin.util.ServiceUtils;
import com.twinsoft.convertigo.engine.util.PropertiesUtils;

@ServiceDefinition(
		name = "Configure",
		roles = { Role.WEB_ADMIN, Role.CERTIFICATE_CONFIG },
		parameters = {},
		returnValue = ""
	)
public class Configure extends XmlService {

	protected void getServiceResult(HttpServletRequest request, Document document) throws Exception {
		Element rootElement = document.getDocumentElement();
				
		File file;

		file = new File(Engine.CERTIFICATES_PATH + "/" + CertificateManager.STORES_PROPERTIES_FILE_NAME);

		synchronized (Engine.CERTIFICATES_PATH) {
			Properties storesProperties = PropertiesUtils.load(file);

			// Creation of the vector containing the certificates and the one containing the links
			List<String> certifVector = new ArrayList<String>();
			List<String> linksVector = new ArrayList<String>();
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
			Collections.sort(linksVector);

			String certifName = "";
			//Properties newStoresProperties = new Properties();

			int i = 0;
			while ( (tmp=(String)request.getParameter("targettedObject_"+i)) != null) {
				certifName = (String)request.getParameter("cert_"+i);
				if (  !certifName.equals("")) {
					String link = tmp + ".";
					if (tmp.equals("projects")) {
						if ( ((tmp=(String)request.getParameter("convProject_"+i)) != null))
							link += tmp + ".";
					}
					if (tmp.equals("tas")) {
						if ( ((tmp=(String)request.getParameter("virtualServer_"+i)) != null) ) {
							link += tmp + ".";
							if ( ((tmp=(String)request.getParameter("group_"+i)) != null)) {
								link += tmp + ".";
								if ( ((tmp=(String)request.getParameter("user_"+i)) != null))
									link += tmp + ".";
							}
						}
						if ( ((tmp=(String)request.getParameter("project_"+i)) != null))
							link += "projects" + "." + tmp + ".";
					}

					link += storesProperties.getProperty(certifName+".type") + ".store";

					storesProperties.setProperty(link, certifName);


				}
				i++;
			}

			String group;
			storesKeysEnum = Collections.enumeration(certifVector);
			while (storesKeysEnum.hasMoreElements()) {
				certifName = (String) storesKeysEnum.nextElement();
				storesProperties.setProperty(certifName, storesProperties.getProperty(certifName));
				storesProperties.setProperty(certifName+".type", storesProperties.getProperty(certifName+".type"));
				group = (String) storesProperties.getProperty(certifName+".group");
				if (group != null) {
					storesProperties.setProperty(certifName+".group", group);
				}
			}

			PropertiesUtils.store(storesProperties, file);
		}
		ServiceUtils.addMessage(document,rootElement,"The mappings have successfully been updated.","message");
	}
		
}	