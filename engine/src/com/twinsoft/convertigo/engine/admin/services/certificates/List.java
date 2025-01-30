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
import java.util.StringTokenizer;

import javax.servlet.http.HttpServletRequest;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.CertificateManager;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.XmlService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.util.Crypto2;
import com.twinsoft.convertigo.engine.util.PropertiesUtils;

@ServiceDefinition(
		name = "List",
		roles = { Role.WEB_ADMIN, Role.CERTIFICATE_CONFIG, Role.CERTIFICATE_VIEW },
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

		synchronized (Engine.CERTIFICATES_PATH) {
			try {
				PropertiesUtils.load(storesProperties, file);
			} catch (Exception e) {
				String message = "Unexpected exception";
				Engine.logAdmin.error(message, e);
				throw new ServiceException(message,e);
			}
		}


		java.util.List<String> certifVector = new ArrayList<String>();
		java.util.List<String> linksVector = new ArrayList<String>();
		String tmp = "";
		Enumeration<?> storesKeysEnum = storesProperties.propertyNames();
		while(storesKeysEnum.hasMoreElements()) {
			tmp = (String)storesKeysEnum.nextElement();
			if ( tmp.indexOf("projects.")!=0 && tmp.indexOf("tas.")!=0 ) {
				if ( !tmp.endsWith(".type") && !tmp.endsWith(".group") ) {
					certifVector.add(tmp);
				}
			} else {
				linksVector.add(tmp);
			}
		}
		Collections.sort(linksVector);

		storesKeysEnum = Collections.enumeration(certifVector);

		Engine.logAdmin.debug("Analyzing certificates...");

		String certificateName, certificateType, certificatePwd, certificateGroup;
		java.util.List<String> installedCertificates = new ArrayList<>();
		java.util.List<Element> elts = new ArrayList<>();
		while (storesKeysEnum.hasMoreElements()) {
			certificateName  = (String) storesKeysEnum.nextElement();
			certificateType  = (String) storesProperties.getProperty(certificateName + ".type");
			certificatePwd   = (String) storesProperties.getProperty(certificateName);
			certificateGroup = (String) storesProperties.getProperty(certificateName + ".group");

			Engine.logAdmin.debug("Found certificate:");
			Engine.logAdmin.debug("   name=" + certificateName);
			Engine.logAdmin.debug("   type=" + certificateType);
			Engine.logAdmin.debug("   password (ciphered)=" + certificatePwd);
			Engine.logAdmin.debug("   group=" + certificateGroup);

			if (certificateType == null) {
				Engine.logAdmin.error("Corrupted certificate '"+certificateName+"' : missing type");
			}

			if (certificatePwd.length() > 0) {
				try {
					certificatePwd   = Crypto2.decodeFromHexString((String) storesProperties.getProperty(certificateName));
				} catch (Exception e) {
					Engine.logAdmin.error("Unable to decipher the password", e);
				}
			}

			Element certificateElement = document.createElement("certificate");
			certificateElement.setAttribute("name", certificateName);
			certificateElement.setAttribute("type", certificateType);
			certificateElement.setAttribute("password", certificatePwd);
			certificateElement.setAttribute("validPass", Boolean.toString(CertificateManager.checkCertificatePassword(certificateType, Engine.CERTIFICATES_PATH + "/" + certificateName, certificatePwd)));
			certificateElement.setAttribute("group", certificateGroup);
			installedCertificates.add(certificateName);
			elts.add(certificateElement);
		}

		Collections.sort(elts, (a, b) -> {
			return a.getAttribute("name").compareToIgnoreCase(b.getAttribute("name"));
		});

		for (Element elt: elts) {
			certificates.appendChild(elt);
		}

		Element candidates = document.createElement("candidates");
		rootElement.appendChild(candidates);    	
		File certifDirectory = new File(Engine.CERTIFICATES_PATH);
		File certifList[] = certifDirectory.listFiles();
		
		elts.clear();
		for (int k=0 ; k<certifList.length ; k++) {
			String certifName=certifList[k].getName();
			String certificateExtensionName = certifName.replaceFirst(".*\\.", ".");
			if(CertificateManager.isCertificateExtension(certificateExtensionName)) {
				if(!installedCertificates.contains(certifName)){
					Element candidateElement = document.createElement("candidate");
					candidateElement.setAttribute("name", certifName);
					elts.add(candidateElement);
				}
			}
		}

		Collections.sort(elts, (a, b) -> {
			return a.getAttribute("name").compareToIgnoreCase(b.getAttribute("name"));
		});

		for (Element elt: elts) {
			candidates.appendChild(elt);
		}

		Element bindings = document.createElement("bindings");
		rootElement.appendChild(bindings);

		Element anonymous = document.createElement("anonymous");
		bindings.appendChild(anonymous);

		boolean cariocaLinksExist = false;
		String link = "";
		storesKeysEnum = Collections.enumeration(linksVector);
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
			bindingElement.setAttribute("link", link);
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
			bindingElement.setAttribute("link", link);
			carioca.appendChild(bindingElement);
		}
	}

}
