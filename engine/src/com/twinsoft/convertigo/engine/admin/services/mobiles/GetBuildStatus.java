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

package com.twinsoft.convertigo.engine.admin.services.mobiles;

import java.io.InputStream;
import java.net.URL;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.io.IOUtils;
import org.codehaus.jettison.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.twinsoft.convertigo.beans.core.MobileApplication;
import com.twinsoft.convertigo.engine.AuthenticatedSessionManager.Role;
import com.twinsoft.convertigo.engine.AuthenticationException;
import com.twinsoft.convertigo.engine.Engine;
import com.twinsoft.convertigo.engine.EngineException;
import com.twinsoft.convertigo.engine.EnginePropertiesManager;
import com.twinsoft.convertigo.engine.EnginePropertiesManager.PropertyName;
import com.twinsoft.convertigo.engine.admin.services.ServiceException;
import com.twinsoft.convertigo.engine.admin.services.XmlService;
import com.twinsoft.convertigo.engine.admin.services.at.ServiceDefinition;
import com.twinsoft.convertigo.engine.admin.services.mobiles.MobileResourceHelper.Keys;
import com.twinsoft.convertigo.engine.enums.Accessibility;
import com.twinsoft.convertigo.engine.enums.HeaderName;
import com.twinsoft.convertigo.engine.enums.MimeType;

@ServiceDefinition(name = "GetBuildStatus", roles = { Role.ANONYMOUS }, parameters = {}, returnValue = "")
public class GetBuildStatus extends XmlService {

	@Override
	protected void getServiceResult(HttpServletRequest request, Document document) throws Exception {
		String project = Keys.project.value(request);
		
		MobileApplication mobileApplication = getMobileApplication(project);
		
		if (mobileApplication == null) {
			throw new ServiceException("no such mobile application");
		} else {
			boolean bTpPrivateRole = Engine.authenticatedSessionManager.hasRole(request.getSession(), Role.TEST_PLATFORM_PRIVATE);
			if (!bTpPrivateRole && mobileApplication.getAccessibility() == Accessibility.Private) {
				throw new AuthenticationException("Authentication failure: user has not sufficient rights!");
			}
		}
		
		String platformName = Keys.platform.value(request);
		
		String sResult = perform(mobileApplication, platformName, request);
		
		JSONObject jsonResult = new JSONObject(sResult);
		
		Element statusElement = document.createElement("build");
		statusElement.setAttribute(Keys.project.name(), project);
		statusElement.setAttribute(Keys.platform.name(), platformName);
		
		if (jsonResult.has(platformName + "_status")) {
			statusElement.setAttribute("status", jsonResult.getString(platformName + "_status"));
		} else {
			statusElement.setAttribute("status", "none");
		}
				
		if (jsonResult.has(platformName + "_bn")) {
			statusElement.setAttribute("bn", jsonResult.getString(platformName + "_bn"));
			statusElement.setAttribute("bp_url", EnginePropertiesManager.getProperty(PropertyName.MOBILE_BUILDER_PLATFORM_URL).replaceFirst("(.*)/.*?$", "$1"));
		}

		if (jsonResult.has(platformName + "_error")) {
			statusElement.setAttribute("error", jsonResult.getString(platformName + "_error"));
		}

		statusElement.setAttribute("version", jsonResult.has("version") ? jsonResult.getString("version") : "n/a");
		statusElement.setAttribute("phonegap_version", jsonResult.has("phonegap_version") ? jsonResult.getString("phonegap_version") : "n/a");
		
		statusElement.setAttribute("revision", jsonResult.has("revision") ? jsonResult.getString("revision") : "n/a");
		statusElement.setAttribute("endpoint", jsonResult.has("endpoint") ? jsonResult.getString("endpoint") : "n/a");

		document.getDocumentElement().appendChild(statusElement);
	}

	static MobileApplication getMobileApplication(String projectName) throws EngineException {
		return Engine.theApp.databaseObjectsManager.getOriginalProjectByName(projectName).getMobileApplication();
	}
	
	static public String perform(MobileApplication mobileApplication, String platformName, HttpServletRequest request) throws Exception {
		String mobileBuilderPlatformURL = EnginePropertiesManager.getProperty(PropertyName.MOBILE_BUILDER_PLATFORM_URL);

		URL url = new URL(mobileBuilderPlatformURL + "/getstatus");
		
		HostConfiguration hostConfiguration = new HostConfiguration();
		hostConfiguration.setHost(url.getHost());
		HttpState httpState = new HttpState();
		Engine.theApp.proxyManager.setProxy(hostConfiguration, httpState, url);
		
		PostMethod method = new PostMethod(url.toString());
		
		try {
			HeaderName.ContentType.setRequestHeader(method, MimeType.WwwForm.value());
			method.setRequestBody(new NameValuePair[] {
				new NameValuePair("application", mobileApplication.getComputedApplicationName()),
				new NameValuePair("platformName", platformName),
				new NameValuePair("auth_token", mobileApplication.getComputedAuthenticationToken()),
				new NameValuePair("endpoint", mobileApplication.getComputedEndpoint(request))
			});
			
			
			int methodStatusCode = Engine.theApp.httpClient.executeMethod(hostConfiguration, method, httpState);

			InputStream methodBodyContentInputStream = method.getResponseBodyAsStream();
			byte[] httpBytes = IOUtils.toByteArray(methodBodyContentInputStream);
			String sResult = new String(httpBytes, "UTF-8");

			if (methodStatusCode != HttpStatus.SC_OK) {
				throw new ServiceException("Unable to get building status for application '" + mobileApplication.getProject()
						+ "' (final app name: '" + mobileApplication.getComputedApplicationName() + "').\n" + sResult);
			}
			
			return sResult;
		} finally {
			method.releaseConnection();
		}
	}
}
