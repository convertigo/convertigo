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

import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.methods.FileRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.codehaus.jettison.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.XmlService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.util.URLUtils;
import com.twinsoft.convertigo.engine.util.XMLUtils;
import com.twinsoft.convertigo.engine.util.ZipUtils;

@ServiceDefinition(name = "LaunchBuild", roles = { Role.ANONYMOUS }, parameters = {}, returnValue = "")
public class LaunchBuild extends XmlService {

	private static final Object buildLock = new Object();

	@Override
	protected void getServiceResult(HttpServletRequest request, Document document) throws Exception {
		synchronized(buildLock) {
			String application = request.getParameter("application");
			
			final MobileResourceHelper mobileResourceHelper = new MobileResourceHelper(application, "_private/mobile/www");
			
			mobileResourceHelper.prepareFiles(request);
			
			JSONObject json = new JSONObject();
			mobileResourceHelper.listFiles(json);
			FileUtils.write(new File(mobileResourceHelper.destDir, "files.json"), json.toString());
			
			json = new JSONObject();
			json.put("applicationId", mobileResourceHelper.mobileApplication.getComputedApplicationId());
			json.put("projectName", mobileResourceHelper.mobileApplication.getProject().getName());
			json.put("endPoint", mobileResourceHelper.mobileApplication.getComputedEndpoint(request));
			FileUtils.write(new File(mobileResourceHelper.destDir, "env.json"), json.toString());
			
						
			File configFile = new File(mobileResourceHelper.mobileDir, "config.xml");
			
			// Update config.xml
			Document configXmlDocument = XMLUtils.loadXml(configFile);
			Element configXmlDocumentElement = configXmlDocument.getDocumentElement();
			configXmlDocumentElement.setAttribute("id", mobileResourceHelper.mobileApplication.getComputedApplicationId());

			FileWriter fileWriter = new FileWriter(configFile);
			XMLUtils.prettyPrintDOMWithEncoding(configXmlDocument, "UTF-8", fileWriter);
			fileWriter.close();

			// Build the ZIP file for the mobile device
			File mobileArchiveFile = new File(mobileResourceHelper.destDir.getParentFile(), application + ".zip");
			ZipUtils.makeZip(mobileArchiveFile.getPath(), mobileResourceHelper.destDir.getPath(), null);
			
			// Login to the mobile builder platform
			String mobileBuilderPlatformURL = EnginePropertiesManager
					.getProperty(PropertyName.MOBILE_BUILDER_PLATFORM_URL);
			String mobileBuilderPlatformUsername = EnginePropertiesManager
					.getProperty(PropertyName.MOBILE_BUILDER_USERNAME);
			String mobileBuilderPlatformPassword = EnginePropertiesManager
					.getProperty(PropertyName.MOBILE_BUILDER_PASSWORD);
			
			PostMethod method;
			int methodStatusCode;
			InputStream methodBodyContentInputStream;
			
			String finalApplicationName = GetBuildStatus.getFinalApplicationName(application);
			
			Map<String, String[]> params = new HashMap<String, String[]>();
			params.put("application", new String[]{finalApplicationName});
			params.put("username", new String[]{mobileBuilderPlatformUsername});
			params.put("password", new String[]{mobileBuilderPlatformPassword});

			// Launch the mobile build
			URL url = new URL(mobileBuilderPlatformURL + "/build?" + URLUtils.mapToQuery(params));
			
			HostConfiguration hostConfiguration = new HostConfiguration();
			hostConfiguration.setHost(new URI(url.toString(), true));
			HttpState httpState = new HttpState();
			Engine.theApp.proxyManager.setProxy(hostConfiguration, httpState, url);
			
			method = new PostMethod(url.toString());
			
			FileRequestEntity entity = new FileRequestEntity(mobileArchiveFile, null);
			method.setRequestEntity(entity);

			methodStatusCode = Engine.theApp.httpClient.executeMethod(hostConfiguration, method, httpState);
			methodBodyContentInputStream = method.getResponseBodyAsStream();
			byte[] httpBytes = IOUtils.toByteArray(methodBodyContentInputStream);
			String sResult = new String(httpBytes, "UTF-8");

			if (methodStatusCode != HttpStatus.SC_OK) {
				throw new ServiceException("Unable to build application '" + finalApplicationName + "'; reason: " + sResult);
			}

			JSONObject jsonObject = new JSONObject(sResult);
			Element statusElement = document.createElement("application");
			statusElement.setAttribute("id", jsonObject.getString("id"));
			document.getDocumentElement().appendChild(statusElement);
		}
	}
}
