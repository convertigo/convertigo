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

package com.twinsoft.convertigo.engine.admin.services.mobiles;

import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.io.IOUtils;
import org.codehaus.jettison.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.twinsoft.convertigo.beans.core.MobileDevice;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.XmlService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.util.XMLUtils;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;

@ServiceDefinition(name = "GetBuildStatus", roles = { Role.ANONYMOUS }, parameters = {}, returnValue = "")
public class GetBuildStatus extends XmlService {

	@Override
	protected void getServiceResult(HttpServletRequest request, Document document) throws Exception {
		String application = request.getParameter("application");
		
		// Get the final application name from config.xml
		String mobileResourcesPath = Engine.PROJECTS_PATH + "/" + application + "/"
				+ MobileDevice.RESOURCES_PATH;

		Document configXmlDocument = XMLUtils.loadXml(mobileResourcesPath + "/config.xml");
		NodeList nodeList = configXmlDocument.getElementsByTagName("name");
		Element nameElement = (Element) nodeList.item(0);
		String finalApplicationName = nameElement.getTextContent();
		
		String platform = request.getParameter("platform");

		String url;
		PostMethod method;
		int methodStatusCode;
		InputStream methodBodyContentInputStream;

		String mobileBuilderPlatformURL = EnginePropertiesManager
				.getProperty(PropertyName.MOBILE_BUILDER_PLATFORM_URL);
		String mobileBuilderPlatformUsername = EnginePropertiesManager
				.getProperty(PropertyName.MOBILE_BUILDER_USERNAME);
		String mobileBuilderPlatformPassword = EnginePropertiesManager
				.getProperty(PropertyName.MOBILE_BUILDER_PASSWORD);

		url = mobileBuilderPlatformURL + "/getstatus";
		method = new PostMethod(url);

		JSONObject jsonResult;
		try {
			method.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
			method.setRequestBody(new NameValuePair[] {
					new NameValuePair("application", finalApplicationName),
					new NameValuePair("username", mobileBuilderPlatformUsername),
					new NameValuePair("password", mobileBuilderPlatformPassword) });

			methodStatusCode = Engine.theApp.httpClient.executeMethod(method);

			methodBodyContentInputStream = method.getResponseBodyAsStream();
			byte[] httpBytes = IOUtils.toByteArray(methodBodyContentInputStream);
			String sResult = new String(httpBytes, "UTF-8");

			if (methodStatusCode != HttpStatus.SC_OK) {
				throw new ServiceException("Unable to get building status for application '" + application
						+ "' (final app name: '" + finalApplicationName + "'); reason: " + sResult);
			}

			jsonResult = new JSONObject(sResult);
//			try {
//				String sError = jsonResult.getString("error");
//				throw new ServiceException(sError);
//			} catch (JSONException e) {
//				// no error
//			}
		} finally {
			method.releaseConnection();
		}

		Element statusElement = document.createElement("build");
		statusElement.setAttribute("application", application);
		statusElement.setAttribute("platform", platform);
		
		if (jsonResult.has(platform + "_status")) {
			statusElement.setAttribute("status", jsonResult.getString(platform + "_status"));
		}
		else {
			statusElement.setAttribute("status", "none");
		}

		if (jsonResult.has(platform + "_error")) {
			statusElement.setAttribute("error", jsonResult.getString(platform + "_error"));
		}

		document.getDocumentElement().appendChild(statusElement);
	}

}
