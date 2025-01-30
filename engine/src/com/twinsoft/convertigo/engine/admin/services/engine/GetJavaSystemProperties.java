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

package com.twinsoft.convertigo.engine.admin.services.engine;

import java.util.Properties;
import java.util.TreeSet;

import javax.servlet.http.HttpServletRequest;

import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.engine.admin.services.XmlService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;

@ServiceDefinition(
		name = "GetJavaSystemProperties",
		roles = { Role.TEST_PLATFORM, Role.HOME_VIEW, Role.HOME_CONFIG },
		parameters = {},
		returnValue = "the Java system properties"
	)
public class GetJavaSystemProperties extends XmlService{

	protected void getServiceResult(HttpServletRequest request, Document document) throws Exception {
		Element rootElement = document.getDocumentElement();

		Properties properties = System.getProperties();

		StringBuffer sProperties = new StringBuffer();
		for (Object propertyName : new TreeSet<Object>(properties.keySet())) {
			if (propertyName instanceof String pName
					&& (!Engine.isCloudMode() || !(pName.contains("password") || pName.contains("billing")))) {
				sProperties.append(pName + "=" + properties.getProperty(pName) + "\n");
			}
		}

		CDATASection cdata = document.createCDATASection("DATA");
		cdata.setData(sProperties.toString());
		rootElement.appendChild(cdata);
	}

}
